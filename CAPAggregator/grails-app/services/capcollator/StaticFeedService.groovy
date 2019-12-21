package capcollator

import grails.gorm.transactions.*
import groovy.xml.MarkupBuilder
import java.io.File
import java.io.FileWriter
import groovy.xml.XmlUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import groovy.util.XmlParser
import java.util.TimeZone
import groovy.xml.StreamingMarkupBuilder 
import org.apache.commons.collections4.map.PassiveExpiringMap;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.PutObjectResult;
import grails.events.annotation.Subscriber
import grails.async.Promise
import static grails.async.Promises.*

@Transactional
class StaticFeedService {

  def grailsApplication
  def capCollatorSystemService

  public static int MAX_FEED_ENTRIES = 100;

  // Hold a cache of rss feeds so that we can avoid repeatedly parsing the same file,
  // particularly when processing multiple files
  private Map rss_cache = Collections.synchronizedMap(new PassiveExpiringMap(1000*60*40))

  private List<String> feed_write_queue = Collections.synchronizedList(new ArrayList<String>());

  // Needs setup in ~/.aws/confing and ~/.aws/credentials
  // AmazonS3 s3 = null;

  // Where we are going to read our bootstrap rss feeds from. Useful for migration
  private String bootstrap_bucket_name;

  // The S3 bucket we will write updated feeds to
  private String bucket_name;

  private String static_feeds_dir;
  private String feed_base_url;

  @javax.annotation.PostConstruct
  def init () {
    log.info("StaticFeedService::init - PATCH-20191201-1426");

    //bucket_name = capCollatorSystemService.getCurrentState().get('capcollator.awsBucketName')
    //static_feeds_dir = capCollatorSystemService.getCurrentState().get('capcollator.staticFeedsDir')
    //feed_base_url = capCollatorSystemService.getCurrentState().get('capcollator.staticFeedsBaseUrl')

    Promise p = task {
      watchRssQueue();
    }
    p.onError { Throwable err ->
      log.error("Promise error",err);
    }
    p.onComplete { result ->
      log.debug("Promise completed OK");
    }
  }

  @Transactional
  @Subscriber 
  capcolSettingsUpdated(Map settings) {
    log.info("Static feed service is notified that settings have updated, ${settings}");

    bucket_name = capCollatorSystemService.getCurrentState().get('capcollator.awsBucketName')
    static_feeds_dir = capCollatorSystemService.getCurrentState().get('capcollator.staticFeedsDir')
    feed_base_url = capCollatorSystemService.getCurrentState().get('capcollator.staticFeedsBaseUrl')

    synchronized(feed_write_queue) {
      feed_write_queue.notifyAll();
    }

    log.info("After config update, bucket_name=${bucket_name}, static_feeds_dir=${static_feeds_dir}, feed_base_url=${feed_base_url}");
  }


  /**
   * @param routingKey - Subscription matching
   * @param body - JSON for alert
   * @param context
   */
  def update(routingKey, body, context) {
    log.info("StaticFeedService::update(${routingKey},...)");
    try {
      String[] key_components = routingKey.split('\\.');
      if ( ( static_feeds_dir != null ) && ( key_components.length == 2 ) ) {
        String sub_name = key_components[1]
        String full_path = static_feeds_dir+'/'+sub_name;
        String cached_alert_file = body.AlertMetadata.cached_alert_xml

        File sub_dir = new File(full_path)
        if ( ! sub_dir.exists() ) {
          log.debug("Setting up new static sub DIR ${full_path}");
          sub_dir.mkdirs()
        }
        else {
          log.debug("${full_path} already present");
        }
  
        File rss_file = new File(full_path+'/rss.xml')
        if ( ! rss_file.exists() ) {
          log.debug("Create starter feed - ${full_path}/rss.xml");
          createStarterFeed(full_path, sub_name);
        }
        else {
          log.debug("${full_path}/rss.xml present");
        }
  
        addItem(full_path, body, sub_name, cached_alert_file)
      }
      else {
        log.error("Unexpected number of routing key components:: ${key_components} or static_feeds_dir is null (${static_feeds_dir})");
      }
    }
    catch ( Exception e ) {
      log.error("Unexpected problem in update", e);
    }
    finally {
      log.debug("StaticFeedService::update complete");
    }
  }


