pipeline {
  parameters {
    booleanParam(
      name: 'DOCKER_ONLY',
      defaultValue: false,
      description: 'Skip tests and WAR build; copy WAR from a previous successful build and push a Docker image only.'
    )
    string(
      name: 'WAR_BUILD_NUMBER',
      defaultValue: '',
      description: 'When DOCKER_ONLY: build number to copy the WAR from (blank = last successful build of this job).'
    )
    string(
      name: 'APP_VERSION',
      defaultValue: '',
      description: 'Docker image tag to push (e.g. 1.2.3). Blank = read from WAR manifest, or Gradle on full builds.'
    )
  }

  options {
    buildDiscarder(logRotator(
      numToKeepStr: '10',
      artifactNumToKeepStr: '5',
    ))
    disableConcurrentBuilds()
  }

  agent none

  environment {
    ORG_GRADLE_PROJECT_buildNumber = "${env.BUILD_NUMBER}"
    APP_NAME = 'gxa'
    REGISTRY = 'dockerhub.ebi.ac.uk'
    IMAGE = 'dockerhub.ebi.ac.uk/ebi-gene-expression/atlas-web-bulk/gxa'
  }

  stages {
    stage('Full pipeline') {
      when { expression { !params.DOCKER_ONLY } }
      agent {
        kubernetes {
          cloud 'gke-autopilot'
          workspaceVolume dynamicPVC(storageClassName: 'premium-rwo', accessModes: 'ReadWriteOnce', requestsSize: '6Gi')
          defaultContainer 'openjdk'
          yamlFile 'jenkins-k8s-pod.yaml'
        }
      }
      stages {
        stage('Scale SolrCloud') {
          steps {
            container('kubectl') {}
          }
        }

        stage('Provision Gradle') {
          options {
            timeout (time: 20, unit: "MINUTES")
          }
          steps {
            sh './gradlew --no-watch-fs'
          }
        }

        stage('–– Compile ––') {
          options {
            timeout (time: 1, unit: "HOURS")
          }
          steps {
            sh './gradlew --no-watch-fs ' +
                    '-Pflyway.url=jdbc:postgresql://localhost:5432/postgres ' +
                    '-Pflyway.user=postgres ' +
                    '-Pflyway.password=postgres ' +
                    "-Pflyway.locations=filesystem:./schemas/flyway/${env.APP_NAME} " +
                    "-Pflyway.schemas=${env.APP_NAME} " +
                    'flywayMigrate'
            sh './gradlew --no-watch-fs ' +
                    '-PdataFilesLocation=/test-data ' +
                    "-PexperimentFilesLocation=/test-data/${env.APP_NAME} " +
                    '-PexperimentDesignLocation=/root/expdesign-rw ' +
                    "-PjdbcUrl=jdbc:postgresql://localhost:5432/postgres?currentSchema=${env.APP_NAME} " +
                    '-PjdbcUsername=postgres ' +
                    '-PjdbcPassword=postgres ' +
                    "-PzkHosts=${env.APP_NAME}-solrcloud-zookeeper-0.${env.APP_NAME}-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181,${env.APP_NAME}-solrcloud-zookeeper-1.${env.APP_NAME}-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181,${env.APP_NAME}-solrcloud-zookeeper-2.${env.APP_NAME}-solrcloud-zookeeper-headless.jenkins-gene-expression.svc.cluster.local:2181 " +
                    "-PsolrHosts=http://${env.APP_NAME}-solrcloud-0.${env.APP_NAME}-solrcloud-headless.jenkins-gene-expression.svc.cluster.local:8983/solr,http://${env.APP_NAME}-solrcloud-1.${env.APP_NAME}-solrcloud-headless.jenkins-gene-expression.svc.cluster.local:8983/solr,http://${env.APP_NAME}-solrcloud-2.${env.APP_NAME}-solrcloud-headless.jenkins-gene-expression.svc.cluster.local:8983/solr,http://${env.APP_NAME}-solrcloud-3.${env.APP_NAME}-solrcloud-headless.jenkins-gene-expression.svc.cluster.local:8983/solr " +
                    '-PsolrUser=solr ' +
                    '-PsolrPassword=SolrRocks ' +
                    ':atlas-web-core:testClasses :app:testClasses'
          }
        }

        stage('–– Unit Tests ––') {
          options {
            timeout (time: 2, unit: "HOURS")
          }
          steps {
            catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
              sh './gradlew --no-watch-fs -PtestResultsPath=ut :atlas-web-core:test --tests *Test'
              sh './gradlew --no-watch-fs -PtestResultsPath=ut :app:test --tests *Test'
            }
          }
        }

        stage('–– Integration Tests ––') {
          options {
            timeout (time: 2, unit: "HOURS")
          }
          steps {
            catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
              sh './gradlew --no-watch-fs -PtestResultsPath=it :atlas-web-core:test --tests *IT'
              sh './gradlew -PsolrUser=solr -PsolrPassword=SolrRocks --no-watch-fs -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT'
              sh './gradlew -PsolrUser=solr -PsolrPassword=SolrRocks --no-watch-fs -PtestResultsPath=e2e :app:test --tests *WIT'
              sh './gradlew --no-watch-fs --parallel :atlas-web-core:jacocoTestReport :app:jacocoTestReport'
            }
          }
        }

        stage('–– Build ––') {
          when { anyOf {
            branch 'develop'; branch 'main'; branch 'release/*'; branch 'chore/*'; branch 'feature/*'
          } }
          stages {
            stage('Provision Node.js build environment') {
              options {
                timeout (time: 1, unit: "HOURS")
              }
              steps {
                // To avoid the unpleasant:
                // Err:4 http://deb.debian.org/debian bullseye-updates InRelease
                //   Connection timed out [IP: 199.232.174.132 80]
                sh 'echo \'APT::Acquire::Retries "10";\' > /etc/apt/apt.conf.d/80-retries'

                // Required by node_modules/cwebp-bin
                sh 'apt update && apt install -y libglu1-mesa gcc'
                sh 'curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash'
                sh 'bash -lc "source $HOME/.nvm/nvm.sh && nvm install 14 --lts"'
                sh 'bash -lc "source $HOME/.nvm/nvm.sh && npm install -g npm-check-updates"'
              }
            }

            stage('Update and build ES bundles') {
              options {
                timeout (time: 1, unit: "HOURS")
              }
              steps {
                sh 'bash -lc \'if [ "$BRANCH_NAME" = "develop" ]; then WEBPACK_OPTS=-i; else WEBPACK_OPTS=-ip; fi; ' +
                        'source "$HOME/.nvm/nvm.sh"; ./compile-front-end-packages.sh ${WEBPACK_OPTS}\''
              }
            }

            stage('Assemble WAR file') {
              options {
                timeout (time: 1, unit: "HOURS")
              }
              steps {
                sh './gradlew --no-watch-fs :app:war'
                archiveArtifacts artifacts: "webapps/${env.APP_NAME}.war", fingerprint: true
              }
            }

            stage('Build and push Docker image') {
              when { branch 'develop' }
              options {
                timeout (time: 1, unit: "HOURS")
              }
              steps {
                script {
                  pushDockerImage(resolveAppVersion())
                }
              }
            }
          }
        }

        stage('Tag release') {
          when { branch 'develop' }
          steps {
            script {
              def ver = sh(
                script: './gradlew --no-watch-fs -q :app:printVersion',
                returnStdout: true
              ).trim()

              echo "Tagging and pushing version ${ver}"
              def tags = ["${ver}", "${ver}-cli"]
              tags.each { tag ->
                echo "Tagging and pushing version ${tag}"
                sh "git tag -a ${tag} -m 'build ${env.BUILD_NUMBER}' ${env.GIT_COMMIT}"
                sh "git push origin ${tag}"
              }
            }
          }
        }
      }
    }

    stage('Docker image only') {
      when { expression { params.DOCKER_ONLY } }
      agent {
        kubernetes {
          cloud 'gke-autopilot'
          workspaceVolume dynamicPVC(storageClassName: 'premium-rwo', accessModes: 'ReadWriteOnce', requestsSize: '2Gi')
          yamlFile 'jenkins-k8s-pod-docker.yaml'
        }
      }
      stages {
        stage('Copy WAR artifact') {
          steps {
            script {
              def selector = params.WAR_BUILD_NUMBER?.trim()
                ? specific(params.WAR_BUILD_NUMBER.trim())
                : lastSuccessful()

              copyArtifacts(
                projectName: env.JOB_NAME,
                selector: selector,
                filter: "webapps/${env.APP_NAME}.war",
                target: '.',
              )
            }
          }
        }

        stage('Build and push Docker image') {
          options {
            timeout (time: 1, unit: "HOURS")
          }
          steps {
            script {
              pushDockerImage(resolveAppVersion())
            }
          }
        }
      }
    }
  }

  post {
    always {
      script {
        if (!params.DOCKER_ONLY) {
          junit 'atlas-web-core/build/ut/**/*.xml'
          junit 'atlas-web-core/build/it/**/*.xml'

          junit 'app/build/ut/**/*.xml'
          junit 'app/build/it/**/*.xml'
          junit 'app/build/e2e/**/*.xml'

          archiveArtifacts artifacts: 'atlas-web-core/build/reports/**', fingerprint: true, allowEmptyArchive: true
          archiveArtifacts artifacts: 'app/build/reports/**', fingerprint: true, allowEmptyArchive: true
          archiveArtifacts artifacts: 'app/src/main/webapp/resources/js-bundles/report.html', fingerprint: true, allowEmptyArchive: true
        }
      }
    }
  }
}

