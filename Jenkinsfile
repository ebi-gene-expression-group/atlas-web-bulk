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
    // 4 parallel test forks × 20 default Hikari pool exceeds sidecar Postgres max_connections (100).
    GRADLE_CI_TEST_PROPS = '-PjdbcMaxPoolSize=5'
  }

  stages {


    stage('Provision Gradle') {
      options {
        timeout (time: 20, unit: "MINUTES")
      }
      steps {
        sh '''
          if [ -d /gradle-ro-dep-cache/modules-2 ]; then
            echo "Gradle RO dep cache: /gradle-ro-dep-cache/modules-2 present"
          else
            echo "WARNING: Gradle RO dep cache not seeded (/gradle-ro-dep-cache/modules-2 missing)"
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
        withSolrCredentials {
          sh './gradlew --no-watch-fs ' +
                  '-PdataFilesLocation=/test-data ' +
                  "-PexperimentFilesLocation=/test-data/${env.APP_NAME} " +
                  '-PexperimentDesignLocation=/tmp/expdesign-rw ' +
                  "-PjdbcUrl=jdbc:postgresql://localhost:5432/postgres?currentSchema=${env.APP_NAME} " +
                  '-PjdbcUsername=postgres ' +
                  '-PjdbcPassword=postgres ' +
                  "${env.GRADLE_CI_TEST_PROPS} " +
                  "-PzkHosts=${env.APP_NAME}-solrcloud-zookeeper-client.${env.APP_NAME}-ci-solrcloud.svc.cluster.local:2181 " +
                  "-PsolrHosts=http://${env.APP_NAME}-solrcloud-common.${env.APP_NAME}-ci-solrcloud.svc.cluster.local/solr " +
                  '-PsolrUser=admin ' +
                  '-PsolrPassword="${SOLR_PASS}" ' +
                  ':atlas-web-core:testClasses :app:testClasses'
        }
      }
    }

    stage('–– Unit Tests ––') {
      when { expression { !params.SKIP_TESTS } }
      options {
        timeout (time: 2, unit: "HOURS")
      }
      steps {
        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
          sh "./gradlew --no-watch-fs ${env.GRADLE_CI_TEST_PROPS} -PtestResultsPath=ut :atlas-web-core:test --tests *Test"
          sh "./gradlew --no-watch-fs ${env.GRADLE_CI_TEST_PROPS} -PtestResultsPath=ut :app:test --tests *Test"
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
          withSolrCredentials {
            sh "./gradlew --no-watch-fs ${env.GRADLE_CI_TEST_PROPS} -PtestResultsPath=it :atlas-web-core:test --tests *IT"
            sh "./gradlew --no-watch-fs ${env.GRADLE_CI_TEST_PROPS} -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT " +
                    '-PsolrUser=admin -PsolrPassword="${SOLR_PASS}"'
            sh "./gradlew --no-watch-fs ${env.GRADLE_CI_TEST_PROPS} -PtestResultsPath=e2e :app:test --tests *WIT " +
                    '-PsolrUser=admin -PsolrPassword="${SOLR_PASS}"'
          }
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
            container('node-build') {
              sh '''
                if [ -d /npm-cache/_cacache ]; then
                  echo "npm CI cache: reusing /npm-cache"
                else
                  echo "npm CI cache: empty or not seeded; npm install will populate /npm-cache"
                fi
                mkdir -p /npm-cache
              '''
              sh 'echo \'APT::Acquire::Retries "10";\' > /etc/apt/apt.conf.d/80-retries'
              sh 'apt update && apt install -y libglu1-mesa gcc'
              sh 'curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash'
              sh 'bash -lc "source $HOME/.nvm/nvm.sh && nvm install 14 --lts"'
              sh 'bash -lc "source $HOME/.nvm/nvm.sh && npm install -g npm-check-updates"'
            }
          }
        }

        stage('Update and build ES bundles') {
          options {
            timeout (time: 1, unit: "HOURS")
          }
          steps {
            container('node-build') {
              sh 'bash -lc \'if [ "$BRANCH_NAME" = "develop" ]; then WEBPACK_OPTS=-i; else WEBPACK_OPTS=-ip; fi; ' +
                      'source "$HOME/.nvm/nvm.sh"; ./compile-front-end-packages.sh ${WEBPACK_OPTS}\''
            }
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
              echo 'Resolving application version from WAR manifest...'
              def appVersion = resolveAppVersion()
              echo "Resolved application version: ${appVersion}"
              pushDockerImage(appVersion)
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
          junit testResults: 'atlas-web-core/build/ut/**/*.xml', allowEmptyResults: true
          junit testResults: 'atlas-web-core/build/it/**/*.xml', allowEmptyResults: true

          junit testResults: 'app/build/ut/**/*.xml', allowEmptyResults: true
          junit testResults: 'app/build/it/**/*.xml', allowEmptyResults: true
          junit testResults: 'app/build/e2e/**/*.xml', allowEmptyResults: true

          archiveArtifacts artifacts: 'atlas-web-core/build/reports/**', fingerprint: true, allowEmptyArchive: true
          archiveArtifacts artifacts: 'app/build/reports/**', fingerprint: true, allowEmptyArchive: true
          archiveArtifacts artifacts: 'app/src/main/webapp/resources/js-bundles/report.html', fingerprint: true, allowEmptyArchive: true
        }
      }
    }
  }
}

def withSolrCredentials(Closure body) {
  // Secret text credential: Solr Operator bootstrap admin password for gxa-ci-solrcloud.
  // kubectl get secret gxa-solrcloud-security-bootstrap -n gxa-ci-solrcloud \
  //   -o jsonpath='{.data.admin}' | base64 -d
  withCredentials([string(credentialsId: 'gxa-ci-solr-admin', variable: 'SOLR_PASS')]) {
    withEnv([
      'SOLR_USER=admin',
      "JAVA_TOOL_OPTIONS=${env.JAVA_TOOL_OPTIONS ?: ''} " +
        '-Dsolr.httpclient.builder.factory=org.apache.solr.client.solrj.impl.PreemptiveBasicAuthClientBuilderFactory ' +
        "-Dbasicauth=admin:${env.SOLR_PASS}",
    ]) {
      body()
    }
  }
}

def resolveAppVersion() {
  def fromManifest = container('openjdk') {
    sh """
      set -eu
      echo "Reading Implementation-Version from \${WORKSPACE}/webapps/${env.APP_NAME}.war"
      ls -lh "\${WORKSPACE}/webapps/${env.APP_NAME}.war"
      MANIFEST_DIR=\$(mktemp -d)
      cd "\${MANIFEST_DIR}"
      jar xf "\${WORKSPACE}/webapps/${env.APP_NAME}.war" META-INF/MANIFEST.MF
      awk -F': ' '/Implementation-Version/{print \$2; exit}' META-INF/MANIFEST.MF > "\${WORKSPACE}/.app-version"
    """
    readFile('.app-version').trim()
  }
  if (fromManifest) {
    return fromManifest
  }

  echo 'WAR manifest had no Implementation-Version; falling back to Gradle printVersion'
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
        set +x
        set -eu
        git config user.email "jenkins@ebi.ac.uk"
        git config user.name "Jenkins CI"
        API_LOG=$(mktemp)
        HTTP_CODE=$(curl -sS -o "${API_LOG}" -w '%{http_code}' \
          -u "x-access-token:${GIT_TOKEN}" \
          https://api.github.com/repos/ebi-gene-expression-group/atlas-web-bulk)
        if [ "${HTTP_CODE}" != "200" ]; then
          echo "GitHub API check failed (HTTP ${HTTP_CODE}). Token cannot access atlas-web-bulk."
          cat "${API_LOG}"
          echo "Fix Jenkins credential github-atlas-web-bulk: PAT needs repo write on this repo,"
          echo "and if the org uses SSO you must Authorize the token for ebi-gene-expression-group"
          echo "at https://github.com/settings/tokens"
          exit 1
        fi
        PERMS=$(grep -E '"push"|"admin"' "${API_LOG}" || true)
        echo "GitHub repo access OK (${PERMS})"
      '''
      ["${ver}", "${ver}-cli"].each { tag ->
        echo "Tagging and pushing ${tag} (commit ${env.GIT_COMMIT})"
        sh """
          set +x
          set -eu
          git tag -fa '${tag}' -m 'build ${env.BUILD_NUMBER}' '${env.GIT_COMMIT}'
          PUSH_LOG=\$(mktemp)
          if ! git -c http.lowSpeedLimit=1 -c http.lowSpeedTime=60 push -f \\
              "https://x-access-token:\${GIT_TOKEN}@github.com/ebi-gene-expression-group/atlas-web-bulk.git" \\
              'refs/tags/${tag}' 2>"\${PUSH_LOG}"; then
            echo "git push failed for tag ${tag}:"
            cat "\${PUSH_LOG}"
            exit 1
          fi
          echo "Pushed tag ${tag}"
        """
      }
    }
  }
}

