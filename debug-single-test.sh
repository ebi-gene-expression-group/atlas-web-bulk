#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source ${SCRIPT_DIR}/docker/dev.env

function print_usage() {
  printf '\n%b\n\n' "Usage: ${0} [ -p SUBREPO_NAME ] -n TEST_NAME"
  printf '%b\n' "Debug a unit/integration test in a module with the given schema version"
  printf '\n%b\n' "-n\tName of the unit/integration test to debug;\n\tfor example: CellPlotDaoIT"
  printf '\n%b\n' "-p\tName of the sub-project the test can be found;\n\tfor example: app or atlas-web-core (default is app)"
  printf '%b\n\n' "-h\tShow usage instructions"
}

REPO_NAME=app
mandatory_name=false

while getopts "n:p:h" opt
do
  case ${opt} in
    n )
      mandatory_name=true; TEST_CASE_NAME=${OPTARG}
      ;;
    p )
      REPO_NAME=${OPTARG}
      if ! [[ "$REPO_NAME" =~ ^(app|atlas-web-core)$ ]]; then
        echo "Project name is not valid: $OPTARG" >&2
        exit 1
      fi
      ;;
    h )
      print_usage
      exit 0
      ;;
    \?)
      printf '%b\n' "Invalid option: -${OPTARG}" >&2
      print_usage
      exit 2
      ;;
    : ) echo "Missing option argument for -$OPTARG" >&2; exit 1;;
  esac
done

if ! $mandatory_name
then
    echo "-n must be provided with the name of the test to execute" >&2
    exit 1
fi

echo "Debugging ${TEST_CASE_NAME}"

docker-compose \
--env-file ${SCRIPT_DIR}/docker/dev.env \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle-test.yml \
run --rm --service-ports \
gxa-gradle bash -c "
set -e

gradle clean

gradle \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/exp \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${PROJECT_NAME}-${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${PROJECT_NAME}-${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
${REPO_NAME}:testClasses

gradle --continuous -PremoteDebug :${REPO_NAME}:test --tests $TEST_CASE_NAME
"
