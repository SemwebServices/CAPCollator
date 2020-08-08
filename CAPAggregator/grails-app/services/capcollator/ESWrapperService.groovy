package capcollator

import java.text.SimpleDateFormat
import java.net.InetAddress;

import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.apache.http.HttpHost;
import static groovy.json.JsonOutput.*;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.index.reindex.BulkByScrollResponse


import org.elasticsearch.client.*
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.builder.*;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.AggregationBuilders;


import org.elasticsearch.client.RequestOptions;
import groovy.util.logging.Slf4j


/**
 * This service insulates KB+ from ES API changes - gather all ES calls in here and proxy them with local interface defintions
 * @see https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high.html
 */
@Slf4j
class ESWrapperService {

  static transactional = false

  def grailsApplication

  RestHighLevelClient esclient = null;

  @javax.annotation.PostConstruct
  def init() {
    log.info("init ES wrapper service eshost: ${grailsApplication.config?.eshost ?: 'default to eskbplusg3'}");
  }

  private synchronized RestHighLevelClient ensureClient() {

    if ( esclient == null ) {
      int retries = 0;
      def es_host_name = grailsApplication.config?.eshost ?: 'elasticsearch'
      while ( ( esclient == null ) && ( retries < 10 ) ) {
        try {
          log.debug("Create new RestHighLevelClient ${es_host_name}");
          esclient = new RestHighLevelClient(RestClient.builder( new HttpHost(es_host_name, 9200, "http")));
          log.debug("ES wrapper service init completed OK");
        }
        catch ( Exception e ) {
          log.warn("Unable to connect to ES - assuming that the ES node is not yet available. Retries=${retries++}");
        }
      }
    }
    else {
      log.debug("return client already held");
    }

    return esclient
  }

  public RestHighLevelClient getClient() {
    if ( esclient == null )
      ensureClient()

    return esclient;
  }

  @javax.annotation.PreDestroy
  def destroy() {
    log.debug("Destroy");
     if ( esclient ) {
       esclient.close();
     }
  }

  public doDelete(es_index, domain, record_id) {
    log.debug("doDelete(${es_index},${domain},${record_id})");
    // See https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/_motivations_around_a_new_java_client.html
    DeleteRequest request = new DeleteRequest(es_index, domain, record_id);
    DeleteResponse response = client.delete(request, RequestOptions.DEFAULT); 
  }

  public doIndex(String es_index, 
                 String domain, 
                 String record_id, 
                 Map idx_record) {

    // See also BulkRequest bulk = new BulkRequest()

    log.debug("doIndex(${es_index},${domain},${record_id},...)");

    IndexRequest request = new IndexRequest(es_index,domain,record_id)
    request.source(idx_record)
    IndexResponse response = getClient().index(request, RequestOptions.DEFAULT);

    return response
  }

  public bulkIndex(String es_index,
                   String domain,
                   String record_id,
                   List<Map> idx_records) {

    // See also BulkRequest bulk = new BulkRequest()

    log.debug("doIndex(${es_index},${domain},${record_id},...)");

    BulkRequest bulk_request = new BulkRequest()
    idx_records.each { idx_record ->
      IndexRequest idx_request = new IndexRequest(es_index,domain,record_id)
      idx_request.source(idx_record)
      bulk_request.add(idx_request)
    }

    BulkResponse bulkResponse = client.bulk(bulk_request, RequestOptions.DEFAULT);
  }

  def searchJson(String index,
                 String query,
                 Integer offset,
                 Integer max,
                 String sortBy,
                 String sortOrder) {

    // log.debug("ESSearchService::search - ${index} ${query}")

    def result = [:]
    RestHighLevelClient esclient = getClient()
    try {
       SearchRequest searchRequest = new SearchRequest(index);
       SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
       // searchSourceBuilder.query(QueryBuilders.matchAllQuery());

       if (sortBy) {
         SortOrder order = SortOrder.ASC
         if (sortOrder) {
           order = SortOrder.valueOf(sortOrder.toUpperCase())
         }
         searchSourceBuilder.sort(sortBy, order);
       }
       // log.debug("srb start to add query and aggregration query string is ${query}")

       // https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-search.html
       searchSourceBuilder.query(QueryBuilders.wrapperQuery(query))
       searchSourceBuilder.from(offset)
       searchSourceBuilder.size(max)
       searchSourceBuilder.trackTotalHits(true)

       searchRequest.source(searchSourceBuilder);

       SearchResponse searchResponse = esclient.search(searchRequest, RequestOptions.DEFAULT);

       if (searchResponse != null) {
         // log.debug("Got search response ${searchResponse.class.name}");

         def search_hits = searchResponse.getHits()
         result.hits = search_hits.getHits()
         result.resultsTotal = search_hits.totalHits?.value

         if (searchResponse.getAggregations()) {
           result.facets = [:]
           searchResponse.getAggregations().each { entry ->
             // log.debug("Aggregation entry ${entry} ${entry.getName()}");
             def facet_values = []
             entry.buckets.each { bucket ->
               bucket.each { bi ->
                 facet_values.add([term:bi.getKey(),display:bi.getKey(),count:bi.getDocCount()])
               }
             }
             result.facets[entry.getName()] = facet_values
           }
         }
       }
    }
    finally {
      // log.debug("Search result(${index},${query}) = ${result?.resultsTotal}");
    }

    result
  }


  def searchQS(String index, String query) {
    // log.debug("ESSearchService::search - ${index} ${query}")
    def result = [:]
    RestHighLevelClient esclient = getClient()
    try {
       SearchRequest searchRequest = new SearchRequest(index);
       SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
       // searchSourceBuilder.query(QueryBuilders.matchAllQuery());
       searchSourceBuilder.query(QueryBuilders.queryStringQuery(query))
       searchSourceBuilder.trackTotalHits(true)
       searchRequest.source(searchSourceBuilder);
       SearchResponse searchResponse = esclient.search(searchRequest, RequestOptions.DEFAULT);

       if (searchResponse) {
         def search_hits = searchResponse.getHits()
         result.hits = search_hits.getHits()
         result.resultsTotal = search_hits.totalHits

         if (searchResponse.getAggregations()) {
           result.facets = [:]
           searchResponse.getAggregations().each { entry ->
             // log.debug("Aggregation entry ${entry} ${entry.getName()}");
             def facet_values = []
             entry.buckets.each { bucket ->
               bucket.each { bi ->
                 facet_values.add([term:bi.getKey(),display:bi.getKey(),count:bi.getDocCount()])
               }
             }
             result.facets[entry.getName()] = facet_values
           }
         }
       }

    }
    catch ( Exception e ) {
      log.error("Problem",e);
    }
    finally {
      log.debug("Search result = ${result.resultsTotal}");
    }

    result
  }

  def deleteByJsonQuery(String index, String query) {
    // https://artifacts.elastic.co/javadoc/org/elasticsearch/client/elasticsearch-rest-high-level-client/7.4.2/org/elasticsearch/client/RestHighLevelClient.html
    // see https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-high-document-delete-by-query.html
    def result = [:]
    RestHighLevelClient esclient = getClient()
    try {
       DeleteByQueryRequest dbq = new DeleteByQueryRequest(index);
       dbq.setQuery(QueryBuilders.wrapperQuery(query));
       BulkByScrollResponse dbq_resp = esclient.deleteByQuery(dbq, RequestOptions.DEFAULT);
    }
    catch ( Exception e ) {
      log.error("Problem in deleteByJsonQuery",e);
    }

    result


  }

}
