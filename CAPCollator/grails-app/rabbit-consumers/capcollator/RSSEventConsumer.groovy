package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class RSSEventConsumer {

  def rssEventHandlerService

  static rabbitConfig = [
    "queue": "CAPCollatorRSSQueue"
  ]

  def handleMessage(def body, MessageContext context) {
    rssEventHandlerService.handleNotification(body,context);
  }
}
