package capcollator

import static groovyx.net.http.HttpBuilder.configure

import grails.gorm.transactions.*

class WebHookService {

  static transactional = false
  def grailsApplication

  public checkHooks(String feed, Map alert_info) {
    // log.debug("WebHookService::checkHooks(${feed}, ${alert_info}");

    Subscription.withTransaction {
      Subscription s = Subscription.findBySubscriptionId(feed);
      if ( s ) {
        s.hooks.each { hook ->
          def builder = configure {
            request.uri = hook.hookUrl
          }
          builder.post {
            request.contentType = 'application/json'
            request.body = alert_info
    
            // handle response
            response.success { fromServer, body ->
              log.debug("Webhook ${feed} ${hook.hookUrl} OK")
            }
  
            response.error { fromServer, body ->
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
}
