#!groovy
def deploy_branch = "origin/20200518"
def deploy_project = "userxx-development"
def app_name = 'pipeline-practice-java'
def app_image = "image-registry.openshift-image-registry.svc:5000/${deploy_project}/${app_name}"

pipeline {
  // pipelineを実行するagentの設定。yamlファイルで設定を渡せる
  // 可能な限りJenkinsfileにagentの設定をもたせたほうが自動化とGit管理が進むためおすすめ。
  agent {
    kubernetes {
      cloud 'openshift'
      yamlFile 'openshift/jenkins-slave-pod.yaml'
    }
  }

  stages {
    stage('Setup') {
      steps {
        sh 'java -version'
        sh 'mvn -v'
        sh 'mvn clean package -DskipTests'
      }
    }

    stage('application test') {
      parallel {
        stage('Code analysis') {
          steps {
            echo "Exec static analysis"
            sh 'mvn spotbugs:check'
          }
        }

        stage('unit test') {
          steps {
            echo "Exec unit test"
            sh 'PGPASSWORD=password psql -U freelancer -d freelancerdb_test -h localhost -f etc/testdata.sql'
            sh 'mvn test'
          }
        }
      }
    }

    stage('Build and Tag OpenShift Image') {
      when {
        expression {
          return env.GIT_BRANCH == "${deploy_branch}" || params.FORCE_FULL_BUILD
        }
      }

      steps {
        echo "Building OpenShift container image"
        script {
          openshift.withCluster() {
            openshift.withProject("${deploy_project}") {
              // update build config
              //sh "oc process -f openshift/templates/application-build.yaml | oc apply -n ${deploy_project} -f -"
              openshift.apply(openshift.process('-f', 'openshift/application-build.yaml', '-p', "NAME=${app_name}"))

              openshift.selector("bc", "${app_name}").startBuild("--from-file=./target/freelancer-service.jar", "--wait=true")
              //openshift.selector("bc", "${app_name}").startBuild("--wait=true")
              openshift.tag("${app_name}:latest", "${app_name}:${env.GIT_COMMIT}")
            }
          }
        }
      }

    }

    stage('deploy') {
      when {
        expression {
          return env.GIT_BRANCH == "${deploy_branch}" || params.FORCE_FULL_BUILD
        }
      }

      steps {
        echo "deploy"
        script {
          openshift.withCluster() {
            openshift.withProject("${deploy_project}") {
              // Apply application manifests
              //sh "oc process -f openshift/templates/application-deploy.yaml -p APP_IMAGE=${app_image} APP_IMAGE_TAG=${env.GIT_COMMIT} | oc apply -n ${deploy_project} -f -"
              openshift.apply(openshift.process('-f', 'openshift/application-deploy.yaml', '-p', "NAME=${app_name}", '-p', "APP_IMAGE=${app_image}", "-p", "APP_IMAGE_TAG=${env.GIT_COMMIT}"))

              // Wait for application to be deployed
              def dc = openshift.selector("dc", "${app_name}").object()
              def dc_version = dc.status.latestVersion
              def rc = openshift.selector("rc", "${app_name}-${dc_version}").object()

              echo "Waiting for ReplicationController ${app_name}-${dc_version} to be ready"
              while (rc.spec.replicas != rc.status.readyReplicas) {
                sleep 5
                rc = openshift.selector("rc", "${app_name}-${dc_version}").object()
              }
            }
          }
        }
      }
    }

    stage('integration-test') {
      when {
        expression {
          return env.GIT_BRANCH == "${deploy_branch}" || params.FORCE_FULL_BUILD
        }
      }

      steps {
        echo "integration-test"
        // TODO
      }
    }
  }
}
