curl -X GET 'http://localhost:9200/alerts/_search' -d '{
     "from":0,
     "size":1000,
     "query":{
       "bool" : {
         "must" : [
           {
             "match" : {
               "AlertMetadata.MatchedSubscriptions" : {
                 "query" : "country-at-lang-en"
               }
             }
           }
         ]
       }
     }
}'

