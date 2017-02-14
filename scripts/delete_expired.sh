curl -XPOST 'http://localhost:9200/alerts/_delete_by_query' -d '
{
  "query": {
    "range" : {
        "AlertMetadata.expires" : {
           "lte" : "2017-02-14T09:59:59+01:00"
        }
    }
  }
}'