def resolveAppVersion() {
  if (params.APP_VERSION?.trim()) {
    return params.APP_VERSION.trim()
  }

  def fromManifest = sh(
    script: "unzip -p webapps/${env.APP_NAME}.war META-INF/MANIFEST.MF | awk -F': ' '/Implementation-Version/{print \$2; exit}'",
    returnStdout: true
  ).trim()
  if (fromManifest) {
    return fromManifest
  }

  if (params.DOCKER_ONLY) {
    error('Could not read Implementation-Version from WAR manifest. Set the APP_VERSION parameter.')
  }

  return sh(
    script: './gradlew --no-watch-fs -q :app:printVersion',
    returnStdout: true
  ).trim()
}

def pushDockerImage(String appVersion) {
  echo "Building and pushing ${env.IMAGE}:${appVersion}"
  container('kaniko') {
    withCredentials([usernamePassword(
      credentialsId: 'gitlab-gxa-container-registry',
      usernameVariable: 'REGISTRY_USER',
      passwordVariable: 'REGISTRY_PASSWORD'
    )]) {
      sh """
        mkdir -p /kaniko/.docker
        AUTH=\$(printf '%s:%s' "\$REGISTRY_USER" "\$REGISTRY_PASSWORD" | base64 | tr -d '\\n')
        printf '{"auths":{"${env.REGISTRY}":{"auth":"%s"}}}' "\$AUTH" > /kaniko/.docker/config.json
        /kaniko/executor \\
            --context "\$WORKSPACE" \\
            --dockerfile "\$WORKSPACE/Dockerfile" \\
            --destination ${env.IMAGE}:${appVersion}
      """
    }
  }
}
