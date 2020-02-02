package capaggregator

import grails.util.Environment 

class BootStrap {

  def CAPIndexingService
  def grailsApplication
  def servletContext
  def capCollatorSystemService

  def init = { servletContext ->

      capCollatorSystemService.init()
      CAPIndexingService.freshen()

  }

  def destroy = {
  }
}
