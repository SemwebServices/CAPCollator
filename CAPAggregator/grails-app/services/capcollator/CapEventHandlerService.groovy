package capcollator

import grails.gorm.transactions.*
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import java.util.Iterator
import static groovy.json.JsonOutput.*
import grails.async.Promise
import static grails.async.Promises.*
import java.text.SimpleDateFormat;

import org.w3c.dom.Document;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

@Transactional
class CapEventHandlerService {

  RabbitMessagePublisher rabbitMessagePublisher
  def ESWrapperService
  def eventService
  def gazService
  def feedFeedbackService
  def capUrlHandlerService

  // import org.apache.commons.collections4.map.PassiveExpiringMap;
  // Time to live in millis - 1000 * 60 == 1m 
  // private Map geo_query_cache = Collections.synchronizedMap(new PassiveExpiringMap(1000*60))
  

  static int queue_size = 0;
  private static final double EARTH_RADIUS_METERS = 6371000.0;

  /**
   * Fired when we have detected a CAP event, to capture the event and index it in our local ES index
   */
  def process(cap_notification) {

    // This is a bit fugly - but whilst there are more than 100 events, wait for the processing to slim down the queue
    while ( queue_size > 25 ) {
      synchronized(this) {
        log.debug("blocking on cap event processing pool ${queue_size}");
        this.wait(10000);
      }
    }

    // break out this call here as this needs to be convered into an executor pool, this handler is
    // becomming a bottleneck in processing as alerts with high numbers of areas and high numbers of
    // vertices can slow down processing
    queue_size++;
    log.debug("CapEventHandlerService::process - enqueue - size is ${queue_size}");

    // Ideally we would like this call to block until a thread is available to take the request
    Promise p = task {
      internalProcess(cap_notification)
    }
    p.onError { Throwable err ->
      log.error("Promise error",err);
    }
    p.onComplete { result ->
      log.debug("Promise completed OK");
    }

  }


