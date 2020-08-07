package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class CAPSubscriptionMatchConsumer {

  def grailsApplication
  def staticFeedService
  def webHookService

  private static int active_counter = 0;

  static rabbitConfig = [
    "exchange": "CAPExchange",
    "binding": "CAPSubMatch.#",
    "transacted": true
  ]

  def handleMessage(def body, MessageContext context) {
    // log.debug("CAPSubscriptionMatchConsumer::handleMessage(${body},${context})");
    // log.debug(context.properties?.toString());
    // log.debug(context.envelope?.toString());
    String[] routing_key_components = context.envelope.routingKey.split('\\.');
    log.debug("CAPSubscriptionMatchConsumer::handleMessage - Routing key components: ${routing_key_components} / ${active_counter++}")
    // If we have a static feeds DIR configured, log alerts in that file.
    try {
      if ( ( grailsApplication.config.staticFeedsDir != null ) &&
           ( context.envelope.routingKey != null ) ) {
        // Check for feed directory
        staticFeedService.update(context.envelope.routingKey, body, context);
      }
      else {
        log.warn("No static feed dir(${grailsApplication.config.staticFeedsDir}) OR no routing key(${context.envelope.routingKey}). No action on sub match");
      }
    }
    catch ( Exception e ) {
      log.error("Problem processing static feed",e);
    }

    try {
      if ( routing_key_components.length == 2 ) {
        webHookService.checkHooks(routing_key_components[1], body);
      }
    }
    catch ( Exception e ) {
      log.error("Problem processing static feed -- Web Hooks",e);
    }
    finally {
      active_counter--;
    }
  }
}
