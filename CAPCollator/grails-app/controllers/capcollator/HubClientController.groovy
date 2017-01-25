package capcollator

import grails.plugin.springsecurity.annotation.Secured

class HubClientController {

  def index() {
    log.debug("HubClientController::index params:${params} ct:${request.contentType}");
    def result = [:]

    switch ( request.contentType ) {

      case 'application/json':
        log.debug("Found JSON in request: ${request.JSON}");
        break;

      case 'application/xml':
        log.debug("Found XML in request: ${request.XML}");
        break;

      default:
        log.debug("Unhandled content type ${request.contentType}");
    }

    if ( params.hub?.challenge ) {
      render(status: 200, text:params.hub.challenge)
    }
    else {
      render(status: 200, text:'OK')
    }
  }
}