  def internalProcess(cap_notification) {

    queue_size--;

    // Wake up anyone who might be blocked
    if ( queue_size < 100 ) {
      synchronized(this) {
        this.notifyAll();
      }
    }

    long start_time = System.currentTimeMillis();
    log.debug("CapEventHandlerService::process (queue_size :: ${queue_size}, alert from ${cap_notification.AlertMetadata.sourceFeed})"); 

    cap_notification.AlertMetadata.compound_identifier = new String(
                                     cap_notification.AlertMetadata.sourceFeed+'|'+
                                     cap_notification.AlertBody.identifier+'|'+
                                     cap_notification.AlertBody.sent);

    try {
      def cap_body = cap_notification.AlertBody
      def polygons_found=0

      if ( cap_notification.AlertMetadata.tags == null ) {
        cap_notification.AlertMetadata.tags=[]
      } 

      if ( cap_notification.AlertMetadata['warnings'] == null ) {
        cap_notification.AlertMetadata['warnings'] = []
      }

      // if ( cap_body.sent != null ) {
      // changing this - use the system timestamp for ordering rather than the date on the alert - which can be odd due to
      // timezone offsets (Incorrectly formatted iso dates don't order properly if they have a timezone offset).
      SimpleDateFormat ts_sdf = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSS'Z'".toString());
      ts_sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
      cap_notification.evtTimestamp = ts_sdf.format(new Date())

      Map geo_query_cache = [:]

      // Extract any shapes from the cap (info) alert['alert']['info'].each { it.['area'] }
      if ( cap_body?.info ) {
        List list_of_info_elements = cap_body.info instanceof List ? cap_body.info : [ cap_body.info ]

        // Create a set - this will prevent duplicate subscriptions if multiple info elements match
        def matching_subscriptions = new java.util.HashSet()

        long size_ie_array = list_of_info_elements.size()
        long ie_ctr = 0;
        list_of_info_elements.each { ie ->

          long query_phase_start_time = System.currentTimeMillis();

          // log.debug("  -> Check info element ${ie_ctr}/${size_ie_array}");
          if ( ie.area ) {
            def list_of_area_elements = ie.area instanceof List ? ie.area : [ ie.area ]

            list_of_area_elements.each { area ->

              // log.debug("Processing area...");

              if ( area.cc_polys == null ) {
                area.cc_polys = [];
              }

              if ( area.polygon != null ) {
  
                if ( !cap_notification.AlertMetadata.tags.contains('AREATYPE_POLYGON') ) 
                  cap_notification.AlertMetadata.tags.add('AREATYPE_POLYGON');

                def list_of_polygon_elements = area.polygon instanceof List ? area.polygon : [ area.polygon ]
  
                // log.debug("....As polygon.. count=${list_of_polygon_elements.size()}");

                list_of_polygon_elements.each { poly_elem ->

                  polygons_found++
                  // We got a polygon
                  def inner_polygon_ring = geoJsonToPolygon(poly_elem)
                  def match_result = matchSubscriptionPolygon(geo_query_cache,inner_polygon_ring)

                  matching_subscriptions.addAll(filterNonGeoProperties(match_result.subscriptions, cap_notification, ie));

                  cap_notification.AlertMetadata['warnings'].addAll(match_result.messages);

                  if ( match_result.status == 'ERROR' ) {
                    log.debug("Adding GEO_SEARCH_ERROR tag to alert");
                    cap_notification.AlertMetadata.tags.add('GEO_SEARCH_ERROR/POLYGON');
                    
                    // IF the search failed - that probably means that there is a problem with the source shape. 
                    if ( cap_notification.AlertMetadata.errorShapes == null )
                      cap_notification.AlertMetadata.errorShapes = [ inner_polygon_ring.toString() ]
                    else
                      cap_notification.AlertMetadata.errorShapes.add(inner_polygon_ring.toString())
                  }
                  else {
                    // We enrich the parsed JSON document with a version of the polygon that ES can index to make the whole
                    // database of alerts geo searchable
                    area.cc_polys.add( [ type:'polygon', coordinates: [ inner_polygon_ring ] ] );
                  }
                }
  
                // If we got a polygon AND there was an info.area.geocode then we can look to see if we should cache that code
                // this is duff -- an alert can have many polygons and many geocodes, so the assumption here is wrong. 
                if ( 1==2 ) {
                  if ( area.geocode && area.geocode.value && area.geocode.valueName ) {
                    log.debug("CAP Alert contains polygon and geocode - cache value - ${area.geocode}");
                    def authorities = area.geocode.valueName instanceof List ? area.geocode.valueName : [ area.geocode.valueName ]
                    def symbols = area.geocode.value instanceof List ? area.geocode.value : [ area.geocode.value ]
    
                    Iterator i1=authorities.iterator()
                    Iterator i2=symbols.iterator()
                    for (; i1.hasNext() && i2.hasNext(); ) {
                      
                      try {
                        gazService.cache(i1.next(), i2.next(), inner_polygon_ring);
                      }
                      catch ( Exception e ) {
                        log.error("problem trying to cache gaz entry",e);
                      }
                    }
                  }
                }
  
                area.geocode?.each { gc ->
                  log.debug("CAP Alert has geocode : ${gc} ");
                }
              }

              if ( area.circle != null ) {

                // log.debug("....As circle");

                if ( !cap_notification.AlertMetadata.tags.contains('AREATYPE_POLYGON') ) 
                  cap_notification.AlertMetadata.tags.add('AREATYPE_CIRCLE');

                def list_of_circle_elements = area.circle instanceof List ? area.circle : [ area.circle ]

                list_of_circle_elements.each { circle ->
                  polygons_found++
  
                  // log.debug("Area defines a circle"); // EG 2.58,-70.06 13
                  def coords_radius = circle.split(' ');
                  def coords = null;
                  def radius = null;
  
                  if ( coords_radius.length > 0 ) {
                    coords = coords_radius[0].split(',');
                  }
  
                  if ( coords_radius.length > 1 ) {
                    // CAP Standard determies that radius must be given in KM, so no conversion needed.
                      // LEFT HERE FOR INFO:: THIS IS NOT THE CASE NOW Got radius part - We assume the feed is giving a radius in miles, so we convert to ES KM here // radius = Integer.parseInt(coords_radius[1]) * 1.6;
                    radius = Float.parseFloat(coords_radius[1])
                  }
                  else {
                    // log.debug("Using default radius of 10 KM");
                    radius = 0
                  }
  
                  if ( coords != null ) {
                    if ( radius == 0 ) {
                      def lat = Float.parseFloat(coords[0]);  // -90 to +90
                      def lon = Float.parseFloat(coords[1]);  // -180 to +180
                      def match_result = matchSubscriptionAsPoint(lat,lon,cap_notification)
                      matching_subscriptions.addAll(filterNonGeoProperties(match_result.subscriptions, cap_notification, ie));
                      cap_notification.AlertMetadata['warnings'].addAll(match_result.messages);
                      if ( match_result.status == 'ERROR' ) {
                        cap_notification.AlertMetadata.tags.add('GEO_SEARCH_ERROR/CIRCLE_AS_POINT');
                      }
                      else {
                        area.cc_polys.add( [ type:'point', coordinates: [ lon, lat ] ] );
                      }
                    }
                    else {
                      // log.debug("Parse coordinates - we assume <circle> elements are composed within the circle element as lat <comma> lon <space> radius : ${circle}");
                      def lat = Float.parseFloat(coords[0]);  // -90 to +90
                      def lon = Float.parseFloat(coords[1]);  // -180 to +180
                      def match_result = matchSubscriptionCircleAsPolygon(lat,lon,radius,cap_notification)
                      matching_subscriptions.addAll(filterNonGeoProperties(match_result.subscriptions, cap_notification, ie));
                      // matching_subscriptions.addAll(match_result.subscriptions);
    
                      cap_notification.AlertMetadata['warnings'].addAll(match_result.messages);
    
                      if ( match_result.status == 'ERROR' ) {
                        cap_notification.AlertMetadata.tags.add('GEO_SEARCH_ERROR/CIRCLE_AS_POLY');
                        if ( cap_notification.AlertMetadata.errorShapes == null )
                          cap_notification.AlertMetadata.errorShapes = [ match_result.approximated_poly.toString() ]
                        else
                          cap_notification.AlertMetadata.errorShapes.add(match_result.approximated_poly.toString() )
                      }
                      else {
                        // We enrich the parsed JSON document with a version of the polygon that ES can index to make the whole
                        // database of alerts geo searchable
   
                        // Convert the circle to a polygon
                        // area.cc_polys.add(  [ type:'circle', coordinates:[ coords[1], coords[0] ], radius:"${radius}km".toString() ] )
                        //double[][] approximated_poly = circle2polygon(10, lat, lon, radius)
                        area.cc_polys.add( [ type:'polygon', coordinates: [ match_result.approximated_poly ] ] );
                      }
                    }
                  }
                  else {
                    log.error("Failed to parse circle area ${circle}");
                  }
                }
              }
            }
          }
          log.info("info element checking complete${ie_ctr++}/${size_ie_array}. QueryPhase elapsed: ${System.currentTimeMillis() - query_phase_start_time}");
        }

        log.debug("The following subscriptions matched : ${matching_subscriptions} (# polygons found:${polygons_found})");

        if ( polygons_found == 0 ) {
          eventService.registerEvent('CAPXMLWithNoPolygon',System.currentTimeMillis());
          cap_notification.AlertMetadata.tags.add('No_Polygon_Provided');
        }
        else {
          cap_notification.AlertMetadata.tags.add('Mappable');
        }

        if ( ! matching_subscriptions.contains('unfiltered') ) {
          // It's likely that the alert was well formed, but did not contain a geo element, and therefore
          // did not match the unfiltered subscription. Add it anyway!
          matching_subscriptions.add('unfiltered');
        }

        cap_notification.AlertMetadata.CCHistory.add(['event':'CC-spatial-processing-complete','timestamp':System.currentTimeMillis()]);
        publishAlert(cap_notification, matching_subscriptions);

        // Index the CAP event
        indexAlert(cap_notification, matching_subscriptions)

        // If there was a geo search error, send feedback to the feed fetcher that there is an issue with the alerts
        // coming from this feed.
        if ( cap_notification.AlertMetadata.tags.find { it.startsWith('GEO_SEARCH_ERROR') } != null ) {
          feedFeedbackService.publishFeedEvent(cap_notification.AlertMetadata.sourceFeed,
                                               null,
                                               [ message: "GEO_SEARCH_ERROR",
                                                 matchedSubscriptions:matching_subscriptions,
                                                 tags:cap_notification.AlertMetadata.tags,
                                                 history:cap_notification.AlertMetadata.CCHistory] );
        }
      }
      else {
        feedFeedbackService.publishFeedEvent(cap_notification.AlertMetadata.sourceFeed,
                                             null,
                                             [ message: "Unable to find any INFO element in alert XML"] );
      }

    }
    catch ( Exception e ) {
      log.debug("CapEventHandlerService::internalProcess Exception processing CAP notification:\n${cap_notification}\n",e);
      feedFeedbackService.publishFeedEvent(cap_notification.AlertMetadata.sourceFeed,null,[ message: "Error: ${e.message}".toString() ])
    }
    finally {
      log.info("CapEventHandlerService::internalProcess complete elapsed=${System.currentTimeMillis() - start_time}");
    }

  }

