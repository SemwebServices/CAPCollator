# Clear down
curl -XDELETE 'http://localhost:9200/alerts'
# Create an index called alerts
curl -XPUT 'http://localhost:9200/alerts'
# Create a type mapping called alert
curl -XPUT 'http://localhost:9200/alerts/alert/_mapping' -d ' 
{ 
   "alert":{ 
      "properties":{ 
         "id":{ 
            "include_in_all":"false", 
            "index":"not_analyzed", 
            "type":"text", 
            "store":"yes" 
         }, 
         "areas" : {
           "type": "nested",
           "properties":{
             "alertShape": {
                "type": "geo_shape",
                "tree": "quadtree",
                "precision": "100m"
             },
             "label":{
               "type":"text", 
               "store":"yes" 
             }
           }
         }
      }
   } 
}' 
curl -XDELETE 'http://localhost:9200/alertssubscriptions'
curl -XPUT 'http://localhost:9200/alertssubscriptions'
curl -XPUT 'http://localhost:9200/alertssubscriptions/alertsubscription/_mapping' -d ' 
{ 
   "alertsubscription":{ 
      "properties":{ 
         "id":{ 
            "include_in_all":"false", 
            "index":"not_analyzed", 
            "type":"text", 
            "store":"yes" 
         }, 
         "subshape": {
            "type": "geo_shape",
            "tree": "quadtree",
            "precision": "100m"
         }
      }
   } 
}'

