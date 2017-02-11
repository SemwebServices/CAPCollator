package capcollator

import grails.transaction.Transactional
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher

@Transactional
class CapEventHandlerService {

  RabbitMessagePublisher rabbitMessagePublisher
  def ESWrapperService

  def process(cap) {
    log.debug("CapEventHandlerService::process ${cap}");

    // Extract any shapes from the cap (info) alert['alert']['info'].each { it.['area'] }
    if ( cap?.info ) {
      def list_of_info_elements = cap.info instanceof List ? cap.info : [ cap.info ]

      list_of_info_elements.each { ie ->
        if ( ie.area ) {
          def list_of_area_elements = ie.area instanceof List ? ie.area : [ ie.area ]
          list_of_area_elements.each { area ->
            if ( area.polygon ) {
              // We got a polygon
              def matching_subscriptions = matchSubscriptions(cap,area.polygon)
            }
          }
        }
      }
    }

    // Index the CAP event
  }

  def matchSubscriptions(cap,polygon) {
    log.debug("matchSubscriptions(cap...,${polygon})");

    // Polygon as given is a ring list of space separated pairs - "x1,y1 x2,y2 x3,y3 x4,y4 x1,y1"
    def polygon_ring = []
    polygon.split(' ').each { coordinate_pair ->
      // geohash wants lon,lat the other way to our geojson, so flip them
      def split_pair = coordinate_pair.split(',')
      polygon_ring.add([split_pair[1],split_pair[0]])
    }

    log.debug("find subs for polygon ring ${polygon_ring}");

    String query = '''{
       "from":0,
       "size":1000,
       "query":{
         "bool": {
           "must": {
             "match_all": {}
           },
           "filter": {
               "geo_shape": {
                 "subshape": {
                   "shape": '''+polygon_ring+''',
                   "relation":'intersects'
                 }
               }
             }
           }
         },
         "sort":{
           "recid":{order:'asc'}
         }
}'''

    log.debug("Query will be \n${query}");

    String[] indexes_to_search = [ 'alertssubscriptions' ]
    ESWrapperService.search(indexes_to_search,query);
  }
}