  def publishAlert(cap_notification, matching_subscriptions) {
    log.debug("publishAlert - Publishing CAPSubMatch. ${matching_subscriptions}");

    if ( cap_notification.AlertMetadata.PrivateSourceUrl != null ) 
      cap_notification.AlertMetadata.remove('PrivateSourceUrl');

    matching_subscriptions.each { sub_id ->

      // Apply other subscription filters
      try {
        if ( criteriaMet(sub_id, cap_notification) ) {
          log.debug("Publishing CAPSubMatch.${sub_id} notification");
          def result = rabbitMessagePublisher.send {
                exchange = "CAPExchange"
                routingKey = 'CAPSubMatch.'+sub_id
                body = cap_notification
          }
        }
      }
      catch ( Exception e ) {
        log.error("Problem trying to publish to rabbit",e);
      }
    }
  }

  private boolean criteriaMet(String sub_id, Map cap_notification) {
    return true;
  }

  def indexAlert(cap_notification, matching_subscriptions) {
    if ( cap_notification.AlertMetadata ) {
      // Store the matching subscriptions in the metadata
      cap_notification.AlertMetadata['MatchedSubscriptions']=matching_subscriptions
      // Drop the signature -- it's very verbose and applies to the underlying XML document. 
      // Consumers should return the source CAP if they want to validate the alert
      cap_notification.AlertBody.Signature=null
      if ( cap_notification.AlertMetadata.compound_identifier != null ) {
        ESWrapperService.doIndex('alerts',
                                 '_doc', 
                                "${cap_notification.AlertMetadata.compound_identifier}".toString(), 
                                cap_notification)
      }
      else {
        log.warn("Alert without synthetic identifier..... ${cap_notification.AlertBody.identifier}");
        ESWrapperService.doIndex('alerts', '_doc', cap_notification)
      }
    }
  }

