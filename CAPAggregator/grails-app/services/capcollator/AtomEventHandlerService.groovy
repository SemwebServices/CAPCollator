package capcollator

import grails.gorm.transactions.*
import static groovy.json.JsonOutput.*
import static grails.async.Promises.*

@Transactional
class AtomEventHandlerService {

  def capUrlHandlerService
  def feedFeedbackService

  private static long LONG_ALERT_THRESHOLD = 2000;

  def handleNotification(body,context) {
    log.debug("AtomEventHandlerService::handleNotification(...,${context})");
    log.debug("${context.properties.headers}");
    String source_feed = context.properties.headers['feed-code'];

    task {
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
        
        log.debug("AtomEventHandlerService::handleNotification processing ${list_of_links.size()} elements");
  
        list_of_links.each { link ->
          log.debug("Looking in link attribute for cap ${link}");
  
          // Different feeds behave differently wrt properly setting the type attribute. 
          // Until we get to grips a little better - try and parse every link - and if we manage to parse XML, see if the root node is a cap element
          if ( ( ( link.get('@type') != null ) && ( 'application/cap+xml'.equals(link.get('@type')) ) ) ||
               ( ( list_of_links != null ) ) ) {
  
            log.debug("  -> processing link type=${link.get('@type')})");
            String url = link.get('@href');
            if ( url ) {
              capUrlHandlerService.handleNotification(url, context)
            }
            else {
              log.error("No url for link ${link}");
            }
          }
          else {
            log.error("Unable to find CAP link in ${body} OR other error parsing XML");
          }
        }
      }
      catch ( Exception e ) {
        log.error("problem handling cap alert ${body} ${context} ${e.message}",e);
        feedFeedbackService.publishFeedEvent(source_feed,
                                             null,
                                             "problem processing Atom event: ${e.message}");

      }
  
      if ( num_cap_files_found == 0 ) {
        eventService.registerEvent('ATOMEntryWithoutValidCapFile',System.currentTimeMillis());
      }
      log.debug("ATOM Event Handler complete");
    }
    log.debug("handleNotification complete");
  }
  
}
