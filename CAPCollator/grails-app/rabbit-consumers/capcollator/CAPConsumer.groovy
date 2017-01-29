package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class CAPConsumer {

  def capEventHandlerService

  static rabbitConfig = [
    "queue": "CAPCollatorQueue"
  ]

  def handleMessage(def body, MessageContext context) {
    capEventHandlerService.handleNotification(body,context);
  }
}
