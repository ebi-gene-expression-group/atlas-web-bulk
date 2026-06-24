pipeline {
  parameters {
    booleanParam(
      name: 'SKIP_TESTS',
      defaultValue: false,
      description: 'Skip unit and integration tests (compile and WAR build still run).'
    )
  }

  options {
    buildDiscarder(logRotator(
      numToKeepStr: '10',
      artifactNumToKeepStr: '5',
    ))
    disableConcurrentBuilds()
  }

  agent {
    kubernetes {
      cloud 'hh-webadmin-35'
      workspaceVolume dynamicPVC(storageClassName: 'standard-nfs-production', accessModes: 'ReadWriteOnce', requestsSize: '6Gi')
      defaultContainer 'openjdk'
      yamlFile 'jenkins-k8s-pod.yaml'
    }
  }

  environment {
    ORG_GRADLE_PROJECT_buildNumber = "${env.BUILD_NUMBER}"
    APP_NAME = 'gxa'
    REGISTRY = 'dockerhub.ebi.ac.uk'
    IMAGE = 'dockerhub.ebi.ac.uk/ebi-gene-expression/atlas-web-bulk/gxa'
  }

  stages {


    stage('Provision Gradle') {
      options {
        timeout (time: 20, unit: "MINUTES")
      }
      steps {
        sh '''
          if [ -d /gradle-ro-dep-cache/modules-2 ]; then
            echo "Gradle RO dep cache seeded."
            export GRADLE_RO_DEP_CACHE=/gradle-ro-dep-cache
          else
            echo "Gradle RO dep cache not seeded yet (/gradle-ro-dep-cache/modules-2 missing); downloading dependencies."
            unset GRADLE_RO_DEP_CACHE
          fi
          mkdir -p build
          ./gradlew --no-watch-fs --console=plain --info tasks
        '''
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
                '-PexperimentDesignLocation=/tmp/expdesign-rw ' +
                "-PjdbcUrl=jdbc:postgresql://localhost:5432/postgres?currentSchema=${env.APP_NAME} " +
                '-PjdbcUsername=postgres ' +
                '-PjdbcPassword=postgres ' +
                "-PzkHosts=${env.APP_NAME}-solrcloud-zookeeper-client.${env.APP_NAME}-ci-solrcloud.svc.cluster.local:2181 " +
                "-PsolrHosts=http://${env.APP_NAME}-solrcloud-common.${env.APP_NAME}-ci-solrcloud.svc.cluster.local/solr " +
                '-PsolrUser=solr ' +
                '-PsolrPassword=SolrRocks ' +
                ':atlas-web-core:testClasses :app:testClasses'
      }
    }

    stage('–– Unit Tests ––') {
      when { expression { !params.SKIP_TESTS } }
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
      when { expression { !params.SKIP_TESTS } }
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
            sh 'echo \'APT::Acquire::Retries "10";\' > /etc/apt/apt.conf.d/80-retries'
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
          pushGitTags(ver)
        }
      }
    }
  }

  post {
    always {
      script {
        if (!params.SKIP_TESTS) {
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
  def fromManifest = container('openjdk') {
    sh(
      script: """
        set -e
        MANIFEST_DIR=\$(mktemp -d)
        cd "\${MANIFEST_DIR}"
        jar xf "\${WORKSPACE}/webapps/${env.APP_NAME}.war" META-INF/MANIFEST.MF
        awk -F': ' '/Implementation-Version/{print \$2; exit}' META-INF/MANIFEST.MF
      """,
      returnStdout: true
    ).trim()
  }
  if (fromManifest) {
    return fromManifest
  }

  return sh(
    script: './gradlew --no-watch-fs -q :app:printVersion',
    returnStdout: true
  ).trim()
}

def pushGitTags(String ver) {
  container('jnlp') {
    withCredentials([usernamePassword(
      credentialsId: 'github-atlas-web-bulk',
      usernameVariable: 'GIT_USER',
      passwordVariable: 'GIT_TOKEN'
    )]) {
      sh '''
        git config user.email "jenkins@ebi.ac.uk"
        git config user.name "Jenkins CI"
      '''
      ["${ver}", "${ver}-cli"].each { tag ->
        echo "Tagging and pushing ${tag}"
        sh """
          git tag -fa '${tag}' -m 'build ${env.BUILD_NUMBER}' '${env.GIT_COMMIT}'
          git push -f "https://\${GIT_USER}:\${GIT_TOKEN}@github.com/ebi-gene-expression-group/atlas-web-bulk.git" 'refs/tags/${tag}'
        """
      }
    }
  }
}

def pushDockerImage(String appVersion) {
  echo "Building and pushing ${env.IMAGE}:${appVersion} and ${env.IMAGE}:latest"
  container('kaniko') {
    withCredentials([usernamePassword(
      credentialsId: 'gitlab-gxa-container-registry',
      usernameVariable: 'REGISTRY_USER',
      passwordVariable: 'REGISTRY_PASSWORD'
    )]) {
      sh """
        set -eu
        set -x
        test -n "\$REGISTRY_USER"
        test -n "\$REGISTRY_PASSWORD"
        test -f "\${WORKSPACE}/webapps/${env.APP_NAME}.war"

        mkdir -p /tmp
        KANIKO_CONTEXT=\$(mktemp -d -p /tmp kaniko-context.XXXXXX)
        trap 'rm -rf "\$KANIKO_CONTEXT"' EXIT
        cp -a "\${WORKSPACE}/." "\$KANIKO_CONTEXT/"

        mkdir -p /kaniko/.docker
        AUTH=\$(printf '%s:%s' "\$REGISTRY_USER" "\$REGISTRY_PASSWORD" | base64 | tr -d '\\n')
        printf '{"auths":{"${env.REGISTRY}":{"auth":"%s"}}}' "\$AUTH" > /kaniko/.docker/config.json

        /kaniko/executor \\
            --verbosity=info \\
            --context "dir://\${KANIKO_CONTEXT}" \\
            --dockerfile "\${KANIKO_CONTEXT}/Dockerfile" \\
            --destination ${env.IMAGE}:${appVersion} \\
            --destination ${env.IMAGE}:latest
      """
    }
  }
}
