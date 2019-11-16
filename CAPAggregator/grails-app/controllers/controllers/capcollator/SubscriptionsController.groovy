package capcollator

import grails.plugin.springsecurity.annotation.Secured
import grails.rest.RestfulController 
import grails.converters.XML
import java.nio.charset.Charset

class SubscriptionsController {

  static responseFormats = [
        details: ['html', 'atom', 'rss']
  ]

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

    response.characterEncoding = 'UTF-8'

    log.debug("Charset: ${Charset.defaultCharset().name()} encoding:${System.getProperty("file.encoding")}");
    result.subscription?.hooks.each { hook ->
      // This is a bit icky - it's done to eagerly fetch the hooks
      log.debug("Hook : ${hook}");
    }

    log.debug("Subscriptions::details() ${params} ${response.format}")

    List<String> query_clauses = []
    query_clauses.add('{ "match": { "AlertMetadata.MatchedSubscriptions": "'+params.id+'"} }')

    if ( params.q ) {
      query_clauses.add('{"simple_query_string": { "query":"'+params.q+'" } }')
    }

    params.each { k,v ->
      if ( k == 'tag' ) {
        query_clauses.add('{ "match" : { "AlertMetadata.tags":"'+v+'" } }');
      }
    }

    log.debug("Joined query clauses: ${query_clauses.join(',')}");

    String index_to_search = 'alerts';
    String es_query = '''{
         "bool": {
           "must": [ 
             '''+query_clauses.join(',\n')+'''
           ]
         } 
     }'''

    result.baseurl = grailsApplication.config.SystemBaseURL
    result.max = params.max ? Integer.parseInt(params.max) : 10;
    result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

    try {
      result.latestAlerts = ESWrapperService.searchJson(index_to_search,
                                                        es_query,
                                                        result.offset,
                                                        result.max,
                                                        'evtTimestamp', 'desc');

      if ( result.latestAlerts != null ) {
        result.totalAlerts = result.latestAlerts.resultsTotal
        result.rows = result.latestAlerts.hits
      }
    }
    catch ( Exception e ) {
      log.error("Problem running ES Query to find alerts for sub query ${e.message}",e);
    }


    respond result
  }

  def rss() {
    def result=[:]
    result.subscription = Subscription.findBySubscriptionId(params.id)

    response.characterEncoding = 'UTF-8'

    def query_clause='';
    if ( params.q ) {
      query_clause = ',{"simple_query_string": { "query":"'+params.q+'" } }'
    }
    String[] index_to_search = 'alerts'
    String es_query = '''{
      "query": {
         "bool": {
           "must": [ 
             { "match": { "AlertMetadata.MatchedSubscriptions": "'''+params.id+'''"} }
             '''+query_clause+'''
           ]
         } 
       }
     }'''

    result.max = params.max ? Integer.parseInt(params.max) : 10;
    result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

    try {
      result.latestAlerts = ESWrapperService.searchJson(index_to_search,es_query,result.offset,result.max,'evtTimestamp','desc');
      result.totalAlerts = result.latestAlerts?.resultsTotal
      result.rows = buildSubscriptionInfo(result.latestAlerts.hits)
    }
    catch ( Exception e ) {
      log.error("Problem with query",e);
    }

    render(template:'/subscriptions/rss', model:[result:result],contentType: "text/xml", encoding: "UTF-8",  contextPath: '/')
  }

  def atom() {
    def result=[:]
    result.subscription = Subscription.findBySubscriptionId(params.id)

    response.characterEncoding = 'UTF-8'

    def query_clause='';
    if ( params.q ) {
      query_clause = ',{"simple_query_string": { "query":"'+params.q+'" } }'
    }
    String[] indexes_to_search = [ 'alerts' ]
    String es_query = '''{
         "bool": {
           "must": [ 
             { "match": { "AlertMetadata.MatchedSubscriptions": "'''+params.id+'''"} }
             '''+query_clause+'''
           ]
         } 
       }'''

    result.max = params.max ? Integer.parseInt(params.max) : 10;
    result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

    try {
      result.latestAlerts = ESWrapperService.searchJson(indexes_to_search,es_query,result.offset,result.max,'evtTimestamp','desc');
      result.totalAlerts = result.latestAlerts?.getHits()?.totalHits
      result.rows = buildSubscriptionInfo(result.latestAlerts?.getHits()?.getHits())
    }
    catch ( Exception e ) {
      log.error("Problem with query",e);
    }

    render(template:'/subscriptions/atom', model:[result:result],contentType: "text/xml", encoding: "UTF-8", contextPath: '/')
  }

  private List buildSubscriptionInfo(rows) {
    List result = []
    rows.each { row ->
      Map<String, Object> src = row.getSource()
      Map new_entry = [:]
      new_entry.identifier=src.AlertBody.identifier
      new_entry.url=src.AlertMetadata.SourceUrl

      def info_elements = src.AlertBody.info instanceof List ? src.AlertBody.info : [ src.AlertBody.info ]

      def first_description = null;
      def first_title = null;

      info_elements.each { info ->
        if ( ( first_description == null ) && ( info.description != null ) )
          first_description = info.description;
        if ( ( first_title == null ) && ( info.headline != null ) )
          first_title = info.headline;

        new_entry.title=first_title
        new_entry.description=first_description
      }

      result.add(new_entry);
    }
    result
  }

  @Secured(['ROLE_ADMIN', 'IS_AUTHENTICATED_FULLY'])
  def addHook() {
    log.debug("addHook ${params}");
    if ( params.newHookUrl &&
         params.newHookUrl.length() > 0 ) {
      Subscription s = Subscription.findBySubscriptionId(params.id);
      WebHook new_hook = new WebHook(ownerSubscription: s, hookUrl: params.newHookUrl)
      new_hook.save(flush:true, failOnError:true);
    }
    redirect(action:'details', id:params.id);
  }
}
