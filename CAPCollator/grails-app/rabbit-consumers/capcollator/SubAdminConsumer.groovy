package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class SubAdminConsumer {

  def grailsApplication
  def staticFeedService

  static rabbitConfig = [
    "exchange": "CAPExchange",
    "binding": "CAPSubAdmin.#"
  ]

  def handleMessage(def body, MessageContext context) {
    log.debug("SubAdminConsumer::handle - key = ${context.envelope.routingKey}");
    staticFeedService.initialiseFeed(context.envelope.routingKey);
  }
}
