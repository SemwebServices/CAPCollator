#!/bin/bash
export SDKMAN_DIR="/home/ibbo/.sdkman"
[[ -s "/home/ibbo/.sdkman/bin/sdkman-init.sh" ]] && source "/home/ibbo/.sdkman/bin/sdkman-init.sh"

sdk use grails 4.0.1
sdk use java 11.0.5.j9-adpt
cd CAPAggregator
grails prod war
cp build/libs/CAPAggregator-2.0.0.war ../docker/CAPAggregator.war
cd ../docker
docker login
docker build -t semweb/caphub_aggregator:v2.0 -t semweb/caphub_aggregator:v2 -t semweb/caphub_aggregator:latest .
docker push semweb/caphub_aggregator:v2.0
docker push semweb/caphub_aggregator:v2
docker push semweb/caphub_aggregator:latest


