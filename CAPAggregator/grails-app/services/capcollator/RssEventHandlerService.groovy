package capcollator

import grails.gorm.transactions.*
import static groovy.json.JsonOutput.*

@Transactional
class RssEventHandlerService {

  def capUrlHandlerService
  def feedFeedbackService

  def handleNotification(body,context) {
    log.debug("RssEventHandlerService::handleNotification(...,${context})");
    log.debug("${context.properties.headers}");

    String source_feed = context.properties.headers['feed-code'];

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
        capUrlHandlerService.handleNotification(link, context)
      }
    }
    catch ( Exception e ) {
      log.error("problem handling cap alert ${body} ${context} ${e.message}",e);
      feedFeedbackService.publishFeedEvent(source_feed,
                                           null,
                                           "problem processing Atom event: ${e.message}");

    }

  }
}
