package capcollator

import grails.plugin.springsecurity.annotation.Secured

class SubscriptionController {

  def CAPIndexingService

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def index() { 
    def result=[:]
    result.subscriptions = Subscription.executeQuery('select s from Subscription as s');
    result
  }


}
