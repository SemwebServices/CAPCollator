#!groovy

podTemplate(
  containers:[
    containerTemplate(name: 'jdk11',                image:'adoptopenjdk:11-jdk-openj9',   ttyEnabled:true, command:'cat'),
    containerTemplate(name: 'docker',               image:'docker:18',                    ttyEnabled:true, command:'cat')
  ],
  volumes: [
    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
  ])
{
  node(POD_LABEL) {

    stage ('checkout') {
      checkout_details = checkout scm
      sh 'echo branch:$BRANCH_NAME'
      sh 'echo commit:$checkout_details.GIT_COMMIT'
    }

    stage ('build service assembly') {
      container('jdk11') {
        dir ('CAPAggregator') {
          sh './gradlew --no-daemon -x test -x integrationTest --console=plain clean build'
          sh 'ls -la ./build/libs/*'
        }
      }
    }
  }
}
