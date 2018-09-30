package capcollator

import grails.plugin.springsecurity.annotation.Secured

class AdminController {

  def CAPIndexingService

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
    def result = [:]
    result.counter=0;
    if (params.subUrl) {
      log.debug("Attempting to parse list of subs from \"${params.subUrl}\"");
      try {
        def list_of_subscriptions = new groovy.json.JsonSlurper().parse(new java.net.URL(params.subUrl))
        list_of_subscriptions.subscriptions.each { subscription_definition ->
          //   "subscriptionId" : "country-ae-city-swic1190-lang-en",
          //   "subscriptionName" : "Official Public alerts for Dubai in country-ae, in English",
          //   "subscriptionUrl" : "https://alert-feeds.s3.amazonaws.com/country-ae-city-swic1190-lang-en/rss.xml",
          //   "languageOnly" : "en",
          //   "highPriorityOnly" : false,
          //   "officialOnly" : true,
          //   "xPathFilterId" : "actual-public",
          //   "xPathFilter" : "//cap:status='Actual' and //cap:scope='Public'",
          //   "areaFilterId" : "country-ae-city-swic1190",
          //   "areaFilter" : {
          //     "polygonCoordinates" : [[54.8833,24.7833],[55.55,24.7833],[55.55,25.35],[54.8833,25.35],[54.8833,24.7833]],
          //     "circleCenterRadius" : "none"
          //   },
          //   "feedRssXml" : ""...
          //   "feedItemsLimit": 200
          log.debug("Add or update subscription.. ${subscription_definition.subscription.subscriptionId}");
          if ( subscription_definition.subscription &&
               subscription_definition.subscription?.subscriptionId &&
               ( subscription_definition.subscription?.subscriptionId.trim().length() > 0 ) ) {
    
            def sub = Subscription.findBySubscriptionId(subscription_definition.subscription.subscriptionId)
            def filter_type=null
            def filter_geometry=null
    
            if ( ( subscription_definition.subscription.areaFilter.circleCenterRadius=="none") ||
                 ( subscription_definition.subscription.areaFilter.circleCenterRadius=="") ||
                 ( subscription_definition.subscription.areaFilter.circleCenterRadius==null) ) {
              filter_type='polygon'
              // geo_json polygons actually have an outer array that is not present in the json file of subscriptions.
              // In order to have the DB contain real geo-json we add the outer array element here.
              filter_geometry = "[${subscription_definition.subscription.areaFilter.polygonCoordinates}]"
            }
            else {
              filter_type='circle'
              filter_geometry = "${subscription_definition.subscription.areaFilter.polygonCoordinates}"
            }
    
            if ( sub ) {
              log.debug("located existing subscrition for ${subscription_definition.subscription.subscriptionId}");
            }
            else {
              log.debug("New sub ${filter_type} ${filter_geometry}");
              sub=new Subscription(
                          subscriptionId:subscription_definition.subscription?.subscriptionId,
                          subscriptionName: subscription_definition.subscription?.subscriptionName,
                          subscriptionUrl:subscription_definition.subscription?.subscriptionUrl,
                          filterType:filter_type,
                          filterGeometry:filter_geometry).save(flush:true, failOnError:true);
            }
          }

          result.counter++
          if ( result.counter % 25 == 0 ) {
            log.info("AdminController::syncSubList - processed ${result.counter} items so far");
          }
        }
      }
      catch ( Exception e ) {
        log.error("problem processing subscription list",e);
      }
      finally {
        reindex();
      }
    }

    result
  }

}
