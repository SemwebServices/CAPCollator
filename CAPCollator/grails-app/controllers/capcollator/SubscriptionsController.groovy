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
    def sub=Subscription.get(params.id)
    sub.save(flush:true, failOnError:true);
    redirect(action:'index');
  }

}
