package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class CAPSubscriptionMatchConsumer {

  def grailsApplication
  def staticFeedService

  static rabbitConfig = [
    "exchange": "CAPExchange",
    "binding": "CAPSubMatch.#"
  ]

  def handleMessage(def body, MessageContext context) {
    log.debug("CAPSubscriptionMatchConsumer::handleMessage(${body},${context})");
    // If we have a static feeds DIR configured, log alerts in that file.
    if ( grailsApplication.config.staticFeedsDir != null ) {
      log.debug("Static feed dir configured to be ${grailsApplication.config.staticFeedsDir}");
      
      // Check for feed directory
      staticFeedService.update(body, context);
    }
  }
}