def pushDockerImage(String appVersion) {
  echo "Building and pushing ${env.IMAGE}:${appVersion} and ${env.IMAGE}:latest"
  container('kaniko') {
    sh """
      set -eu
      echo "kaniko: workspace=\${WORKSPACE}"
      ls -lh "\${WORKSPACE}/webapps/${env.APP_NAME}.war"
      echo "contents of workspace:"
      test -f "\${WORKSPACE}/Dockerfile"
    """
    withCredentials([usernamePassword(
      credentialsId: 'gitlab-gxa-container-registry',
      usernameVariable: 'REGISTRY_USER',
      passwordVariable: 'REGISTRY_PASSWORD'
    )]) {
      sh """
        set +x
        set -eu
        echo "kaniko: writing registry auth config"
        test -n "\$REGISTRY_USER"
        test -n "\$REGISTRY_PASSWORD"
        mkdir -p /kaniko/.docker
        AUTH=\$(printf '%s:%s' "\$REGISTRY_USER" "\$REGISTRY_PASSWORD" | base64 | tr -d '\\n')
        printf '{"auths":{"${env.REGISTRY}":{"auth":"%s"}}}' "\$AUTH" > /kaniko/.docker/config.json
        echo "kaniko: starting image build"
        /kaniko/executor \\
            --verbosity=info \\
            --context "\${WORKSPACE}" \\
            --dockerfile "\${WORKSPACE}/Dockerfile" \\
            --destination ${env.IMAGE}:${appVersion} \\
            --destination ${env.IMAGE}:latest
      """
    }
  }
}
