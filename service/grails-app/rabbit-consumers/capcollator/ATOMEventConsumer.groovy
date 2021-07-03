package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class ATOMEventConsumer {

  def atomEventHandlerService
  private static int active_counter = 0;

  static rabbitConfig = [
    "queue": "CAPCollatorATOMQueue"
    // "transacted": true
  ]

  def handleMessage(def body, MessageContext context) {
    log.debug("ATOMEventConsumer::handleMessage counter=${active_counter++}");
    atomEventHandlerService.handleNotification(body,context);
    active_counter--;
  }
}
