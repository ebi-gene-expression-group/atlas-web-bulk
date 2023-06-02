#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# PROJECT_NAME
# POSTGRES_HOST
# POSTGRES_DB
# POSTGRES_USER
# POSTGRES_PASSWORD
ENV_FILE=${SCRIPT_DIR}/../../dev.env
source ${ENV_FILE}

# countdown
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

LOG_FILE=/dev/stdout
REMOVE_VOLUMES=false
function print_usage() {
  printf '\n%b\n' "Usage: ${0} [ -r ] [ -l FILE ]"
  printf '\n%b\n' "Populate a bulk Expression Atlas Postgres 11 database. The experiment design volume will be created"
  printf '\n%b\n' "as a (desirable) side effect."
  printf '\n%b\n' "-r\tRemove volumes before creating them"
  printf '\n%b\n' "-l FILE\tLog file (default is ${LOG_FILE})"
  printf '%b\n\n' "-h\tDisplay usage instructions"
}


while getopts "l:rh" opt
do
  case ${opt} in
    l)
      LOG_FILE=${OPTARG}
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

# Because Flyway (in docker-compose-postgres.yml) is mounting a bind volume with a relative path, it needs to be
# declared first; see https://github.com/docker/compose/issues/3874
DOCKER_COMPOSE_COMMAND="docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
--env-file ${SCRIPT_DIR}/../test-data.env \
--file ${SCRIPT_DIR}/../../docker-compose-postgres.yml \
--file ${SCRIPT_DIR}/docker-compose.yml"

DOCKER_COMPOSE_COMMAND_VARS="DOCKERFILE_PATH=${SCRIPT_DIR}"

if [ "${REMOVE_VOLUMES}" = "true" ]; then
  countdown "🗑 Remove Docker Compose volumes of Postgres"
  eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down --volumes >> ${LOG_FILE} 2>&1"
  print_done
fi

print_stage_name "🛫 Spin up service to load test experiments in Postgres"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "up --build >> ${LOG_FILE} 2>&1"
print_done

print_stage_name "🛬 Bring down all services"
eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down --rmi local >> ${LOG_FILE} 2>&1"
print_done

DOCKER_COMPOSE_POSTGRES_COMMAND="docker compose \
--env-file ${ENV_FILE} \
--file ${SCRIPT_DIR}/../../docker-compose-postgres.yml"

printf '%b\n' "🙂 All done!"
printf '%b\n\n' "   The Postgres 11 container exposes port 5432, so you should be able to connect with psql (password is ${POSTGRES_PASSWORD}):"
printf '%b\n' "   ${DOCKER_COMPOSE_POSTGRES_COMMAND} up -d && \\"
printf '%b\n' "   echo 'SELECT * FROM experiment' | psql -h localhost -d ${POSTGRES_DB} -U ${POSTGRES_USER} && \\"
printf '%b\n' "   ${DOCKER_COMPOSE_POSTGRES_COMMAND} down"