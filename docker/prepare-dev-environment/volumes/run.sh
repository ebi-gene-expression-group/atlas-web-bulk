#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# EXP_IDS
source ${SCRIPT_DIR}/../test-data.env
# ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME
# ATLAS_DATA_GXA_VOL_NAME
# ATLAS_DATA_GXA_EXPDESIGN_VOL_NAME
# WEBAPP_PROPERTIES_VOL_NAME
source ${SCRIPT_DIR}/../../dev.env
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

function print_usage() {
  printf '\n%b\n\n' "Usage: ${0} [ -r ] [ -l FILE ]"
  printf '%b\n' "Provision four Docker volumes for bulk Expression Atlas testing & development:"
  printf '%b\n' " • ${ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME}: gene annotations from array designs, Ensembl, Reactome,"
  printf '%b\n' "                                         WormBase ParaSite, miRBase, Gene Ontology and InterPro"
  printf '%b\n' " • ${ATLAS_DATA_GXA_VOL_NAME}: test experiments data bundles, species and release definition files"
  printf '%b\n' " • ${ATLAS_DATA_GXA_EXPDESIGN_VOL_NAME}: experiment design files of test experiments"
  printf '%b\n' " • ${WEBAPP_PROPERTIES_VOL_NAME}: configuration files for the web application"
  printf '\n%b\n' "-r\tRemove volumes before creating them"
  printf '\n%b\n' "-l FILE\tLog file (default is /dev/stdout)"
  printf '%b\n\n' "-h\tShow usage instructions"
}

LOG_FILE=/dev/stdout
REMOVE_VOLUMES=false
while getopts "rl:h" opt
do
  case ${opt} in
    r)
      REMOVE_VOLUMES=true
      ;;
    l)
      LOG_FILE=$OPTARG
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

ALL_VOLUMES="${ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME} ${ATLAS_DATA_GXA_VOL_NAME} ${ATLAS_DATA_GXA_EXPDESIGN_VOL_NAME} ${WEBAPP_PROPERTIES_VOL_NAME}"

if [ "${REMOVE_VOLUMES}" = "true" ]; then
  print_stage_name "🗑 Remove Docker volumes: ${ALL_VOLUMES}"
  for VOL_NAME in ${ALL_VOLUMES}
  do
    docker volume rm ${VOL_NAME} >> ${LOG_FILE} 2>&1 || true
  done
  print_done
fi

print_stage_name "💾 Create Docker volumes: ${ALL_VOLUMES}"
for VOL_NAME in ${ALL_VOLUMES}
do
  docker volume create ${VOL_NAME} >> ${LOG_FILE} 2>&1
done
print_done

IMAGE_NAME=gxa-volumes-populator

print_stage_name "🚧 Build Docker image ${IMAGE_NAME}"
docker build \
-t ${IMAGE_NAME} ${SCRIPT_DIR} >> ${LOG_FILE} 2>&1
print_done

ATLAS_DATA_BIOENTITY_PROPERTIES_MAPPING=${ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME}:/atlas-data/bioentity_properties:rw
ATLAS_DATA_GXA_MAPPING=${ATLAS_DATA_GXA_VOL_NAME}:/atlas-data/gxa:rw
WEBAPP_PROPERTIES_MAPPING=${WEBAPP_PROPERTIES_VOL_NAME}:/webapp-properties:rw
ATLAS_DATA_GXA_EXPDESIGN_MAPPING=${ATLAS_DATA_GXA_EXPDESIGN_VOL_NAME}:/atlas-data/gxa-expdesign:rw

print_stage_name "⚙ Spin up ephemeral container to populate volumes"
docker run --rm \
-v ${ATLAS_DATA_BIOENTITY_PROPERTIES_MAPPING} \
-v ${ATLAS_DATA_GXA_MAPPING} \
-v ${WEBAPP_PROPERTIES_MAPPING} \
-v ${ATLAS_DATA_GXA_EXPDESIGN_MAPPING} \
-e EXP_IDS="${EXP_IDS}" \
${IMAGE_NAME} >> ${LOG_FILE} 2>&1
print_done

printf '%b\n' "🙂 All done! You can inspect the volume contents attaching them to a container:"
printf '%b\n' "   # ${ATLAS_DATA_GXA_EXPDESIGN_VOL_NAME} will be empty until we load data in Postgres"
printf '%b\n' "   docker run \\"
printf '%b\n' "   -v ${ATLAS_DATA_BIOENTITY_PROPERTIES_MAPPING} \\"
printf '%b\n' "   -v ${ATLAS_DATA_GXA_MAPPING} \\"
printf '%b\n' "   -v ${WEBAPP_PROPERTIES_MAPPING} \\"
printf '%b\n' "   -v ${ATLAS_DATA_GXA_EXPDESIGN_MAPPING} \\"
printf '%b\n' "   --rm -it ubuntu:jammy bash"
