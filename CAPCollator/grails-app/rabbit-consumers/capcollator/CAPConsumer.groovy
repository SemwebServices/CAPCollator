package capcollator

import com.budjb.rabbitmq.consumer.MessageContext

class CAPConsumer {

    def handleMessage(def body, MessageContext context) {
        log.debug("CAPConsumer::handleMessage(${body},${context}");
    }
}