  private List geoJsonToPolygon(String polygon_ring_string) {


    // Polygon as given is a ring list of space separated pairs - "x1,y1 x2,y2 x3,y3 x4,y4 x1,y1"
    List polygon_ring = []

    def last_pair = null
    // def cleaned_polygon_ring_string = polygon_ring_string.replaceAll('\\s+',' ');
    def cleaned_polygon_ring_string = polygon_ring_string.replaceAll('\n',' ');
    def list_of_pairs = cleaned_polygon_ring_string.split(' ')
    list_of_pairs.each { coordinate_pair ->
      // geohash wants lon,lat the other way to our geojson, so flip them
      def split_pair = coordinate_pair.split(',')
      if ( split_pair.size() == 2 ) {
        if ( ( last_pair != null ) && ( ( last_pair[0] == split_pair[0] ) && ( last_pair[1] == split_pair[1] ) ) ) {
          // log.debug("Skipping repeated pair of coordinates ${split_pair}");
        }
        else {
          polygon_ring.add([Float.parseFloat(split_pair[1]),Float.parseFloat(split_pair[0])])
          last_pair = split_pair
        }
      }
      else {
        log.error("Problem attempting to split coordiate pair ${coordinate_pair}");
      }
    }

    return polygon_ring
  }


  def matchSubscriptionAsPoint(lat,lon,cap_notification) {

    def result = [
      subscriptions:null,
      messages:[],
      status:'OK'
    ]

    String query = '''{ "bool": { "must": { "match_all": {} }, "filter": { "geo_shape": { "subshape": { "shape": { "type": "point", "coordinates":['''+lon+''','''+lat+'''] }, "relation":"intersects" } } } } }'''

    try {
      def matching_subs = ESWrapperService.searchJson('alertssubscriptions',query,0,200,null,null);

      if ( matching_subs ) {
        result.subscriptions = matching_subs.hits;
      }
    }
    catch ( Exception e ) {
      result.messages.add((e.message+'\n'+query).toString());
      result.status='ERROR';
      log.error("[point] SEARCH ERROR(${e.message}):: Validate with\ncurl -X GET 'http://eshost:9200/alertssubscriptions/_search' -H 'Content-Type: application/json' -d '${query}'")
    }

    result
  }

