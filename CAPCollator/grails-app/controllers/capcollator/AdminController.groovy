package capcollator

import grails.plugin.springsecurity.annotation.Secured
import static grails.async.Promises.*

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
      String target_url = params.subUrl
      log.debug("Attempting to parse list of subs from \"${target_url}\"");

      def import_promise = task {
        subsImportService.loadSubscriptionsFrom(target_url)
      }

      import_promise.onComplete {
        log.debug("subsImportService.loadSubscriptionsFrom - completed");
      }

      import_promise.onError { Throwable err ->
        println "An error occured ${err.message}"
      }

      log.debug("Started background loader task");
    }

    return [status:subsImportService.getStatus()];

  }

}
