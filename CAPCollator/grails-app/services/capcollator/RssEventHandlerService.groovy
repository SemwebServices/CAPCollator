package capcollator

import grails.transaction.Transactional
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import static groovy.json.JsonOutput.*

@Transactional
class RssEventHandlerService {

  RabbitMessagePublisher rabbitMessagePublisher
  def eventService
  def alertCacheService

  def process(cap_url) {
    log.debug("RssEventHandlerService::process ${cap_url}");
  }


  def handleNotification(body,context) {
    log.debug("RssEventHandlerService::handleNotification(...,${context})");
    log.debug("${context.properties.headers}");

    def ts_1 = System.currentTimeMillis();

    int num_cap_files_found = 0;

    try {
      def list_of_links = null
      // Json will be different if we have just 1 body.link - so wrap if needed
      if ( body.link instanceof List ) {
        list_of_links = body.link
      }
      else {
        if ( body.link != null ) {
          list_of_links = [ body.link ]
        }
      }
      
      log.debug("RssEventHandlerService::handleNotification processing ${list_of_links.size()} elements");

      list_of_links.each { link ->

        log.debug("Looking in link attribute for cap ${link}");

        // Different feeds behave differently wrt properly setting the type attribute. 
        // Until we get to grips a little better - try and parse every link - and if we manage to parse XML, see if the root node is a cap element

        log.debug("  -> processing link ${link}");

        try {
          def ts_2 = System.currentTimeMillis();
          def cap_link = link.toString()

          log.debug("test ${cap_link}");
          java.net.URL cap_link_url = new java.net.URL(cap_link)
          java.net.URLConnection conn = cap_link_url.openConnection()
          conn.setConnectTimeout(5000);
          conn.setReadTimeout(5000);
            
          def detected_content_type = conn.getContentType()
          log.debug("URL Connection reports content type ${detected_content_type}");

          if ( detected_content_type &&
               ( detected_content_type.toLowerCase().startsWith('text/xml') ||
                 detected_content_type.toLowerCase().startsWith('application/octet-stream') ||   // Because of http://www.gestiondelriesgo.gov.co
                 detected_content_type.toLowerCase().startsWith('application/xml') ) ) {

            def parser = new XmlSlurper()
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false) 
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            def parsed_cap = parser.parse(conn.getInputStream())
  
            if ( parsed_cap.identifier ) {
              num_cap_files_found++
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
              alert_metadata.CCHistory=[]
              alert_metadata.CCHistory.add(["event":"CAPCollator notified","timestamp":ts_1]);
              alert_metadata.CCHistory.add(["event":"CAPCollator fetch alert","timestamp":ts_2]);
              alert_metadata.CCHistory.add(["event":"CAPCollator publish CAP event","timestamp":ts_3]);
              alert_metadata.SourceUrl = cap_link

              if ( latest_expiry && latest_expiry.trim().length() > 0 )
                alert_metadata.Expires = latest_expiry

              if ( latest_effective && latest_effective.trim().length() > 0 )
                alert_metadata.Effective = latest_effective

              // Render the cap object as JSON - We wrap the converted message in an object so we can add some metadata about
              // processing time - for stats / tracking the delay through the system
              String json_text = '{ "AlertMetadata":'+toJson(alert_metadata)+',"AlertBody":'+capcollator.Utils.XmlToJson(entry)+'}'
  
              // http://www.nws.noaa.gov/geodata/ tells us how to understand geocode elements
              // Look at the various info.area elements - if the "polygon" element is null see if we can find an info.area.geocode we understand well enough to expand
              broadcastCapEvent(json_text, context.properties.headers)
            }
            else {
              log.warn("No valid CAP from ${cap_link} -- consider improving rules for handling this");
            }
          }
          else {
            log.warn("${cap_link} (content type ${detected_content_type}) seems not to be XML and therefore cannot be a CAP message - skipping");
          }
        }
        catch ( Exception e ) {
          log.error("problem handling cap alert ${body} ${context} ${e.message}");
        }
        finally {
          log.debug("ATOM Checker Task Complete");
        }
      }
    }
    catch ( Exception e ) {
      log.error("problem handling cap alert ${body} ${context} ${e.message}",e);
    }

    if ( num_cap_files_found == 0 ) {
      eventService.registerEvent('ATOMEntryWithoutValidCapFile',System.currentTimeMillis());
    }
  }
  

  

  def domNodeToString(node) {
    //Create stand-alone XML for the entry
    String xml_text =  groovy.xml.XmlUtil.serialize(node)
    xml_text
  }

  def broadcastCapEvent(json_event, headers) {
    // log.debug("broadcastCapEvent ${json_event}");
    try {
      def result = rabbitMessagePublisher.send {
              exchange = "CAPExchange"
              // headers = [
              //   'feed-id':feed_id,
              //   'entry-id':entry.id,
              //   'feed-url':entry.ownerFeed.baseUrl
              // ]
              routingKey = 'CAPAlert.'+(headers['feed-id'])
              body = json_event
      }
      log.debug("Result of Rabbit RPC publish: ${result} (Routing key was CAPAlert.${headers['feed-id']})");
    }
    catch ( Exception e ) {
      log.error("Problem trying to publish to rabbit",e);
    }
  }
}
