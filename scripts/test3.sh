curl -X GET 'http://wah.semweb.co/es/alerts/_search' -d '
{
  "from": 0,
  "size": 1000,
  "query": {
         "bool": {
           "must": {
             "match_all": {}
           },
           "filter": {
             "geo_shape": {
               "*cc_polys": {
                 "shape": {
                   "type": "circle",
                   "coordinates":[53.386036,-1.4702315],
                   "radius": "2.0km"
                 },
                 "relation":"intersects"
               }
             }
           }
         }
       }
  }
'
