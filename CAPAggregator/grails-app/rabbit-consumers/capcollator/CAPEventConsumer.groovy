package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class CAPEventConsumer {

  def capEventHandlerService

  static rabbitConfig = [
    "exchange": "CAPExchange",
    "binding": "CAPAlert.#"
    // "transacted": true
  ]

  def handleMessage(def body, MessageContext context) {
    log.info("CAPEventConsumer::handleMessage");
    capEventHandlerService.process(body);
  }
}