  // IF therw are S3 credentals configured, push the alert there also
  private pushToS3(String path) {
    try {
      if ( ( bucket_name != null ) && 
           ( bucket_name.length() > 0 ) && 
           ( static_feeds_dir != null ) ) {
        // AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        // ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(profile);
                             // .withRegion("us-west-1") // The first region to try your request against
                             // .withCredentials(new ProfileCredentialsProvider('inmet')) // The first region to try your request against
                             // .withRegion("us-east-1") // The first region to try your request against
        AmazonS3 client = AmazonS3ClientBuilder.defaultClient();


        // Strip off any prefix we are using locally, to leave the raw path
        String s3_key = path.replaceAll((static_feeds_dir+'/'),'');
     
        ObjectMetadata om = new ObjectMetadata()
        om.setCacheControl('max-age=60');

        File file_input = new File(path);
        om.setContentLength(file_input.length());
        om.setContentType('application/xml');

        log.debug("S3 mirror ${path} in bucket ${bucket_name} - key name will be ${s3_key}");
        PutObjectResult result = client.putObject(bucket_name, s3_key, new FileInputStream(path), om);
        log.debug("Result of s3.putObject: ${result}");

        client.shutdown()
      }
      else {
        log.warn("pushToS3(${path}) - no action - bucket name null(${bucket_name}) OR static_feeds_dir null (${static_feeds_dir})");
      }
    }
    catch ( com.amazonaws.AmazonServiceException ase) {
      log.error("Problem with AWS mirror",ase);
    }
    finally {
      log.info("pushToS3(${path}) complete");
    }
  }

  private void createStarterFeed(String path, subname) {

    String feed_name_prefix = capCollatorSystemService.getCurrentState().get('capcollator.feedTitlePrefix') ?: ''
    String feed_name_postfix = capCollatorSystemService.getCurrentState().get('capcollator.feedTitlePostfix') ?: ''

    // <?xml version='1.0' encoding='UTF-8'?>
    // <rss xmlns:atom="http://www.w3.org/2005/Atom" version="2.0">
    //   <channel>
    //     <atom:link rel="self" href="https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml" type="application/rss+xml"/>
    //     <atom:link rel="alternate" title="RSS" href="https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml" type="application/rss+xml"/>
    //     <title>Latest Valid CAP alerts received, unfiltered</title>
    //     <link>https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml</link>
    //     <description>This feed lists the most recent valid CAP alerts uploaded to the Filtered Alert Hub.</description>
    //     <language>en</language>
    //     <copyright>public domain</copyright>
    //     <pubDate>Sun, 26 Aug 2018 21:23:20 GMT</pubDate>
    //     <lastBuildDate>Wed, 4 Jan 2017 11:31:34 GMT</lastBuildDate>
    //     <docs>http://blogs.law.harvard.edu/tech/rss</docs>
    //     <image>
    //      <title>Latest Valid CAP alerts received, unfiltered</title>
    //      <url>https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/capLogo.jpg</url>
    //      <link>https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml</link>
    //     </image>
    //   </channel>
    // </rss>

    def fileWriter = new FileWriter(path+'/rss.xml');
    def rssBuilder = new MarkupBuilder(fileWriter)

    def sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")
    def pub_date_str = sdf.format(new Date());
 
    rssBuilder.mkp.xmlDeclaration(version:'1.0', encoding:'utf-8')
    rssBuilder.pi('xml-stylesheet':[href:'https://alert-feeds.s3.amazonaws.com/rss-style.xsl', type:'text/css']);
    rssBuilder.'rss'(// 'xmlns':'http://www.w3.org/2005/Atom', -- The namespace for the document is not ATON
                     'xmlns:rss':'http://www.rssboard.org/rss-specification',
                     'xmlns:atom':'http://www.w3.org/2005/Atom',
                     'xmlns:dc':'http://purl.org/dc/elements/1.1/', 
                     'xmlns:cap':'http://demo.semweb.co/capcollator/', 
                     version:"2.0") {
      channel {
        'atom:link'(rel:'self',href:"${feed_base_url}/${subname}/rss.xml", type:"application/rss+xml")
        'atom:link'(rel:'alternate',title:'RSS',href:"${feed_base_url}/${subname}/rss.xml", type:"application/rss+xml")
        title("${feed_name_prefix}Latest Valid CAP alerts received, ${subname}${feed_name_postfix}")
        link("${feed_base_url}/${subname}")
        description("This feed lists the most recent valid CAP alerts uploaded to the Filtered Alert Hub.")
        language("en");
        copyright("public domain");
        pubDate(pub_date_str);
        //lastBuildDate(pub_date_str);
        docs("http://blogs.law.harvard.edu/tech/rss");
        image {
          title("${feed_name_prefix}Latest Valid CAP alerts received, ${subname}${feed_name_postfix}");
          url("${feed_base_url}/${subname}/capLogo.jpg");
          link("${feed_base_url}/${subname}/rss.xml");
        }
      }
    }
    fileWriter.close();

    log.info("createStarterFeed calling push to s3 ${path}/rss.xml");
    pushToS3(path+'/rss.xml');
  }

