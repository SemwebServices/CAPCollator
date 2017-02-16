

## Technology

Currently implemented in Java using the groovy-on-grails framework, but runnable as a microservice using the
production jar. An embedded database is used for feed state. Elasticsearch 5.2+ is used for geo indexing

## Deployment

There are many deployment options, the initial goal was for a local facade, but projects may also use the
service internally to poll RSS and turn feeds into event streams.

### GeneralUser

    CREATE USER capcollator WITH PASSWORD 'capcollator';

### Dev Env

    DROP DATABASE capcollatordev;
    CREATE DATABASE capcollatordev;
    GRANT ALL PRIVILEGES ON DATABASE capcollatordev to capcollator;

### Production

    DROP DATABASE capcollatorprod;
    CREATE DATABASE capcollatorprod;
    GRANT ALL PRIVILEGES ON DATABASE capcollatorprod to capcollator;



### Gaz data

See http://eric.clst.org/Stuff/USGeoJSON
http://www.nws.noaa.gov/geodata/catalog/wsom/html/pubzone.htm



# Installation

In order to work fully, the ES mappings need to be installed. run the es5_config.sh script from the scripts directory BEFORE running


## ES Queries

http://localhost:9200/alertssubscriptions/alertsubscription/_search?q=*

## RabbitMQ

http://localhost:15672/#/ -- mgt interface
http://localhost:15670/ -- WebSockents info

## Apache2

CAPCollator makes it's events available over websockets. This is done via rabbitmq. The following stanza can be
used to proxy the rabbitmq web_stomp plugin in front of apache for easy connectivity.

      AllowEncodedSlashes On
      <LocationMatch "/rabbitws/">
        ProxyPass http://localhost:15674/ nocanon
        ProxyPassReverse http://localhost:15674/
      </LocationMatch>

