curl -XPOST 'http://localhost:9200/alerts/_delete_by_query' -d '
{
  "query": {
    "match_all":{}
  }
}'
