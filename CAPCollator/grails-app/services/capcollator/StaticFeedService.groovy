package capcollator

import grails.transaction.Transactional
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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Transactional
class StaticFeedService {

  def grailsApplication
  public static int MAX_FEED_ENTRIES = 100;

  // Hold a cache of rss feeds so that we can avoid repeatedly parsing the same file,
  // particularly when processing multiple files
  private Map rss_cache = Collections.synchronizedMap(new PassiveExpiringMap(1000*60*40))

  private List<String> feed_write_queue = Collections.synchronizedList(new ArrayList<String>());

  ExecutorService executor = Executors.newSingleThreadExecutor()

  // Needs setup in ~/.aws/confing and ~/.aws/credentials
  AmazonS3 s3 = null;

  @javax.annotation.PostConstruct
  def init () {
    log.debug("StaticFeedService::init");
    try {
      if ( grailsApplication.config.awsBucketName ) {
        log.info("Configure AWS S3 to mirror feeds using bucket ${grailsApplication.config.awsBucketName}");
        s3 = AmazonS3ClientBuilder.defaultClient();
        log.info("S3 configured");
      }
      else {
        log.info("No awsBucketName configured - local feeds will not be mirrored on S3");
      }

      executor.execute {
        watchRssQueue();
      }
    }
    catch ( com.amazonaws.AmazonServiceException ase) {
      log.error("Problem with AWS mirror setup",ase);
    }
  }


  /**
   * @param routingKey - Subscription matching
   * @param body - JSON for alert
   * @param context
   */
  def update(routingKey, body, context) {
    String[] key_components = routingKey.split('\\.');
    if ( key_components.length == 2 ) {
      String sub_name = key_components[1]
      String full_path = grailsApplication.config.staticFeedsDir+'/'+sub_name;
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
      log.error("Unexpected number of routing key components:: ${key_components}");
    }
    log.debug("StaticFeedService::update complete");
  }


  // IF therw are S3 credentals configured, push the alert there also
  private pushToS3(String path) {
    try {
      if ( s3 ) {
        // Strip off any prefix we are using locally, to leave the raw path
        String s3_key = path.replaceAll((grailsApplication.config.staticFeedsDir+'/'),'');

        // log.debug("S3 mirror ${path} in bucket ${grailsApplication.config.awsBucketName} - key name will be ${s3_key}");

        s3.putObject(grailsApplication.config.awsBucketName, s3_key, new File(path));
      }
    }
    catch ( com.amazonaws.AmazonServiceException ase) {
      log.error("Problem with AWS mirror",ase);
    }
  }

  private void createStarterFeed(String path, subname) {

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

    rssBuilder.'rss'(// 'xmlns':'http://www.w3.org/2005/Atom', -- The namespace for the document is not ATON
                     'xmlns:rss':'http://www.rssboard.org/rss-specification',
                     'xmlns:atom':'http://www.w3.org/2005/Atom',
                     'xmlns:dc':'http://purl.org/dc/elements/1.1/', 
                     'xmlns:cap':'http://demo.semweb.co/capcollator/', 
                     version:"2.0") {
      channel {
        'atom:link'(rel:'self',href:"${grailsApplication.config.staticFeedsBaseUrl}/${subname}/rss.xml", type:"application/rss+xml")
        'atom:link'(rel:'alternate',title:'RSS',href:"${grailsApplication.config.staticFeedsBaseUrl}/${subname}/rss.xml", type:"application/rss+xml")
        title("Latest Valid CAP alerts received, ${subname}")
        link("${grailsApplication.config.staticFeedsBaseUrl}/${subname}")
        description("This feed lists the most recent valid CAP alerts uploaded to the Filtered Alert Hub.")
        language("en");
        copyright("public domain");
        pubDate(pub_date_str);
        lastBuildDate(pub_date_str);
        docs("http://blogs.law.harvard.edu/tech/rss");
        image {
          title("Latest Valid CAP alerts received, ${subname}");
          url("${grailsApplication.config.staticFeedsBaseUrl}/${subname}/capLogo.jpg");
          link("${grailsApplication.config.staticFeedsBaseUrl}/${subname}/rss.xml");
        }
      }
    }
    fileWriter.close();

    pushToS3(path+'/rss.xml');
  }

