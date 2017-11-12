package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class RSSEventConsumer {

  def rssEventHandlerService

  static rabbitConfig = [
    "queue": "CAPCollatorRSSQueue"
  ]

  def handleMessage(def body, MessageContext context) {
    log.debug("RSSEventConsumer::handleMessage");
    rssEventHandlerService.handleNotification(body,context);
  }
}
