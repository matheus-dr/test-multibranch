def ENVIRONMENT_MAP = [
  'Development': 'develop;dev',
  'Production': 'main;prod',
  'Release': 'null;qa',
]

pipeline {
  agent any

  parameters {
    booleanParam(name: 'isGoingToDeploy', defaultValue: false)
    string(name: 'projectName', defaultValue: '')
    string(name: 'sshKey', defaultValue: '')
    string(name: 'appRegistry', defaultValue: '')
    choice(name: 'environmentParam', choices: ['Development', 'Production'])
    string(name: 'appRegistryUrl', defaultValue: '')
    string(name: 'ecrRegistryCredential', defaultValue: '')
    string(name: 'awsCredentials', defaultValue: '')
    string(name: 'awsRegion', defaultValue: '')
    string(name: 'baseRef', defaultValue: '')
  }

  options {
    buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '5', daysToKeepStr: '', numToKeepStr: '5')
    disableConcurrentBuilds()
  }

  stages {
    stage('Setup') {
      steps {
        script {
          def vars = ENVIRONMENT_MAP[params.environmentParam].split(';')

          env.branchName = params.environmentParam == 'Release' ? baseRef : vars[0]
          env.suffix = vars[1]
        }
      }
    }

    stage('Checkout SCM') {
      steps {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "*/${env.branchName}"]],
          extensions: [[$class: 'CleanCheckout']],
          doGenerateSubmoduleConfigurations: false,
          submoduleCfg: [],
          userRemoteConfigs: [[credentialsId: params.sshKey, url: GIT_URL]]
        ])
      }
    }

    /*stage('Cleaning old dependencies and cache') {
      steps {
        sh 'composer clearcache'
        sh 'rm -rf vendor/*'
      }
    }

    stage('Installing dependencies') {
      steps {
        sh 'composer install'
      }
    }

    stage('Building image with docker') {
      steps {
        sh "docker build -t ${params.appRegistry}:${env.suffix}-latest ."
      }
    }

    stage('Testing project') {
      steps {
        sh './vendor/bin/phpunit'
      }
    }*/

    stage('Debug') {
      steps {
        echo 'It is working :)'
      }
    }

    stage('Enable CD') {
      steps {
        build job: "${env.projectName}/cd",
          parameters: [
            string(name: 'sshKey', value: params.sshKey),
            string(name: 'projectName', value: params.projectName),
            string(name: 'environmentParam', value: params.environmentParam),
            string(name: 'appRegistryUrl', value: params.appRegistryUrl),
            string(name: 'ecrRegistryCredential', value: params.ecrRegistryCredential),
            string(name: 'awsCredentials', value: params.awsCredentials),
            string(name: 'awsRegion', value: params.awsRegion),
            string(name: 'baseRef', value: params.baseRef)
          ],
          wait: false,
          propagate: false
      }

      when {
        expression {
          return params.isGoingToDeploy
        }
      }
    }
  }
}