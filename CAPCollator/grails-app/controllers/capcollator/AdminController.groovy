package capcollator

import grails.plugin.springsecurity.annotation.Secured

class AdminController {

  def CAPIndexingService

  def index() { 
  }

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def registerConsumer() { 
  }

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def reindex() { 
    log.debug("AdminController::Reindex");
    CAPIndexingService.reindexSubscriptions()
    redirect(controller:'home',action:'index');
  }
}
