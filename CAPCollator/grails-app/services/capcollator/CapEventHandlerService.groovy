package capcollator

import grails.transaction.Transactional

@Transactional
class CapEventHandlerService {

  def process(cap_url) {
    log.debug("CapEventHandlerService::process ${cap_url}");
  }
}
