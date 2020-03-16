#!groovy
def deploy_branch = "origin/master"
def deploy_project = "app-development"
def build_config_name = "freelancer-build"
def app_image = "image-registry.openshift-image-registry.svc:5000/${deploy_project}/${build_config_name}"

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
    stage('Checkout Source') {
      steps {
        checkout scm
      }
    }

    stage('Setup') {
      steps {
				sh 'java -version'
				sh 'mvn -v'
      }
    }

    stage('application test') {
      parallel {
        stage('Code analysis') {
          steps {
            echo "Exec static analysis"
            sh 'PGPASSWORD=password psql -U freelancer -d freelancerdb_test -h localhost -f etc/testdata.sql'
            sh 'mvn clean test'
          }
        }

        stage('unit test') {
          steps {
            echo "Exec unit test"
          }
        }
      }
    }

    stage('build') {
      steps {
				sh 'mvn clean package -DskipTests'
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
              openshift.apply(openshift.process('-f', 'openshift/application-build.yaml', '-p', "NAME=${build_config_name}"))

              openshift.selector("bc", "${build_config_name}").startBuild("--from-file=./target/freelancer-service.jar", "--wait=true")
              //openshift.selector("bc", "${build_config_name}").startBuild("--wait=true")
              openshift.tag("${build_config_name}:latest", "${build_config_name}:${env.GIT_COMMIT}")
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
              //sh "oc process -f openshift/templates/application-deploy.yaml -p APP_IMAGE=${app_image} APP_IMAGE_TAG=${env.GIT_COMMIT} | oc apply -n ${deploy_project} -f -"
              openshift.apply(openshift.process('-f', 'openshift/application-deploy.yaml', '-p', "APP_IMAGE=${app_image}", "-p", "APP_IMAGE_TAG=${env.GIT_COMMIT}"))
            }
          }
        }
      }
    }
  }
}
