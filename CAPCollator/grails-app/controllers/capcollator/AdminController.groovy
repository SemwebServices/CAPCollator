package capcollator

import grails.plugin.springsecurity.annotation.Secured

class AdminController {

  def CAPIndexingService
  def subsImportService

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

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def syncSubList() {
    if (params.subUrl) {
      log.debug("Attempting to parse list of subs from \"${params.subUrl}\"");
      subsImportService.loadSubscriptionsFrom(params.subUrl)
    }

    return [status:subsImportService.getStatus()];

  }

}
