package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class RSSEventConsumer {

  private static int active_counter = 0;
  def rssEventHandlerService

  static rabbitConfig = [
    "queue": "CAPCollatorRSSQueue"
    // "transacted": true
  ]

  def handleMessage(def body, MessageContext context) {
    log.debug("RSSEventConsumer::handleMessage counter=${active_counter++}");
    rssEventHandlerService.handleNotification(body,context);
    active_counter--;
  }
}