  private void createStarterFeedFromTemplate(String path, String subname, String tmpl) {
    if ( ( tmpl != null ) && 
         ( tmpl.length() > 0 ) ) {
      File f = new File(path+'/rss.xml')
      if ( f.exists() ) {
        log.warn("${path}/rss.xml already exists - not overwriting");
      } 
      else {
        // Should probably validate the XML here before just writing it
        f.write(tmpl)
        log.info("createStarterFeedFromTemplate calling pushToS3 - ${path}/rss.xml");
        pushToS3(path+'/rss.xml');
      }
    }
    else {
      log.error("Unable to find subscription by ID: ${subname}. Fallback to old method")
      createStarterFeed(path,subname);
    }
  }

  private groovy.util.Node getExistingRss(String path) {

    groovy.util.Node result = null;

    result = rss_cache.get(path);
 
    if ( result == null ) {
      log.debug("Looks like entry for ${path} was evicted from cache. Reload");
      try {
        log.debug("Parse existing RSS at ${path}/rss.xml and cache");
        File existing_rss = new File(path+'/rss.xml');
        if ( existing_rss.exists() ) {
          groovy.util.XmlParser xml_parser = new XmlParser(false,true,true)
          xml_parser.startPrefixMapping('atom','http://www.w3.org/2005/Atom');
          xml_parser.startPrefixMapping('','');
          // xml_parser.processingInstruction('xml-stylesheet', 'type="text/xsl" href="https://cap-alerts.s3.amazonaws.com/rss-style.xsl"');
          result = xml_parser.parse(new File(path+'/rss.xml'))

          // This parser ignores processing instructions, so manually re-add a stylesheet

          rss_cache.put(path, result);
        }
        else {
          log.warn("getExistingRss(${path}) did not find an RSS file, and expected to.");
        }
      }
      catch ( Exception e ) {
        log.error("Problem trying to parse existing feed at ${path}",e)
      }
    }
    else {
      // log.debug("RSS Feed retrieved from cache, no need to parse");
      // Re-put the XML so that we reset the expiration time... Making this a kind of LRU expiring cache
      rss_cache.put(path, result);
    }

    result
  }

  // This method should defer writing briefly in case other alerts come in, so we can write them all at once.
  private void enqueueRss(String path) {
    log.debug("Waiting for lock on feed write queue");
    synchronized(feed_write_queue) {
      if ( feed_write_queue.contains(path) ) {
        // Already queued
        log.info("${path} already present in feed_write_queue. Current size is ${feed_write_queue.size()}");
      }
      else {
        feed_write_queue.add(path);
        log.info("Add ${path} to feed_write_queue. Current size is ${feed_write_queue.size()} notify all");
        feed_write_queue.notifyAll();
      }
    }
    log.debug("enqueueRss(${path}) complete");
  }

