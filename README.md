

## Technology

Currently implemented in Java using the groovy-on-grails framework, but runnable as a microservice using the
production jar. An embedded database is used for feed state.

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
