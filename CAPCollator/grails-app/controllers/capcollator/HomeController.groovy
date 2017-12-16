package capcollator

import grails.plugin.springsecurity.annotation.Secured

class HomeController {

  def eventService

  def index() { 
    // def result = [:]
    // result.statsCache = eventService.getStatsCache()
    // result
    redirect(controller:'subscriptions', action:'index');
  }

  def status() { 
    def result = [:]
    result.statsCache = eventService.getStatsCache()
    result
  }

  def topic() {
    log.debug("Topic: ${params}");
    def result = [:]
    result
  }


  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def login() {
    redirect(controller:'subscriptions', action:'index');
  }

}
