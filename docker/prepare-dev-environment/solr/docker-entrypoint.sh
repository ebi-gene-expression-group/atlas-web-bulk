#!/usr/bin/env bash

for _SPECIES in ${SPECIES}
do
  rsync -av --include=${_SPECIES}* --include=*/ --exclude=* /atlas-data/bioentity_properties /root
done

cd /root/atlas-web-bulk
./gradlew -PdataFilesLocation=/root \
-PexperimentFilesLocation=/atlas-data/gxa \
-PexperimentDesignLocation=/atlas-data/gxa-expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/postgres \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
:cli:bootRun --args="bioentities-json --output=/root/bioentity-properties-jsonl"

cd /root/index-bioentities/bin
./create-bioentities-collection.sh
./create-bioentities-schema.sh
./create-bioentities-suggesters.sh

export SOLR_COLLECTION=${SOLR_COLLECTION_BIOENTITIES}
export SCHEMA_VERSION=${SOLR_COLLECTION_BIOENTITIES_SCHEMA_VERSION}
for FILE in `ls /root/bioentity-properties-jsonl/*.jsonl`
do
  INPUT_JSONL=${FILE} ./solr-jsonl-chunk-loader.sh >> /dev/stdout 2>&1
done
./build-suggesters.sh
unset SOLR_COLLECTION
unset SCHEMA_VERSION

cd /root/atlas-web-bulk
./gradlew -PdataFilesLocation=/root \
-PexperimentFilesLocation=/atlas-data/gxa \
-PexperimentDesignLocation=/atlas-data/gxa-expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
:cli:bootRun --args="bulk-analytics-json --output=/root/experiments-jsonl -e $(echo ${EXP_IDS} | sed -e "s/ /,/g")"

cd /root/solr-bulk/bin
./create-bulk-analytics-collection.sh
./create-bulk-analytics-schema.sh

cd /root/index-bioentities/bin
export SOLR_COLLECTION=${SOLR_COLLECTION_BULK_ANALYTICS}
export SCHEMA_VERSION=${SOLR_COLLECTION_BULK_ANALYTICS_SCHEMA_VERSION}
export SOLR_PROCESSORS=dedupe
for FILE in `ls /root/experiments-jsonl/*.jsonl`
do
  INPUT_JSONL=${FILE} ./solr-jsonl-chunk-loader.sh >> /dev/stdout 2>&1
done
unset SOLR_COLLECTION
unset SCHEMA_VERSION
