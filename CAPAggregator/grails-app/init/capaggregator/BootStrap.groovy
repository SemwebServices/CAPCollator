package capaggregator

import grails.util.Environment 

class BootStrap {

  def CAPIndexingService
  def grailsApplication
  def servletContext
  def capCollatorSystemService

  def init = { servletContext ->
    log.info("Starting CAPCollator. ${grailsApplication.metadata?.getApplicationName()} ${grailsApplication.metadata?.getApplicationVersion()}");
    capCollatorSystemService.init()
    CAPIndexingService.freshen()
  }

  def destroy = {
  }
}
