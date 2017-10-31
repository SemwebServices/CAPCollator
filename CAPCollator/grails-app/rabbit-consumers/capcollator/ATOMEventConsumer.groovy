package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class ATOMEventConsumer {

  def atomEventHandlerService

  static rabbitConfig = [
    "queue": "CAPCollatorATOMQueue"
  ]

  def handleMessage(def body, MessageContext context) {
    atomEventHandlerService.handleNotification(body,context);
  }
}