  /**
   * Find all subscriptions which overlap with the supplied polygon ring
   * having obtained the list, check non-spatial filter properties
   */
  def matchSubscriptionPolygon(geo_query_cache,polygon_ring) {

    log.debug("matchSubscriptionPolygon(${polygon_ring})");

    String poly_str = polygon_ring.toString();

    def result=geo_query_cache.get(poly_str)

    if ( result ) {
      return result;
    }
    else {
      result = [
        subscriptions:null,
        messages:[],
        status:'OK'
      ]
    }

    String query = '''{ "bool": { "must": { "match_all": {} }, "filter": { "geo_shape": { "subshape": { "shape": { "type": "polygon", "coordinates":['''+polygon_ring+'''] }, "relation":"intersects" } } } } }'''


    try {
      def matching_subs = ESWrapperService.searchJson('alertssubscriptions',query,0,200,null,null);

      if ( matching_subs ) {
        result.subscriptions = matching_subs.hits;
      }
    }
    catch ( Exception e ) {
      result.messages.add((e.message+'\n'+query).toString());
      result.status='ERROR';
      log.error("[polygon] SEARCH ERROR(${e.message}):: Validate with\ncurl -X GET 'http://eshost:9200/alertssubscriptions/_search' -H 'Content-Type: application/json' -d '${query}'")
    }
    finally {
      log.debug("search completed ok");
    }

    geo_query_cache.put(poly_str,result)

    result
  }

  private List filterNonGeoProperties(org.elasticsearch.search.SearchHit[] matching_subscriptions, 
                                      Map cap_notification, 
                                      Map info_element) {
    List result = []

    log.debug("filterNonGeoProperties(...,${cap_notification} ${info_element})");

    // In CapUrl handler we might have decorated the the alert URL with some credentials and
    // put that alert in PrivateSourceUrl. Reuse it here. This URL will be removed before the
    // json is published so we don't leak credential
    Document d = fetchDOM(cap_notification.AlertMetadata.PrivateSourceUrl);

    matching_subscriptions?.each { matching_sub ->
      Map sub_as_map = matching_sub.getSourceAsMap();
      if ( passNonSpatialFilter(sub_as_map, cap_notification, info_element, d) ) {
        result.add(sub_as_map.shortcode?.toString())
      }
    }

    // log.debug("filterNonGeoProperties called with list of ${matching_subscriptions.size()} returns list of ${result.size()}");
    return result;
  }

  def matchSubscriptionCircle(float lat, float lon, float radius, Map cap_notification) {

    // log.debug("matchSubscriptionCircle(${lat},${lon},${radius})");

    def result=[
      subscriptions:null,
      messages:[],
      status:'OK'
    ]

    String query = '''{ "bool": { "must": { "match_all": {} }, "filter": { "geo_shape": { "subshape": { "shape": { "type": "circle", "coordinates":['''+lon+''','''+lat+'''], "radius": "'''+radius+'''km" }, "relation":"intersects" } } } } }'''


    String indexes_to_search = 'alertssubscriptions'
    try {
      def matching_subs = ESWrapperService.searchJson('alertssubscriptions',query,0,200,null,null);

      if ( matching_subs ) {
        result.subscriptions = matching_subs.hits
      }
    }
    catch ( Exception e ) {
      result.messages.add((e.message+'\n'+query).toString());
      log.error("Problem trying to match circle::${e.message}",e);
      result.status='ERROR';
      log.error("matchSubscriptionCircle ERROR: Validate with\ncurl -X GET 'http://eshost:9200/alertssubscriptions/_search' -H 'Content-Type: application/json' -d '${query}'")
    }

    result
  }

