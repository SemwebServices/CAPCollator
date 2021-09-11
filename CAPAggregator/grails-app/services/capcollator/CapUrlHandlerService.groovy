package capcollator

import grails.gorm.transactions.*
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import static groovy.json.JsonOutput.*
import org.apache.commons.collections4.map.PassiveExpiringMap;
import groovyx.net.http.HttpBuilder
import groovyx.net.http.FromServer
import groovyx.net.http.ChainedHttpConfig
import static groovyx.net.http.HttpBuilder.configure

// Moving to Apache http client implementation for HttpBuilderNG
import groovyx.net.http.ApacheHttpBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.config.RequestConfig


/**
 * This is where all CAP URLs detected in feeds, be they atom, RSS, or other source types come to be resolved.
 */
@Transactional
class CapUrlHandlerService {

  private static int MAX_HTTP_TIME = 4*1000; // 4s
  private static byte[] stylesheet_pattern = '<?xml-stylesheet href='.getBytes();

  RabbitMessagePublisher rabbitMessagePublisher
  def eventService
  def staticFeedService
  def feedFeedbackService

  // Alerts can live in the cache for up to 2 minutes
  private Map parsed_alert_cache = Collections.synchronizedMap(new PassiveExpiringMap(1000*60*2))
  private Map local_feed_setting_cache = [:]

  private static final long LONG_ALERT_THRESHOLD = 2000;
  private static final int MAX_RETRIES = 3;

  def process(cap_url) {
    log.debug("RssEventHandlerService::process ${cap_url}");
  }

  def getLocalFeedSettings(String feed_code) {
    def result = local_feed_setting_cache[feed_code];
    if ( result == null ) {
      LocalFeedSettings.withNewTransaction {
        LocalFeedSettings lfs = LocalFeedSettings.findByUriname(feed_code)
        if ( lfs != null ) {
          result = [ status: 'ModPresent', authenticationMethod: lfs.authenticationMethod, credentials:lfs.credentials ]
        }
        else {
          result = [ status: 'NoModification' ]
        }
        local_feed_setting_cache[feed_code] = result;
      }
    }
    return result;
  }

