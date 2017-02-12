package capcollator

import grails.transaction.Transactional
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher

@Transactional
class CapEventHandlerService {

  RabbitMessagePublisher rabbitMessagePublisher
  def ESWrapperService

  def process(cap_notification) {
    // log.debug("CapEventHandlerService::process ${cap_notification}");

    def cap_body = cap_notification.AlertBody

    // Extract any shapes from the cap (info) alert['alert']['info'].each { it.['area'] }
    if ( cap_body?.info ) {
      def list_of_info_elements = cap_body.info instanceof List ? cap_body.info : [ cap_body.info ]

      // Create a set - this will prevent duplicate subscriptions if multiple info elements match
      def matching_subscriptions = new java.util.HashSet()

      list_of_info_elements.each { ie ->
        log.debug("  -> Check info element");
        def polygons_found=0
        if ( ie.area ) {
          def list_of_area_elements = ie.area instanceof List ? ie.area : [ ie.area ]
          list_of_area_elements.each { area ->
            if ( area.polygon ) {
              polygons_found++
              // We got a polygon
              def inner_polygon_ring = geoJsonToPolygon(area.polygon)
              matching_subscriptions.addAll(matchSubscriptions(inner_polygon_ring))

              // We enrich the parsed JSON document with a version of the polygon that ES can index to make the whole
              // database of alerts geo searchable
              area.cc_poly = [ type:'polygon', coordinates:[ inner_polygon_ring ] ]
            }
          }
        }
      }

      log.debug("The following subscriptions matched : ${matching_subscriptions}");

      // Index the CAP event
      indexAlert(cap_notification, matching_subscriptions)
    }

  }

  def indexAlert(cap_notification, matching_subscriptions) {
    if ( cap_notification.AlertMetadata ) {
      // Store the matching subscriptions in the metadata
      cap_notification.AlertMetadata['MatchedSubscriptions']=matching_subscriptions
      // Drop the signature -- it's very verbose and applies to the underlying XML document. 
      // Consumers should return the source CAP if they want to validate the alert
      cap_notification.AlertBody.Signature=null
      ESWrapperService.index('alerts','alert',cap_notification)
    }
  }

  def geoJsonToPolygon(polygon) {

    // Some feeds wrap the outer polygon in an array, if so, extract it.
    def polygon_ring_string = polygon instanceof List ? polygon[0] : polygon

    // Polygon as given is a ring list of space separated pairs - "x1,y1 x2,y2 x3,y3 x4,y4 x1,y1"
    def polygon_ring = []

    def last_pair = null
    def list_of_pairs = polygon_ring_string.split(' ')
    list_of_pairs.each { coordinate_pair ->
      // geohash wants lon,lat the other way to our geojson, so flip them
      def split_pair = coordinate_pair.split(',')
      if ( split_pair.size() == 2 ) {
        if ( ( last_pair != null ) && ( ( last_pair[0] == split_pair[0] ) && ( last_pair[1] == split_pair[1] ) ) ) {
          log.debug("Skipping repeated pair of coordinates ${split_pair}");
        }
        else {
          polygon_ring.add([split_pair[1],split_pair[0]])
          last_pair = split_pair
        }
      }
      else {
        log.error("Problem attempting to split coordiate pair ${coordinate_pair}");
      }
    }

    return polygon_ring
  }

  def matchSubscriptions(polygon_ring) {

    def result=[]

    String query = '''{
         "bool": {
           "must": {
             "match_all": {}
           },
           "filter": {
               "geo_shape": {
                 "subshape": {
                   "shape": {
                     "type": "polygon",
                     "coordinates":['''+polygon_ring+''']
                   },
                   "relation":"intersects"
                 }
               }
             }
           }
         }'''

    String[] indexes_to_search = [ 'alertssubscriptions' ]
    def matching_subs = ESWrapperService.search(indexes_to_search,query);

    if ( matching_subs ) {
      matching_subs.getHits().getHits().each { matching_sub ->
        result.add(matching_sub.sourceAsMap().shortcode)
      }
    }

    result
  }
}
