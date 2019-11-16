package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class ATOMEventConsumer {

  def atomEventHandlerService

  static rabbitConfig = [
    "queue": "CAPCollatorATOMQueue"
  ]

  def handleMessage(def body, MessageContext context) {
    log.debug("ATOMEventConsumer::handleMessage");
    atomEventHandlerService.handleNotification(body,context);
  }
}