  private watchRssQueue() {
    log.debug("watchRssQueue()");
    try {
      while(true) {
        String path_to_write = null;

        synchronized(feed_write_queue) {

          if ( feed_write_queue.size() == 0 ) {
            log.info("watchRssQueue() waiting up to 60s (Queue size is 0)");
            feed_write_queue.wait(60000);
          }

          if ( feed_write_queue.size() > 0 ) {
            path_to_write = feed_write_queue.remove(0)
            log.debug("Removed ${path_to_write} from feed write queue. new size is ${feed_write_queue.size()}");
          }
        }

        try {
          if ( path_to_write != null ) {
            // https://stackoverflow.com/questions/13681882/parse-xml-using-groovy-override-charset-in-declaration-and-add-xml-processing-i

            log.debug("watchRssQueue() process ${path_to_write}");
            // def xml_for_feed = rss_cache.get(path_to_write)
            def xml_for_feed = getExistingRss(path_to_write)
            // <?xml-stylesheet href='https://cap-alerts.s3.amazonaws.com/rss-style.xsl' type='text/css'?>
  
            if ( xml_for_feed == null ) {
              log.error("Unable to find xml for feed with path ${path_to_write} - existing queue cache was null");
            }
            else {
              synchronized (xml_for_feed) {
                java.io.Writer writer = new StringWriter(); // FileWriter(path_to_write+'/rss.xml')
                XmlUtil.serialize(xml_for_feed, writer)
                writer.flush()
                writer.close()

                // This is fugly, but will do for now.
                String newfeed = writer.toString();
                log.debug("Before Replace... ${newfeed.substring(0,50)}");
                java.lang.CharSequence cs1 = '<rss';
                java.lang.CharSequence cs2 = '''
<?xml-stylesheet href='https://alert-feeds.s3.amazonaws.com/rss-style.xsl' type='text/xsl'?>
<rss''';
                newfeed = newfeed.replace(cs1, cs2);
                log.debug("After Replace... ${newfeed.substring(0,50)}");

                File f = new File(path_to_write+'/rss.xml')
                f.write(newfeed)

                log.info("call push to S3 from watchRssQueue with ${path_to_write}");
                pushToS3(path_to_write+'/rss.xml');
              }
            }
          }
          else {
            // This thread wakes up every 60s now - path to write can be null
          }
        }
        catch ( Exception e ) {
          log.error("Exception whilst trying to write ${path_to_write}.",e);
        }
      }
    }
    catch ( Exception e ) {
      log.error("problem watching RSS Queue",e);
    }
  }