  def handleNotification(link,context) {

    log.info("RssEventHandlerService::handleNotification(...,${context})");
    log.debug("${context.properties.headers}");

    def ts_1 = System.currentTimeMillis();
    def cap_link = link.toString()

    String source_feed = context.properties.headers['feed-code'];
    String source_id = context.properties.headers['feed-id'];
    String is_official = context.properties.headers['feed-is-official'];
    String original_cap_link = cap_link

    def lfs = getLocalFeedSettings(source_feed);
    if ( lfs?.status=='ModPresent' ) {
      switch ( lfs.authenticationMethod ) {
        case 'pin':
          cap_link += "?pin=${lfs.credentials}"
          break;
        default:
          break;
      }

      log.debug("modifed URL: ${cap_link}");
    }


    log.debug("Looking in link attribute for cap ${link}");

    // Different feeds behave differently wrt properly setting the type attribute. 
    // Until we get to grips a little better - try and parse every link - and if we manage to parse XML, see if the root node is a cap element

    log.debug("  -> processing link ${link} via ${source_feed}");

    boolean completed_ok = false;
    int retries = 0;
    
    while ( !completed_ok && retries < MAX_RETRIES ) {
      try {
        def ts_2 = System.currentTimeMillis();

        log.debug("test ${cap_link}");
        def detected_content_type = null

        HttpBuilder http_client = ApacheHttpBuilder.configure {
          request.uri = cap_link
          client.clientCustomizer { HttpClientBuilder builder ->
            RequestConfig.Builder requestBuilder = RequestConfig.custom()
            requestBuilder.connectTimeout = MAX_HTTP_TIME
            requestBuilder.connectionRequestTimeout = MAX_HTTP_TIME
            builder.defaultRequestConfig = requestBuilder.build()
          }
        }

        String response_content = http_client.get {
          response.parser('application/xml') { ChainedHttpConfig cfg, FromServer fs ->
            fs.inputStream.text
          }
          response.parser('application/octet-stream') { ChainedHttpConfig cfg, FromServer fs ->
            fs.inputStream.text
          }
          response.parser('text/xml') { ChainedHttpConfig cfg, FromServer fs ->
            fs.inputStream.text
          }

          response.failure { FromServer resp ->
            log.debug("Failure fetching content : ${resp}")
            return null;
          }

          response.success { resp, content ->
            detected_content_type = FromServer.Header.find( resp.headers, 'Content-Type')?.value
            return content;
          }

        }

        log.debug("URL Connection reports content type (In response to HEAD) ${detected_content_type}");

        if ( detected_content_type &&
             ( detected_content_type.toLowerCase().startsWith('text/xml') ||
               detected_content_type.toLowerCase().startsWith('application/octet-stream') ||   // Because of http://www.gestiondelriesgo.gov.co
               detected_content_type.toLowerCase().startsWith('application/xml') ) ) {

          def parser = new XmlSlurper()

          def fetch_completed = System.currentTimeMillis();

          byte[] alert_bytes = response_content.getBytes()

          // It would be nice to see if we can extract any stylesheet referenced as <?xml-stylesheet href='capatomproduct.xsl' type='text/xsl'?>
          // from the first n bytes
          String xml_stylesheet = findStylesheet(alert_bytes)

          String cached_alert_xml = staticFeedService.writeAlertXML(alert_bytes, source_feed, new Date(ts_2))

          parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false) 
          parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

          def parsed_cap = parser.parse(new ByteArrayInputStream(alert_bytes));
          String alert_uuid = java.util.UUID.randomUUID().toString()

          if ( ( parsed_cap != null ) && ( parsed_cap.identifier != null ) ) {

            def ts_3 = System.currentTimeMillis();
            log.debug("Managed to parse link, looks like CAP :: handleNotification ::\"${parsed_cap.identifier}\"");

            def entry = domNodeToString(parsed_cap)

            def latest_expiry = null;
            def latest_effective = null;
            parsed_cap.info.each { info_element ->
              latest_expiry = info_element.expires?.text()
              latest_effective = info_element.effective?.text()
            }

            log.debug("latest_expiry is ${latest_expiry}");
            def alert_metadata = [:]
            alert_metadata.createdAt=System.currentTimeMillis()
            alert_metadata.CCHistory=[]

            alert_metadata.CCHistory.add(["event":"CC-RSS-notified","timestamp":ts_1]);
            alert_metadata.CCHistory.add(["event":"CC-HTTP Get completed","timestamp":fetch_completed]);
            alert_metadata.CCHistory.add(["event":"CC-parse complete","timestamp":ts_3]);
            alert_metadata.CCHistory.add(["event":"CC-emit CAP event","timestamp":alert_metadata.createdAt]);

            def elapsed = alert_metadata.createdAt - ts_1;
            if ( elapsed > LONG_ALERT_THRESHOLD ) {
              log.info("Alert processing exceeded LONG_ALERT_THRESHOLD(${elapsed}) ${cap_link}");
            }

            alert_metadata.detectedStylesheetPI = xml_stylesheet
            alert_metadata.hasStylesheet = (xml_stylesheet == null) ? 'N' : 'Y'
            alert_metadata.PrivateSourceUrl = cap_link
            alert_metadata.SourceUrl = original_cap_link
            alert_metadata.capCollatorUUID = alert_uuid;
            alert_metadata.sourceFeed = source_feed;
            alert_metadata.sourceIsOfficial = is_official;
            alert_metadata.cached_alert_xml = cached_alert_xml;

            if ( latest_expiry && latest_expiry.trim().length() > 0 )
              alert_metadata.Expires = latest_expiry

            if ( latest_effective && latest_effective.trim().length() > 0 )
              alert_metadata.Effective = latest_effective

            // Render the cap object as JSON - We wrap the converted message in an object so we can add some metadata about
            // processing time - for stats / tracking the delay through the system
            String json_text = '{ "AlertMetadata":'+toJson(alert_metadata)+',"AlertBody":'+capcollator.Utils.XmlToJson(entry)+'}'

            // http://www.nws.noaa.gov/geodata/ tells us how to understand geocode elements
            // Look at the various info.area elements - if the "polygon" element is null see if we can find an info.area.geocode we understand well enough to expand
            broadcastCapEvent(json_text, source_feed)

            completed_ok = true;
          }
          else {
            log.warn("No valid CAP from ${cap_link} -- consider improving rules for handling this");
            feedFeedbackService.publishFeedEvent(source_feed,
                                                 source_id,
                                                 "No valid CAP found at ${cap_link}");
            completed_ok = true;
          }
        }
        else {
          log.warn("${cap_link} (content type ${detected_content_type}) - unable to parse or no identifier present");
          completed_ok = true;
        }
      }
      catch ( java.net.SocketTimeoutException ste ) {
        log.error("Connection timeout: ${cap_link} ${context} ${ste.message}");
        // sleep for .5s before retry
        Thread.sleep(500)

        feedFeedbackService.publishFeedEvent(source_feed,
                                             source_id,
                                             "TIMEOUT processing CAP URL (retry ${retries}/elapsed ${System.currentTimeMillis()-ts_1}): ${cap_link} ${ste.message}");

        retries++;
      }
      catch ( Exception e ) {
        log.error("problem handling cap alert ${cap_link} ${context} ${e.message}");
        // sleep for .5s before retry
        Thread.sleep(500)

        feedFeedbackService.publishFeedEvent(source_feed,
                                             source_id,
                                             "problem processing CAP URL (retry ${retries}/elapsed ${System.currentTimeMillis()-ts_1}): ${cap_link} ${e.message}");


        retries++;
      }
      finally {
        log.debug("CAP URL Checker Task Complete");
      }
    }
  }

  private String findStylesheet(byte[] alert_bytes) {
    String stylesheet=null;
    int start_of_pi =  indexOf(alert_bytes, stylesheet_pattern, 350);
    if ( start_of_pi >= 0 ) {
      int start_of_stylesheet = start_of_pi+22;
      byte quote_char = alert_bytes[start_of_stylesheet]
      int end_of_stylesheet = 0;
      for ( int i=start_of_stylesheet+1; (end_of_stylesheet==0)&&(i<250) ; i++ ) {
        if ( alert_bytes[i] == quote_char ) {
          end_of_stylesheet = i;
        }
        else {
          // println("skip ${alert_bytes[i] as char} (${alert_bytes[i]} looking for ${quote_char})");
        }
      }
      if ( end_of_stylesheet != 0 ) {
        stylesheet = new String(Arrays.copyOfRange(alert_bytes, start_of_stylesheet+1, end_of_stylesheet))
      }
    }
    return stylesheet;
  }

  private static int indexOf(byte[] data, byte[] pattern, max_search) {
    int[] failure = computeFailure(pattern);

    int j = 0;

    for (int i = 0; ( ( i < data.length ) && ( i < max_search ) ); i++) {
      while (j > 0 && pattern[j] != data[i]) {
        j = failure[j - 1];
      }
      if (pattern[j] == data[i]) { 
        j++; 
      }
      if (j == pattern.length) {
        return i - pattern.length + 1;
      }
    }
    return -1;
  }

  /**
   * Computes the failure function using a boot-strapping process,
   * where the pattern is matched against itself.
   */
  private static int[] computeFailure(byte[] pattern) {
    int[] failure = new int[pattern.length];

    int j = 0;
    for (int i = 1; i < pattern.length; i++) {
      while (j>0 && pattern[j] != pattern[i]) {
        j = failure[j - 1];
      }
      if (pattern[j] == pattern[i]) {
        j++;
      }
      failure[i] = j;
    }

    return failure;
  }
  
  def domNodeToString(node) {
    String xml_text =  groovy.xml.XmlUtil.serialize(node)
    xml_text
  }

  private void broadcastCapEvent(String json_event, String feed_code) {
    try {
      def result = rabbitMessagePublisher.send {
              exchange = "CAPExchange"
              routingKey = 'CAPAlert.'+feed_code
              body = json_event
      }
      log.debug("Result of Rabbit RPC publish: ${result} (Routing key was CAPAlert.${feed_code})");
    }
    catch ( Exception e ) {
      log.error("Problem trying to publish to rabbit",e);
    }
  }
}
