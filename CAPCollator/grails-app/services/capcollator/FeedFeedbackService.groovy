package capcollator

import grails.transaction.Transactional
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import java.util.Iterator
import static groovy.json.JsonOutput.*


@Transactional
class FeedFeedbackService {

  RabbitMessagePublisher rabbitMessagePublisher

  // Publish an event which describes the outcome of this entry being processed - this allows us to feed back to the event emitter
  // If there was a problem processing the event, or other outcome
  public void publishFeedEvent(String source_feed,
                               String event_id,
                               String outcome) {
    this.publishFeedEvent(source_feed,event_id,[message:outcome]);
  }

  public void publishFeedEvent(String source_feed,
                               String event_id,
                               Map info) {
    log.debug("publishFeedEvent(${source_feed},${event_id},${info})");
    // Publish to CAPExchange with routing key FFFeedback.feedid
  }

}
