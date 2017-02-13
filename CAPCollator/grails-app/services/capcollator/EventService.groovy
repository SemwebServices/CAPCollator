package capcollator

import grails.transaction.Transactional

@Transactional
class EventService {

  def registerEvent(String eventCode, long timestamp) {
    log.debug("registerEvent ${eventCode} ${timestamp}");
  }
}
