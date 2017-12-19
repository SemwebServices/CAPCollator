curl -X GET 'http://wah.semweb.co/es/alertssubscriptions/_search' -d '
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
                    "subshape": {
                        "shape": {
                            "type": "circle",
                            "coordinates": [
                                "-70.06",
                                "12.58"
                            ],
                            "radius": "13km"
                        },
                        "relation": "intersects"
                    }
                }
            }
        }
    },
    "sort": {
        "recid": {
            "order": "asc"
        }
    }
}
'
