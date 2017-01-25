package capcollator

import grails.plugin.springsecurity.annotation.Secured

class AdminController {

  def index() { 
  }

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def registerConsumer() { 
  }
}
