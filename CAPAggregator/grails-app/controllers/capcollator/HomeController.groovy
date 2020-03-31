package capcollator

import grails.plugin.springsecurity.annotation.Secured
import grails.converters.JSON

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

  def about() {
    def result = [:]
    result
  }


  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def login() {
    redirect(controller:'subscriptions', action:'index');
  }

  def explorer1() {
    def result=[:]
    result
  }

  def info() {
    def result=[:]
    result.headers=[:]
    request.getHeaderNames().each { header_name ->
      result.headers[header_name] = request.getHeader(header_name);
    }
    render result as JSON
  }
}
