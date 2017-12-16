package capcollator

import grails.plugin.springsecurity.annotation.Secured

class SubscriptionsController {

  def CAPIndexingService
  def ESWrapperService

  def index() { 
    def result=[:]
    def qry_params = [:]
    def base_sub_qry = 'from Subscription as s'

    if ( ( params.q != null ) && ( params.q.length() > 0 ) ) {
      base_sub_qry += ' where lower(s.subscriptionId) like :a or lower(s.subscriptionName) like :a or lower(s.subscriptionUrl) like :a'
      qry_params.a = "%${params.q.toLowerCase()}%".toString()
    }
    def order_by_clause = ' order by s.id'

    def select_control_params = [
      max: params.max ?: 10,
      offset: params.offset ?: 0
    ]

    result.totalSubscriptions = Subscription.executeQuery('select count(s) '+base_sub_qry,qry_params)[0]
    result.subscriptions = Subscription.executeQuery('select s '+base_sub_qry+order_by_clause,qry_params,select_control_params)

    log.debug("found ${result.totalSubscriptions} feeds");

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
    result.subscription = Subscription.findBySubscriptionId(params.id)
    String[] indexes_to_search = [ 'alerts' ]
    String es_query = '''{
         "bool": {
           "must": {
             "match": { "AlertMetadata.MatchedSubscriptions": "'''+params.id+'''"}
           }
         } 
       }'''

    // result.latestAlerts = ESWrapperService.search(indexes_to_search,es_query);
    result.latestAlerts = ESWrapperService.search(indexes_to_search,es_query,0,100,'AlertBody.sent','desc');
    result
  }

}
