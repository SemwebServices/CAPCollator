package capcollator

import grails.transaction.Transactional

@Transactional
class StaticFeedService {


  def update(body, context) {
    log.debug("staticFeedService.update(${body},${context}) -${grailsApplication.config.staticFeedsDir}");
  }

}