  /**
   * Find all subscriptions which overlap with the supplied polygon ring
   * having obtained the list, check non-spatial filter properties
   */
  def matchSubscriptionCircleAsPolygon(float lat, float lon, float radius, Map cap_notification) {

    log.debug("matchSubscriptionCircleAsPolygon(${lat},${lon},${radius},...)");

    double[][] poly = circle2polygon(10, lat, lon, radius) ;

    log.debug("poly: ${poly}");

    def result = [
      approximated_poly: poly,
      subscriptions:null,
      messages:[],
      status:'OK'
    ]

    String query = '''{ "bool": { "must": { "match_all": {} }, "filter": { "geo_shape": { "subshape": { "shape": { "type": "polygon", "coordinates":['''+poly.toString()+'''] }, "relation":"intersects" } } } } }'''


    try {
      def matching_subs = ESWrapperService.searchJson('alertssubscriptions',query,0,200,null,null);

      if ( matching_subs ) {
        result.subscriptions = matching_subs.hits;
      }
    }
    catch ( Exception e ) {
      log.error("Problem in matchSubscriptionCircleAsPolygon(${lat},${lon},${radius},${cap_notification})");
      result.messages.add((e.message+'\n'+query).toString());
      result.status='ERROR';
      log.error("[circleAsPoly] SEARCH ERROR(${e.message}):: Validate with\ncurl -X GET 'http://eshost:9200/alertssubscriptions/_search' -H 'Content-Type: application/json' -d '${query}'")
    }

    result
  }


  private boolean passNonSpatialFilter(Map subscription, Map cap_notification, Map info_element, Document d) {

    boolean result = true;

    if ( ( subscription.languageOnly ) && ( !subscription.languageOnly.equalsIgnoreCase('none') ) ) {
      def lang_from_alert =  info_element.language?.toLowerCase() ?: 'en'
      if ( lang_from_alert?.startsWith(subscription.languageOnly.toLowerCase()) ) {
        log.debug("Pass language-only");
      }
      else {  
        log.debug("Did not pass language filter (req:${subscription.languageOnly}/rejected:${info_element.language}) ");
        result = false;
      }
    }

    // (//cap:urgency='Immediate' or //cap:urgency='Expected') and (//cap:severity='Extreme' or //cap:severity='Severe') and (//cap:certainty='Observed' or //cap:certainty='Likely')
    if ( ( subscription.highPriorityOnly ) && ( subscription.highPriorityOnly.equalsIgnoreCase('true') ) ) {
      // If ( urgency==immediate || urgency==expected ) && ( severity==extreme || severity==severe ) && ( certainty==observed || certainty==likely )
      // if ( info_element.urgency ) (info_element.severity)   info_element.certainty
      if ( ( info_element.urgency?.equalsIgnoreCase('immediate') || info_element.urgency?.equalsIgnoreCase('expected') ) &&
           ( info_element.severity?.equalsIgnoreCase('extreme') || info_element.severity?.equalsIgnoreCase('severe') ) &&
           ( info_element.certainty?.equalsIgnoreCase('observed') || info_element.severity?.equalsIgnoreCase('likely') ) ) {
        log.debug("Pass - Filter high priority only");
      }
      else {
        log.debug("Did not pass high priority filter - urgency:${info_element.urgency} severity:${info_element.severity} certainty:${info_element.certainty}");
        result = false;
      }
    }

    if ( ( subscription.officialOnly ) && ( subscription.officialOnly.equalsIgnoreCase('true') ) ) {
      if ( cap_notification.AlertMetadata.sourceIsOfficial.equalsIgnoreCase('true') ) {
        log.debug("Pass Filter official priority only");
      }
      else {
        log.debug("Did not pass official filter (${cap_notification.AlertMetadata.sourceIsOfficial} needs to == true)");
        result = false;
      }
    }

    // subscription.xPathFilter contains an expath expression
    // http://www.saxonica.com/documentation/#!xpath-api/jaxp-xpath/factory
    if ( ( subscription.xPathFilter != null ) && 
         ( subscription.xPathFilter.length() > 0 ) && 
         ( subscription.xPathFilter != 'none' ) ) {

      javax.xml.namespace.NamespaceContext ns_ctx = new javax.xml.namespace.NamespaceContext() {
         @Override
         public String getNamespaceURI(String prefix) {
           if ( prefix=='cap' )
             return 'urn:oasis:names:tc:emergency:cap:1.2'
           else
             return null;
         }
 
         @Override
         public String getPrefix(String namespaceURI) {
             return null;
         }
 
         @SuppressWarnings("rawtypes")
         @Override
         public Iterator getPrefixes(String namespaceURI) {
             return null;
         }
      }

      log.debug("Process xpath filter ${subscription.xPathFilter}");
      XPathFactory xpathFactory = new net.sf.saxon.xpath.XPathFactoryImpl();
      XPath xPath = xpathFactory.newXPath();
      xPath.setNamespaceContext(ns_ctx)
      result = xPath.compile(subscription.xPathFilter).evaluate(d, XPathConstants.BOOLEAN);
      log.debug("XPath result: ${result}");
    }
    else {
      log.debug("No XPATH present (${subscription.xPathFilter})");
    }

    log.debug("passNonSpatialFilter ${cap_notification.AlertMetadata.compound_identifier} against sub ${subscription?.shortcode} filter ${result?'':'Did not pass'} - returns ${result}");

    return result;
  }

