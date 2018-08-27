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
    // log.debug("CAPSubscriptionMatchConsumer::handleMessage(${body},${context})");
    // log.debug(context.properties?.toString());
    // log.debug(context.envelope?.toString());
    log.debug(context.envelope.routingKey);
    // If we have a static feeds DIR configured, log alerts in that file.
    if ( ( grailsApplication.config.staticFeedsDir != null ) &&
         ( context.envelope.routingKey != null ) ) {
      // Check for feed directory
      staticFeedService.update(context.envelope.routingKey, body, context);
    }
  }
}