  private groovy.util.Node getExistingRss(String path) {

    groovy.util.Node result = null;

    result = rss_cache.get(path);
 
    if ( result == null ) {
      // log.debug("Parse existing RSS at ${path}/rss.xml and cache");
      groovy.util.XmlParser xml_parser = new XmlParser(false,true,true)
      xml_parser.startPrefixMapping('atom','http://www.w3.org/2005/Atom');
      xml_parser.startPrefixMapping('','');
      result = xml_parser.parse(new File(path+'/rss.xml'))
      rss_cache.put(path, result);
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
    log.debug("enqueueRss(${path}) wait for lock on feed_write_queue");
    synchronized(feed_write_queue) {
      if ( feed_write_queue.contains(path) ) {
        // Already queued
      }
      else {
        feed_write_queue.add(path);
        // log.debug("Add ${path} to feed_write_queue. Current size is ${feed_write_queue.size()} notify all");
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
          // log.debug("watchRssQueue() waiting");
          feed_write_queue.wait();
          if ( feed_write_queue.size() > 0 ) {
            path_to_write = feed_write_queue.remove(0)
          }
        }

        if ( path_to_write != null ) {
          // log.debug("watchRssQueue() process ${path_to_write}");
          java.io.Writer writer = new FileWriter(path_to_write+'/rss.xml')
          def xml_for_feed = rss_cache.get(path_to_write)

          synchronized (xml_for_feed) {
            XmlUtil.serialize(rss_cache.get(path_to_write), writer)
          }

          writer.flush()
          writer.close()
          pushToS3(path_to_write+'/rss.xml');
        }
        else {
          // log.debug("watchRssQueue awake, but no file to write");
        }
      }
    }
    catch ( Exception e ) {
      log.error("problem",e);
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void addItem(String path, node, subname, cached_alert_file) {

    // log.debug("addItem(${path},${node})");
    if ( node?.AlertMetadata?.capCollatorUUID ) {
      // log.debug("capCollatorUUID: ${node.AlertMetadata.capCollatorUUID}")
  
        Long alert_created_systime = node.AlertMetadata.createdAt
  
        String source_feed_id = node.AlertMetadata.sourceFeed;
        // String static_alert_file = writeAlertFile(node.AlertMetadata.capCollatorUUID, path, node, source_alert, alert_created_systime, source_feed_id);

        groovy.util.Node xml = null;
        try {
          xml = getExistingRss(path);
        }
        catch ( Exception e ) {
          log.error("Problem trying to read existing RSS file : ${path} - so resetting the file");
          createStarterFeed(path, subname);
          xml = getExistingRss(path);
        }

        synchronized(xml) {
  
          //Edit File e.g. append an element called foo with attribute bar
          // log.debug("Get first info section");
          def info = getFirstInfoSection(node);
    
          def formatted_pub_date = null;
          def formatted_pub_date_2 = null;
          def formatted_write_date = new SimpleDateFormat('yyyy-MM-dd\'T\'HH-mm-ss-SSS.z').format(new Date());
  
          try {
            formatted_pub_date_2 = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss.SSSZ').format(new Date(alert_created_systime));
            def sdf = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ssX')
            def alert_date = sdf.parse(node?.AlertBody?.sent);
            formatted_pub_date = new SimpleDateFormat('EEE, dd MMM yyyy HH:mm:ss Z').format(alert_date);
          }
          catch ( Exception e ) {
          }
    
          def atomns = new groovy.xml.Namespace('http://www.w3.org/2005/Atom','atom')
          def ccns = new groovy.xml.Namespace('http://demo.semweb.co/CapCollator','capcol');
    
          def new_item_node = xml.channel[0].appendNode( 'item' );
          new_item_node.appendNode( 'title', info?.headline ?: info?.description );
          new_item_node.appendNode( 'link', "${grailsApplication.config.staticFeedsBaseUrl}/${cached_alert_file}".toString());
          new_item_node.appendNode( 'description', info?.description);
          new_item_node.appendNode( 'pubDate', formatted_pub_date ?: node?.AlertBody?.sent);
          new_item_node.appendNode( atomns.'updated', formatted_pub_date_2 )
          new_item_node.appendNode( ccns.'dateWritten', formatted_write_date )
          new_item_node.appendNode( ccns.'sourceFeed', node?.AlertMetadata.sourceFeed )
    
          //      //'dc:creator'('creator')
          //      //'dc:date'('date')
    
          // The true asks the sort to mutate the source list. Source elements without a pubDate element high - so the none item
          // entries float to the top of the list
          xml.channel[0].children().sort(true) { a,b ->
            ( b.'atom:updated'?.text() ?: 'zzz'+(b.name().toString() ) ).compareTo( ( a.'atom:updated'?.text() ?: 'zzz'+(a.name().toString() ) ) )
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

  private String writeAlertFile(uuid, path, node, content, alert_time, sourcefeed_id) {

    // https://alert-hub.s3.amazonaws.com/us-epa-aq-en/2018/09/07/12/28/2018-09-07-12-28-41-693.xml
    // log.debug("writeAlertNode ${new String(content)}");
    TimeZone timeZone_utc = TimeZone.getTimeZone("UTC");
    def sdf = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ssX')

    def output_filename_sdf = new SimpleDateFormat('yyyy-MM-dd\'T\'HH-mm-ss-SSS.z')
    output_filename_sdf.setTimeZone(timeZone_utc);

    // def alert_date = sdf.parse(node?.AlertBody?.sent);
    def alert_date = new Date(alert_time)

    def cal = Calendar.getInstance(timeZone_utc)
    cal.setTime(alert_date);

    def alert_path = '/'
    // log.debug("Write to ${path}${alert_path}")

    int duplicate_protection = 0

    String prefix = generatePrefix()
    File alert_path_dir = new File(path+alert_path+prefix);
    if ( ! alert_path_dir.exists() ) {
      // log.debug("Setting up new static sub DIR ${alert_path_dir}");
      alert_path_dir.mkdirs()
    }

    String output_filename = sourcefeed_id+'_'+output_filename_sdf.format(alert_date)
    String full_alert_filename = alert_path+prefix+output_filename + '_0.xml'

    File new_alert_file = new File(path+full_alert_filename)
    while ( new_alert_file.exists() ) {
      full_alert_filename = alert_path+prefix+output_filename + '_' + (++duplicate_protection) +'.xml'
      new_alert_file = new File(path+full_alert_filename)
    }

    // log.debug("Writing alert [${content.length}] xml to ${new_alert_file}");

    new_alert_file << content

    pushToS3(path+full_alert_filename);

    return full_alert_filename
  }

  private String generatePrefix() {
    def rnd = new Random();
    String result = ''+ ( ( rnd.nextInt(26) + ('a' as char) ) as char ) + ( ( rnd.nextInt(26) + ('a' as char) ) as char ) + '/'
    result;
  }

  // Cache the source alert XML in the local filesystem
  public String writeAlertXML(byte[] content, String sourcefeed_id, Date alert_time) {

    // log.debug("writeAlertXML(...,${sourcefeed_id},${alert_time})")

    String path = grailsApplication.config.staticFeedsDir+'/'

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

    // log.debug("Writing alert [${content.length}] xml to ${new_alert_file}");

    new_alert_file << content

    pushToS3(path+full_alert_filename);

    return full_alert_filename
  }
}
