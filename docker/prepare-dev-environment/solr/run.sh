#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# PROJECT_NAME
# SOLR_CLOUD_CONTAINER_1_NAME
# SOLR_CLOUD_CONTAINER_2_NAME
ENV_FILE=${SCRIPT_DIR}/../../dev.env
source ${ENV_FILE}

# countdown
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

REMOVE_VOLUMES=false
LOG_FILE=/dev/stdout
function print_usage() {
  printf '\n%b\n' "Usage: ${0} [ -r ] [ -l FILE ]"
  printf '\n%b\n' "Populate a Docker Compose SolrCloud 8 cluster with bulk Expression Atlas data."

  printf '\n%b\n' "-r\t\tRemove volumes before creating them"
  printf '\n%b\n' "-l FILE \tLog file (default is ${LOG_FILE})"
  printf '%b\n\n' "-h\t\tDisplay usage instructions"
}


while getopts "k:o:l:rh" opt
do
  case ${opt} in
    l)
      LOG_FILE=$OPTARG
      ;;
    r)
      REMOVE_VOLUMES=true
      ;;
    h)
      print_usage
      exit 0
      ;;
    \?)
      printf '%b\n' "Invalid option: -${OPTARG}" >&2
      print_usage
      exit 2
      ;;
  esac
done

DOCKER_COMPOSE_COMMAND="docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
--env-file ${SCRIPT_DIR}/../test-data.env \
--file ${SCRIPT_DIR}/../../docker-compose-postgres.yml \
--file ${SCRIPT_DIR}/../../docker-compose-solrcloud.yml \
--file ${SCRIPT_DIR}/docker-compose.yml"

DOCKER_COMPOSE_SOLRCLOUD_COMMAND="docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
--file ${SCRIPT_DIR}/../../docker-compose-solrcloud.yml"

DOCKER_COMPOSE_COMMAND_VARS="DOCKERFILE_PATH=${SCRIPT_DIR}"

if [ "${REMOVE_VOLUMES}" = "true" ]; then
  countdown "🗑 Remove Docker Compose Solr and ZooKeeper volumes"
  eval "${DOCKER_COMPOSE_SOLRCLOUD_COMMAND}" "down --volumes >> ${LOG_FILE} 2>&1"
  print_done
fi

print_stage_name "🛫 Spin up containers to index bioentity annotations and test experiments metadata and data in Solr"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "up --build >> ${LOG_FILE} 2>&1"
print_done

print_stage_name "🛬 Bring down all services"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down --rmi local >> ${LOG_FILE} 2>&1"
print_done

printf '%b\n' "🙂 All done! You can keep $(basename ${SOLR_PRIVATE_KEY}) and reuse it to sign any other Solr packages."
printf '%b\n' "  Start the SolrCloud cluster again with the following command:"
printf '%b\n\n' "  ${DOCKER_COMPOSE_SOLRCLOUD_COMMAND} up -d"
printf '%b\n\n' "  You can point your browser at http://localhost:8983 to explore your SolrCloud instance."
printf '%b\n' "  Stop the SolrCloud cluster again with the following command:"
printf '%b\n' "  ${DOCKER_COMPOSE_SOLRCLOUD_COMMAND} down"

