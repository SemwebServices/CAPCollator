package capcollator

import grails.transaction.Transactional
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher

@Transactional
class CapEventHandlerService {

  RabbitMessagePublisher rabbitMessagePublisher

  def process(cap) {
    log.debug("CapEventHandlerService::process ${cap}");
  }
}
