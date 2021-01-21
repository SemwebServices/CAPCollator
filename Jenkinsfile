#!groovy

podTemplate(
  containers:[
    containerTemplate(name: 'jdk11',                image:'adoptopenjdk:11-jdk-openj9',   ttyEnabled:true, command:'cat'),
    containerTemplate(name: 'docker',               image:'docker:18',                    ttyEnabled:true, command:'cat')
  ],
  volumes: [
    hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
    // hostPathVolume(hostPath: '/var/lib/jenkins/.gradledist', mountPath: '/root/.gradle')
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
      sh 'pwd'
      sh 'id'
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

    stage('Publish Docker Image') {
      container('docker') {
        dir('docker') {
          if ( checkout_details?.GIT_BRANCH == 'master' ) {
            println("Considering build tag : ${constructed_tag} version:${props.appVersion}");
            // Some interesting stuff here https://github.com/jenkinsci/pipeline-examples/pull/83/files
            if ( !is_snapshot ) {
              do_k8s_update=true
              docker.withRegistry('','semwebdockerhub') {
                println("Publishing released version with latest tag and semver ${semantic_version_components}");
                docker_image.push('latest')
                docker_image.push("v${app_version}".toString())
                docker_image.push("v${semantic_version_components[0]}.${semantic_version_components[1]}".toString())
                docker_image.push("v${semantic_version_components[0]}".toString())
                deploy_cfg='deploy_latest.yaml'
              }
            }
            else {
              docker.withRegistry('','semwebdockerhub') {
                println("Publishing snapshot-latest");
                docker_image.push('snapshot-latest')
                deploy_cfg='deploy_snapshot.yaml'
              }
            }
          }
          else {
            println("Not publishing docker image for branch ${checkout_details?.GIT_BRANCH}. Please merge to master for a docker image build");
          }
        }
      }
    }

    stage('Rolling Update') {
      if ( deploy_cfg != null ) {
          env.CONSTRUCTED_TAG = constructed_tag
          println("Attempt to deploy : ${deploy_cfg}, env.CONSTRUCTED_TAG=${env.CONSTRUCTED_TAG}");
          kubernetesDeploy(
            // Credentials for k8s to run the required deployment commands
            kubeconfigId: 'local_k8s',
            // Definition of the deployment
            configs: "k8s/${deploy_cfg}",
            enableConfigSubstitution: true
          )
      }
    }



    stage ('Remove old builds') {
      //keep 3 builds per branch
      properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '3', numToKeepStr: '3']]]);
    }
  }

}