  private void addItem(String path, node, subname, cached_alert_file) {


    // log.debug("addItem(${path},${node})");
    if ( node?.AlertMetadata?.capCollatorUUID ) {
      // log.debug("capCollatorUUID: ${node.AlertMetadata.capCollatorUUID}")
  
        Long alert_created_systime = node.AlertMetadata.createdAt
  
        String source_feed_id = node.AlertMetadata.sourceFeed;

        groovy.util.Node xml = null;
        try {
          xml = getExistingRss(path);
        }
        catch ( Exception e ) {
          log.error("Problem trying to read existing RSS file : ${path} - so resetting the file");
          createStarterFeedFromTemplate(path, subname);
          xml = getExistingRss(path);
        }

        synchronized(xml) {
  
          //Edit File e.g. append an element called foo with attribute bar
          // log.debug("Get first info section");
          def info = getFirstInfoSection(node);
    
          def formatted_pub_date = null;
          def formatted_pub_date_2 = null;
          def formatted_write_date = new SimpleDateFormat('yyyy-MM-dd\'T\'HH-mm-ss-SSS.z').format(new Date());
          def rfc822_formatted_write_date = new SimpleDateFormat('EEE, dd MMM yyyy HH:mm:ss Z').format(new Date());
  
          try {
            formatted_pub_date_2 = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss.SSSZ').format(new Date(alert_created_systime));
            def sdf = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ssX')
            def alert_date = sdf.parse(node?.AlertBody?.sent);
            formatted_pub_date = new SimpleDateFormat('EEE, dd MMM yyyy HH:mm:ss Z').format(alert_date);
          }
          catch ( Exception e ) {
            log.error("Problem formatting dates for static feed publishing. alert sent at ${node?.AlertBody?.sent}", e);
          }
    
          def atomns = new groovy.xml.Namespace('http://www.w3.org/2005/Atom','atom')
          def ccns = new groovy.xml.Namespace('http://demo.semweb.co/CapCollator','capcol');
    
          String feed_entry_prefix = capCollatorSystemService.getCurrentState().get('capcollator.feedEntryPrefix') ?: ''
          String feed_entry_postfix = capCollatorSystemService.getCurrentState().get('capcollator.feedEntryPostfix') ?: ''

          String entry_title = info?.headline ?: info?.description;

          def new_item_node = xml.channel[0].appendNode( 'item' );
          new_item_node.appendNode( 'title', "${feed_entry_prefix}${entry_title}${feed_entry_postfix}".toString() );
          new_item_node.appendNode( 'link', "${feed_base_url}/${cached_alert_file}".toString());
          new_item_node.appendNode( 'description', info?.description);
          new_item_node.appendNode( 'pubDate', formatted_pub_date ?: node?.AlertBody?.sent);
          new_item_node.appendNode( atomns.'updated', formatted_pub_date_2 )
          // new_item_node.appendNode( ccns.'dateWritten', formatted_write_date )
          new_item_node.appendNode( ccns.'sourceFeed', node?.AlertMetadata.sourceFeed )
          new_item_node.appendNode( ccns.'alertId', node?.AlertMetadata?.capCollatorUUID )
    
          //      //'dc:creator'('creator')
          //      //'dc:date'('date')

          log.debug("Static feed appending new node with atom:updated ${formatted_pub_date_2} entry will be sorted based on this value");
    
          // The true asks the sort to mutate the source list. Source elements without a pubDate element high - so the none item
          // entries float to the top of the list
          xml.channel[0].children().sort(true) { a,b ->
            ( b.'atom:updated'?.text() ?: 'zzz'+(b.name().toString() ) ).compareTo( ( a.'atom:updated'?.text() ?: 'zzz'+(a.name().toString() ) ) )
          }

          if ( xml.channel[0].lastBuildDate.size() == 0 ) {
            xml.channel[0].appendNode('lastBuildDate', rfc822_formatted_write_date);
          }
          else {
            xml.channel[0].lastBuildDate[0].value = rfc822_formatted_write_date;
          }
    
          if ( xml.channel[0].pubDate.size() == 0 ) {
            log.debug("add pub date");
            xml.channel[0].appendNode('pubDate', rfc822_formatted_write_date)
          }
          else if ( xml.channel[0].pubDate.size() == 1 ) {
            log.debug("update pub date to ${formatted_pub_date} ?: ${node?.AlertBody?.sent}");
            xml.channel[0].pubDate[0].value = rfc822_formatted_write_date
          }
          else {
            log.warn("Multiple pubDate elements");
          }
    
          // log.debug("Trim rss feed. Size before: ${xml.channel[0].children().size()}");
          int ctr = MAX_FEED_ENTRIES;
          xml.channel[0] = xml.channel[0].item.each { n ->
            if ( ctr > 0 ) {
              ctr--;
            }
            else {
              log.debug("remove...");
              n.replaceNode{}
            }
          }
          // log.debug("Trim rss feed. Size after: ${xml.channel[0].children().size()}");
  
        }
  
        // Update the cache with the latest XML (enqueueRss will refer back to the cache)
        rss_cache.put(path, xml)

        // Write the file
        log.debug("Enqueue update for path ${path}");
        enqueueRss(path);
    }
    else {
      log.warn("Missing alert uuid");
    }
  }

  private Map getFirstInfoSection(node) {
    Map result = null;
    if ( node.AlertBody.info instanceof List ) 
      result = node.AlertBody.info.get(0);
    else
      result = node.AlertBody.info

    return result
  }

  private String generatePrefix() {
    def rnd = new Random();
    String result = ''+ ( ( rnd.nextInt(26) + ('a' as char) ) as char ) + ( ( rnd.nextInt(26) + ('a' as char) ) as char ) + '/'
    result;
  }

