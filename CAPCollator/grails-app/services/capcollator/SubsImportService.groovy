package capcollator

import grails.transaction.Transactional

@Transactional
class SubsImportService {


  def status = [
    running:false,
    progress:[]
  ]

  // A home for running subscription imports outside of controllers for progress reporting
  def getStatus() {
    return status;
  }

  def loadSubscriptionsFrom(String url) {

    boolean proceed=false;
    synchronized(status) {
      if ( status.running==false ) {
        status.running=true
        proceed=true
        def job_progress=[
          startTime:System.currentTimeMillis(),
          endTime:null,
          url: url,
          status: 'idle',
          numDefined:0,
          numLoaded:0,
          numCreated:0,
          numUpdayed:0,
          numErrors:0
        ]
        status.progress.add(job_progress);
      }
    }

    if ( proceed ) {

  
      try {
        def list_of_subscriptions = new groovy.json.JsonSlurper().parse(new java.net.URL(url))
        int num_subscriptions = list_of_subscriptions.subscriptions.size();
  
        long total_processing_time = 0;
  
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
  
          long sub_start_time = System.currentTimeMillis();
  
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
                          filterGeometry:filter_geometry,
                          languageOnly:subscription_definition.subscription?.languageOnly,
                          highPriorityOnly:subscription_definition.subscription?.highPriorityOnly,
                          officialOnly:subscription_definition.subscription?.officialOnly,
                          xPathFilterId:subscription_definition.subscription?.xPathFilterId
                       ).save(flush:true, failOnError:true);
            }
          }
  
          result.counter++
          long elapsed = System.currentTimeMillis() - sub_start_time;
          total_processing_time += elapsed;
          long average_processing_time = total_processing_time / result.counter
          long long_running_threshold = average_processing_time * 1.5
  
          if ( elapsed > long_running_threshold ) {
            log.info("${subscription_definition.subscription?.subscriptionId} took ${elapsed}ms to process, average is ${average_processing_time}");
          }
  
          if ( ( result.counter % 25 == 0 ) || ( result.counter == num_subscriptions ) ) {
            log.info("AdminController::syncSubList - processed ${result.counter} of ${num_subscriptions} sub definitions so far. Avg processing time ${average_processing_time}");
          }
  
        }
      }
      catch ( Exception e ) {
        log.error("problem processing subscription list",e);
      }
      finally {
        synchronized(status) {
          status.running=false;
        }
      }
    }
  }
}