package capcollator

import grails.plugin.springsecurity.annotation.Secured

class HubClientController {

  def capEventHandlerService

  def index() {

    log.debug("HubClientController::index params:${params} ct:${request.contentType}");

    def result = [:]

    switch ( request.contentType ) {

      case 'application/json':
        log.debug("Found JSON in request: ${request.JSON}");
        if ( request.JSON.link ) {
          def cap_link = null
          if ( request.JSON.link instanceof List ) {
            request.JSON.link.each { link_entry -> 
              if ( ( cap_link == null ) && ( link_entry.'@rel' == 'alternate' ) ) {
                cap_link = link_entry.'@href'
              }
            }
          }
          else {
            if ( request.JSON.link.'@rel' == 'alternate' ) {
              cap_link = request.JSON.link.'@href'
            }
          }
          log.debug("Got cap link ${cap_link}");
          capEventHandlerService.process(cap_link)
        }
        else {
          log.debug("Can't find link element");
        }
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
