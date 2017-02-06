package capcollator

import grails.plugin.springsecurity.annotation.Secured

class SubscriptionsController {

  def CAPIndexingService

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def index() { 
    def result=[:]
    result.subscriptions = Subscription.executeQuery('select s from Subscription as s');
    result
  }

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def touch() {
    log.debug("Touch subscription ${params.id}");
    def sub=Subscription.get(params.id)
    sub.notes="${System.currentTimeMillis()}"
    sub.save(flush:true, failOnError:true);
    redirect(action:'index');
  }

}
