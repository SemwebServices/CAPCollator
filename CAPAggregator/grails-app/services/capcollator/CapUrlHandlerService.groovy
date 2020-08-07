package capcollator

import grails.gorm.transactions.*
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import static groovy.json.JsonOutput.*
import org.apache.commons.collections4.map.PassiveExpiringMap;
import groovyx.net.http.HttpBuilder
import groovyx.net.http.FromServer
import groovyx.net.http.ChainedHttpConfig
import static groovyx.net.http.HttpBuilder.configure


/**
 * This is where all CAP URLs detected in feeds, be they atom, RSS, or other source types come to be resolved.
 */
@Transactional
class CapUrlHandlerService {

  RabbitMessagePublisher rabbitMessagePublisher
  def eventService
  def staticFeedService
  def feedFeedbackService

  // Alerts can live in the cache for up to 2 minutes
  private Map parsed_alert_cache = Collections.synchronizedMap(new PassiveExpiringMap(1000*60*2))

  private static final long LONG_ALERT_THRESHOLD = 2000;
  private static final int MAX_RETRIES = 3;

  def process(cap_url) {
    log.debug("RssEventHandlerService::process ${cap_url}");
  }

  def handleNotification(link,context) {

    log.debug("RssEventHandlerService::handleNotification(...,${context})");
    log.debug("${context.properties.headers}");

    def ts_1 = System.currentTimeMillis();
    def cap_link = link.toString()

    String source_feed = context.properties.headers['feed-code'];
    String source_id = context.properties.headers['feed-id'];
    String is_official = context.properties.headers['feed-is-official'];
    String original_cap_link = cap_link

    LocalFeedSettings lfs = LocalFeedSettings.findByUriname(source_feed)
    if ( lfs != null ) {
      log.debug("Have override local feed settings for ${lfs.uriname}");

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
        // java.net.URL cap_link_url = new java.net.URL(cap_link)
        // java.net.URLConnection conn = cap_link_url.openConnection()
        // conn.setConnectTimeout(5000);
        // conn.setReadTimeout(5000);
        def detected_content_type = null
        HttpBuilder http_client = configure {
          request.uri = cap_link
          client.clientCustomizer { HttpURLConnection conn ->
            conn.connectTimeout = 5000;
          }
        }

        http_client.head {
          response.success { FromServer resp ->
            detected_content_type = FromServer.Header.find( resp.headers, 'Content-Type')?.value
          }

          response.failure {
            log.warn("Unable to get last modified from server");
          }
        }

        log.debug("URL Connection reports content type ${detected_content_type}");

        if ( detected_content_type &&
             ( detected_content_type.toLowerCase().startsWith('text/xml') ||
               detected_content_type.toLowerCase().startsWith('application/octet-stream') ||   // Because of http://www.gestiondelriesgo.gov.co
               detected_content_type.toLowerCase().startsWith('application/xml') ) ) {

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
          }

          def parser = new XmlSlurper()

          def fetch_completed = System.currentTimeMillis();

          // byte[] alert_bytes = conn.getInputStream().getBytes();
          byte[] alert_bytes = response_content.getBytes()

          String cached_alert_xml = staticFeedService.writeAlertXML(alert_bytes, source_feed, new Date(ts_2))

          parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false) 
          parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
          // def parsed_cap = parser.parse(conn.getInputStream())

          def parsed_cap = parser.parse(new ByteArrayInputStream(alert_bytes));
          String alert_uuid = java.util.UUID.randomUUID().toString()

          if ( ( parsed_cap != null ) && 
               ( parsed_cap.identifier != null ) ) {

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
              log.info("Alert processing exceeded LONG_ALERT_THRESHOLD(${elapsed}) ${cap_link_url}");
            }

            alert_metadata.PrivateSourceUrl = cap_link
            alert_metadata.SourceUrl = original_cap_link
            alert_metadata.capCollatorUUID = alert_uuid;
            alert_metadata.sourceFeed = source_feed;
            alert_metadata.sourceIsOfficial = is_official;
            alert_metadata.cached_alert_xml = cached_alert_xml;

            // alertCacheService.put(alert_uuid,alert_bytes);

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
          log.warn("${cap_link} (content type ${detected_content_type}) seems not to be XML and therefore cannot be a CAP message - skipping");
          completed_ok = true;
        }
      }
      catch ( Exception e ) {
        log.error("problem handling cap alert ${cap_link} ${context} ${e.message}",e);
        // sleep for .5s before retry
        Thread.sleep(500)

        feedFeedbackService.publishFeedEvent(source_feed,
                                             source_id,
                                             "problem processing CAP event (retry ${retries}): ${cap_link} ${e.message}");


        retries++;
      }
      finally {
        log.debug("CAP URL Checker Task Complete");
      }
    }
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
