package capcollator

class BootStrap {

  def sysusers = [
    [
     name:'admin',
     pass: System.getenv('YARM_ADM_PW')?:'ChangeMeImmediately',
     display:'Admin',
     email:'admin@semweb.co', 
     roles:['ROLE_ADMIN','ROLE_USER']
    ]
  ]

  def init = { servletContext ->
    setUpUserAccounts()
    syncSubscriptionList()
  }

  def setUpUserAccounts() {
    sysusers.each { su ->
      log.debug("test ${su.name} ${su.pass} ${su.display} ${su.roles}");
      def user = User.findByUsername(su.name)
      if ( user ) {
        if ( user.password != su.pass ) {
          log.debug("Hard change of user password from config ${user.password} -> ${su.pass}");
          user.password = su.pass;
          user.save(failOnError: true)
        }
        else {
          log.debug("${su.name} present and correct");
        }
      }
      else {
        log.debug("Create user...");
        user = new User(
                      username: su.name,
                      password: su.pass,
                      display: su.display,
                      email: su.email,
                      enabled: true).save(failOnError: true)
      }

      log.debug("Add roles for ${su.name} (${su.roles})");
      su.roles.each { r ->

        def role = Role.findByAuthority(r) ?: new Role(authority:r).save(flush:true, failOnError:true)

        if ( ! ( user.authorities.contains(role) ) ) {
          log.debug("  -> adding role ${role} (${r})");
          UserRole.create user, role
        }
        else {
          log.debug("  -> ${role} already present");
        }
      }
    }
  }


  def syncSubscriptionList() {
    try {
      def live_json_data = new groovy.json.JsonSlurper().parse(new java.net.URL('https://s3-eu-west-1.amazonaws.com/alert-hub-subscriptions/json'))
      ingestSubscriptions(live_json_data.subscriptions)
    }
    catch ( Exception e ) {
      log.error("problem syncing cap feed list",e);
    }
  }

  def ingestSubscriptions(list_of_subscriptions) {
    list_of_subscriptions.each { subscription_definition ->
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
      log.debug("Add or update subscription ${subscription_definition}");

      if ( subscription_definition.subscription?.subscriptionId && 
           ( subscription_definition.subscription?.subscriptionId.trim().length() > 0 ) ) {

        def sub = Subscription.findBySubscriptionId(subscription_definition.subscription.subscriptionId)
        def filter_type=null
        def filter_geometry=subscription_definition.subscription.areaFilter.polygonCoordinates

        if ( ( subscription_definition.subscription.areaFilter.circleCenterRadius=="none") ||
             ( subscription_definition.subscription.areaFilter.circleCenterRadius=="") ||
             ( subscription_definition.subscription.areaFilter.circleCenterRadius==null) ) {
          filter_type='polygon'
        }
        else {
          filter_type='circle'
        }

        if ( sub ) {
          log.debug("located existing subscrition for ${subscription_definition.subscription.subscriptionId}");
        }
        else {
          log.debug("New sub ${filter_type} ${filter_geometry}");
          // sub=new Subscription(
          //             subscriptionId:subscription_definition.subscription?.subscriptionId,
          //             subscriptionName: subscription_definition.subscription?.subscriptionName,
          //             subscriptionUrl:subscription_definition.subscription?.subscriptionUrl,
          //             filterType:filter_type,
          //             filterGeometry:filter_geometry).save(flush:true, failOnError:true);
        }
      }

    }
  }

  def destroy = {
  }

}
