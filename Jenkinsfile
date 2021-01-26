pipeline {
  // pipelineを実行するagentの設定。yamlファイルで設定を渡せる
  // 可能な限りJenkinsfileにagentの設定をもたせたほうが自動化とGit管理が進むためおすすめ。
  agent {
    kubernetes {
      cloud 'openshift'
      yamlFile 'openshift/jenkins-agent-pod.yaml'
    }
  }

  environment {
    deploy_branch = "origin/master"
    deploy_project = "userxx-development"
    app_name = 'pipeline-practice-java'
    app_image = "image-registry.openshift-image-registry.svc:5000/${deploy_project}/${app_name}"
    JAVA_HOME = """${sh(
                returnStdout: true,
                script: 'alternatives --list | grep "java_sdk_1.8.0\\s" | awk \'{print $3}\' | tr -d \'\\n\''
            )}"""
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
            //sh 'mvn spotbugs:check'
          }
        }

        stage('unit test') {
          steps {
            echo "Exec unit test"
            sh 'PGPASSWORD=password psql -U freelancer -d freelancerdb_test -h localhost -f etc/testdata.sql'
            sh 'mvn test'
            junit allowEmptyResults: true,
                  keepLongStdio: true,
                  healthScaleFactor: 2.0,
                  testResults: '**/target/surefire-reports/TEST-*.xml'
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
        echo "integration-test, e2e-test"
        script {
          openshift.withCluster() {
            openshift.withProject("${deploy_project}") {
              def dc = openshift.selector("route", "${app_name}").object()
              def url = dc.spec.host
              echo "${url}"
              while (true) {
                def app_status = sh(returnStdout: true, script: "curl ${url}/hello -o /dev/null -w '%{http_code}' -s").trim()
                if(app_status == "200") {
                  break;
                }
                sleep 5
              }
              // selenium test
              sh "python src/test/selenium/sample.py http://${url}/health"
            }
          }
        }
      }
    }
  }
}
