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
      props = readProperties file: './CAPAggregator/gradle.properties'
      app_version = props.appVersion
      deploy_cfg = null;
      semantic_version_components = app_version.toString().split('\\.')
      is_snapshot = app_version.contains('SNAPSHOT')
      constructed_tag = "build-${props?.appVersion}-${checkout_details?.GIT_COMMIT?.take(12)}"
      do_k8s_update = false
      println("Got props: asString:${props} appVersion:${props.appVersion}/${props['appVersion']}/${semantic_version_components}");
      sh 'echo branch:$BRANCH_NAME'
      sh 'echo commit:$checkout_details.GIT_COMMIT'
    }

    stage ('build service assembly') {
      container('jdk11') {
        dir ('CAPAggregator') {
          sh './gradlew --no-daemon -x test -x integrationTest --console=plain clean build'
          sh 'ls -la ./build/libs/*'
          sh "cp build/libs/CAPAggregator-${props.appVersion}.war ../docker/CAPAggregator.war".toString()
        }
      }
    }

    // https://www.jenkins.io/doc/book/pipeline/docker/
    stage('Build Docker Image') {
      container('docker') {
        dir('docker') {
          println("Docker build")
          docker_image = docker.build("semweb/caphub_aggregator")
        }
      }
    }


    stage ('Remove old builds') {
      //keep 3 builds per branch
      properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '3', numToKeepStr: '3']]]);
    }
  }

}
