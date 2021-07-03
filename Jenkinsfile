@Library ('swjenkins') _

// Build and CD this module - publish the docker image, provision this snapshot version in the cluster and then update the named
// tenants to use this snapshot.
// This works: curl https://uat.semweb.co/dabdata/actuator/health
Map args=[
  dockerImageName:'docker.semweb.co/semweb/caphub_aggregator',
  // deploymentTemplate:'k8s/deployment_template.yaml',
  // targetNamespace:'alert-hub-uat',
  // deployAs:'feedfacade',
  // healthActuator:'http://aggregator-service.alert-hub-uat:8080/CAPAggregator/actuator/health'
]

buildSWService(args)