  def matchAlertCircle(float lat, float lon, float radius) {

    def result = [messages:[]];
    log.debug("matchAlertCircle(${lat},${lon},${radius})");

    // ES shape points accept Geo-point expressed as an array with the format: [ lon, lat] or a string "lat,lon"
    String query = '''{
         "bool": {
           "must": {
             "match_all": {}
           },
           "filter": {
             "geo_shape": {
               "AlertBody.info.area.cc_polys": {
                 "shape": {
                   "type": "circle",
                   "coordinates":['''+lon+''','''+lat+'''],
                   "radius": "'''+radius+'''km"
                 },
                 "relation":"intersects"
               }
             }
           }
         }
     }'''

    log.debug("Validate with\ncurl -X GET 'http://eshost:9200/alerts/_search' -H 'Content-Type: application/json' -d '${query}'")

    String[] indexes_to_search = [ 'alerts' ]
    try {
      result.alerts = ESWrapperService.searchJson(indexes_to_search,query,0,200,null,null);
      result.status='OK';
    }
    catch ( Exception e ) {
      log.error("Problem trying to match circle::${e.message}",e);
      result.messages.add(e.message);
      result.status='ERROR';
    }

    result
  }


  public static double[][] circle2polygon(int segments, double latitude, double longitude, double radius) {

    double PI = 3.141592;

    if (segments < 5) {
        throw new IllegalArgumentException("you need a minimum of 5 segments");
    }
    double[][] points = new double[segments+1][0];

    double relativeLatitude = radius / EARTH_RADIUS_METERS * 180 / PI;

    // things get funny near the north and south pole, so doing a modulo 90
    // to ensure that the relative amount of degrees doesn't get too crazy.
    double relativeLongitude = relativeLatitude / Math.cos(Math.toRadians(latitude)) % 90;

    for (int i = 0; i < segments; i++) {
        // radians go from 0 to 2*PI; we want to divide the circle in nice
        // segments
        double theta = 2 * PI * i / segments;
        // trying to avoid theta being exact factors of pi because that results in some funny behavior around the
        // north-pole
        theta = theta += 0.1;
        if (theta >= 2 * PI) {
            theta = theta - 2 * PI;
        }

        // on the unit circle, any point of the circle has the coordinate
        // cos(t),sin(t) where t is the radian. So, all we need to do that
        // is multiply that with the relative latitude and longitude
        // note, latitude takes the role of y, not x. By convention we
        // always note latitude, longitude instead of the other way around
        double latOnCircle = latitude + relativeLatitude * Math.sin(theta);
        double lonOnCircle = longitude + relativeLongitude * Math.cos(theta);
        if (lonOnCircle > 180) {
            lonOnCircle = -180 + (lonOnCircle - 180);
        } else if (lonOnCircle < -180) {
            lonOnCircle = 180 - (lonOnCircle + 180);
        }

        if (latOnCircle > 90) {
            latOnCircle = 90 - (latOnCircle - 90);
        } else if (latOnCircle < -90) {
            latOnCircle = -90 - (latOnCircle + 90);
        }

        points[i] = new double[2]// [ latOnCircle, lonOnCircle ];

        // For ES, we need lon,lat in the shape so we switch the original order here
        points[i][1] = latOnCircle
        points[i][0] = lonOnCircle
    }
    // should end with same point as the origin
    points[points.length-1] = new double[2]// [points[0][0],points[0][1]];
    points[points.length-1][0] = points[0][0]
    points[points.length-1][1] = points[0][1]
    return points;
  }

  // https://www.baeldung.com/java-xpath
  Document fetchDOM(String alert_url) {
    log.debug("Fetch alert from ${alert_url}");
    InputStream is = new URL(alert_url).openStream();
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    return builder.parse(is);
  }
}
