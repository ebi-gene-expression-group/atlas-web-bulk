#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# EXP_IDS
# SPECIES
source ${SCRIPT_DIR}/../test-data.env
# ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME
# ATLAS_DATA_GXA_VOL_NAME
# SOLR_CLOUD_CONTAINER_1_NAME
# SOLR_CLOUD_CONTAINER_2_NAME
source ${SCRIPT_DIR}/../../dev.env
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

function print_usage() {
  printf '\n%b\n' "Usage: ${0} [ -k DIRECTORY ] [ -o FILE ] [ -l FILE ]"
  printf '\n%b\n' "Populate a Docker Compose SolrCloud 8 cluster with Bulk Expression Atlas data."

  printf '\n%b\n' "-l FILE \tLog file (default is /dev/stdout)"
  printf '%b\n\n' "-h\t\tDisplay usage instructions"
}

SOLR_KEYS_DIRECTORY=${SCRIPT_DIR}
LOG_FILE=/dev/stdout
while getopts "l:h" opt
do
  case ${opt} in
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

IMAGE_NAME=gxa-solr-indexer
print_stage_name "🚧 Build Docker image ${IMAGE_NAME}"
docker build \
-t ${IMAGE_NAME} ${SCRIPT_DIR} >> ${LOG_FILE} 2>&1
print_done

print_stage_name "🌅 Start Solr 8 cluster in Docker Compose"
docker-compose \
--env-file ${SCRIPT_DIR}/../../dev.env \
-f ${SCRIPT_DIR}/../../docker-compose-solrcloud.yml \
up -d >> ${LOG_FILE} 2>&1
print_done

print_stage_name "💤 Give Solr ten seconds to start up before copying ontology file..."
sleep 10
print_done

print_stage_name "⚙ Spin up Postgres and Solr indexer containers to index volume data in Solr"
docker-compose --env-file ${SCRIPT_DIR}/../../dev.env \
-f ${SCRIPT_DIR}/../../docker-compose-postgres.yml \
up -d >> ${LOG_FILE} 2>&1
print_done

GRADLE_RO_DEP_CACHE_DEST=/gradle-ro-dep-cache
docker run --rm -it \
--env-file ${SCRIPT_DIR}/../../dev.env \
-v ${ATLAS_DATA_BIOENTITY_PROPERTIES_VOL_NAME}:/atlas-data/bioentity_properties:ro \
-v ${ATLAS_DATA_GXA_VOL_NAME}:/atlas-data/gxa:ro \
-v ${GRADLE_RO_DEP_CACHE_VOL_NAME}:${GRADLE_RO_DEP_CACHE_DEST}:ro \
-v ${ATLAS_DATA_GXA_EXPDESIGN_VOL_NAME}:/atlas-data/gxa-expdesign:ro \
-e GRADLE_RO_DEP_CACHE=${GRADLE_RO_DEP_CACHE_DEST} \
-e SPECIES="${SPECIES}" \
-e EXP_IDS="${EXP_IDS}" \
-e SOLR_HOST=${SOLR_CLOUD_CONTAINER_1_NAME}:8983 \
-e SOLR_NUM_SHARDS=2 \
-e NUM_DOCS_PER_BATCH=20000 \
-e SOLR_COLLECTION_BIOENTITIES=bioentities \
-e SOLR_COLLECTION_BIOENTITIES_SCHEMA_VERSION=1 \
-e SOLR_COLLECTION_BULK_ANALYTICS=bulk-analytics \
-e SOLR_COLLECTION_BULK_ANALYTICS_SCHEMA_VERSION=1 \
--network atlas-test-net \
${IMAGE_NAME} >> ${LOG_FILE} 2>&1
print_done

printf '%b\n' "🙂 All done! Point your browser at http://localhost:8983 to explore your SolrCloud instance."