  // Cache the source alert XML in the local filesystem
  public String writeAlertXML(byte[] content, String sourcefeed_id, Date alert_time) {

    log.debug("writeAlertXML(...,${sourcefeed_id},${alert_time})")

    String path = static_feeds_dir+'/'

    // Arrange for a UTC timestamp to be used as the filename
    TimeZone timeZone_utc = TimeZone.getTimeZone("UTC");
    def sdf = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ssX')

    def output_filename_sdf = new SimpleDateFormat('yyyy-MM-dd\'T\'HH-mm-ss-SSS.z')
    output_filename_sdf.setTimeZone(timeZone_utc);

    def cal = Calendar.getInstance(timeZone_utc)
    cal.setTime(alert_time);

    // Generate a prefix so we distribute the buckets evenly
    String prefix = generatePrefix()
    def alert_path = sourcefeed_id+'/'
    File alert_path_dir = new File(path+alert_path+prefix);
    if ( ! alert_path_dir.exists() ) {
      // log.debug("Setting up new static sub DIR ${alert_path_dir}");
      alert_path_dir.mkdirs()
    }

    // Prefpare a postfix suffixe to protect from duplicate times - _0 _1 _2 etc
    int duplicate_protection = 0

    // Work out our starter filename
    String output_filename = sourcefeed_id+'_'+output_filename_sdf.format(alert_time)
    String full_alert_filename = alert_path+prefix+output_filename + '_0.xml'

    File new_alert_file = new File(path+full_alert_filename)
    while ( new_alert_file.exists() ) {
      full_alert_filename = alert_path+prefix+output_filename + '_' + (++duplicate_protection) +'.xml'
      new_alert_file = new File(path+full_alert_filename)
    }

    log.debug("Writing alert [${content.length}] xml to ${new_alert_file}");

    new_alert_file << content

    log.info("call pushToS3 from writeAlertXML with ${path} and ${full_alert_filename}");
    pushToS3(path+full_alert_filename);

    return full_alert_filename
  }

  public void initialiseFeed(String sub_name, String tmpl) {
    if ( tryToCacheRssFromS3(sub_name) == false ) {
      String full_path = static_feeds_dir+'/'+sub_name;

      File sub_dir = new File(full_path)
      if ( ! sub_dir.exists() ) {
        sub_dir.mkdirs()
      }
      else {
        log.debug("${full_path} already present");
      }
  
      String starter_rss_file = full_path+'/rss.xml'
      File rss_file = new File(starter_rss_file);
      if ( ! rss_file.exists() ) {
        log.info("Create starter feed - ${full_path}/rss.xml");
        createStarterFeedFromTemplate(full_path, sub_name, tmpl);
      }
      else {
        log.debug("${full_path}/rss.xml present");
      }
  
      pushToS3(starter_rss_file);
    }
  }

  // Try to pull the local cache of the RSS from S3
  // return true if the file was pulled, false otherwise
  private boolean tryToCacheRssFromS3(String sub_name) {
    boolean result = false;

    String bucket = bootstrap_bucket_name ?: bucket_name;

    if ( ( bucket != null ) && 
         ( bucket.length() > 0 ) && 
         ( static_feeds_dir != null ) ) {


      String full_path = static_feeds_dir+'/'+sub_name;
      File sub_dir = new File(full_path)
      if ( ! sub_dir.exists() ) {
        sub_dir.mkdirs()
      }
      else {
        log.debug("${full_path} already present");
      }
  
      String starter_rss_file = full_path+'/rss.xml'
      File rss_file = new File(starter_rss_file);
      if ( ! rss_file.exists() ) {
        try {
          log.debug("build client");
          AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient()
          log.debug("Got client");
          String s3_key = starter_rss_file.replaceAll((static_feeds_dir+'/'),'');
          log.debug("Attempt to cache ${s3_key} from bucket ${bucket}");
          if ( s3.doesObjectExist(bucket, s3_key) ) {
            log.debug("${starter_rss_file} found in S3 - pulling file into local cache");
            S3Object s3o = s3.getObject(bucket, s3_key);
            rss_file << s3o.getObjectContent()
            result = true;
          }
          s3.shutdown()
        }
        catch ( Exception e ) {
          log.error("Problem fetching feed from s3");
        }
        finally { 
          log.debug("fetch from s3 complete");
        }
      }
      else {
        result = true;
      }
    }
    else {
      log.warn("tryToCacheRssFromS3(${sub_name}) - No action. bucket null (${bucket}) or  static_feeds_dir null (${static_feeds_dir})");
    }

    return result;

  }
}
