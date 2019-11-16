package capcollator

import grails.transaction.Transactional
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.ContentType

class WebHookService {

  static transactional = false
  def grailsApplication

  public checkHooks(String feed, Map alert_info) {
    // log.debug("WebHookService::checkHooks(${feed}, ${alert_info}");

    Subscription s = Subscription.findBySubscriptionId(feed);
    if ( s ) {

      s.hooks.each { hook ->
         HTTPBuilder http = new HTTPBuilder(hook.hookUrl)

        http.request(Method.POST) {
          body = alert_info
          requestContentType = ContentType.JSON

          response.success = { resp ->
            log.debug("Webhook ${feed} ${hook.hookUrl} OK")
          }

          response.failure = { resp ->
            log.debug("Webhook ${feed} ${hook.hookUrl} FAILED ${resp.status}")
          }
        }
      }
    }
    else {
      log.warn("Unable to find subscription for feed ${feed}");
    }
  }
}
