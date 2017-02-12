package capcollator

import grails.transaction.Transactional
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher

@Transactional
class CapEventHandlerService {

  RabbitMessagePublisher rabbitMessagePublisher
  def ESWrapperService

  def process(cap_notification) {
    log.debug("CapEventHandlerService::process ${cap_notification}");

    // Extract any shapes from the cap (info) alert['alert']['info'].each { it.['area'] }
    if ( cap_notification?.info ) {
      def list_of_info_elements = cap_notification.info instanceof List ? cap_notification.info : [ cap_notification.info ]

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
              matching_subscriptions.addAll(matchSubscriptions(area.polygon))
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
    log.debug("indexAlert(${cap_notification},${matching_subscriptions}");
  }

  def matchSubscriptions(polygon) {

    def result=[]

    log.debug("matchSubscriptions(cap...,${polygon} ${polygon?.class?.name})");

    // Some feeds wrap the outer polygon in an array, if so, extract it.
    def polygon_ring_string = polygon instanceof List ? polygon[0] : polygon

    // Polygon as given is a ring list of space separated pairs - "x1,y1 x2,y2 x3,y3 x4,y4 x1,y1"
    def polygon_ring = []
    def list_of_pairs = polygon_ring_string.split(' ')
    list_of_pairs.each { coordinate_pair ->
      // geohash wants lon,lat the other way to our geojson, so flip them
      def split_pair = coordinate_pair.split(',')
      if ( split_pair.size() == 2 ) {
        polygon_ring.add([split_pair[1],split_pair[0]])
      }
      else {
        log.error("Problem attempting to split coordiate pair ${coordinate_pair}");
      }
    }

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
