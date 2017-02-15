package capcollator

import grails.plugin.springsecurity.annotation.Secured

class SubscriptionsController {

  def CAPIndexingService

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

  def details() {
    def result=[:]
    result.subscription = Subscription.get(params.id)
    result
  }

}
