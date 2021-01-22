#!/usr/bin/env bash

/index-gxa/bin/create-bioentities-collection.sh
/index-gxa/bin/create-bioentities-schema.sh

/index-gxa/bin/create-gxa-analytics-config-set.sh
/index-gxa/bin/create-gxa-analytics-collection.sh
/index-gxa/bin/create-gxa-analytics-schema.sh

# Restore all collections
# Env variables come from Dockerfile
for SOLR_HOST in $SOLR_HOSTS
do
  for SOLR_COLLECTION in $SOLR_COLLECTIONS
  do
    curl "http://${SOLR_HOST}/solr/${SOLR_COLLECTION}/replication?command=restore&location=/var/backups/solr&name=${SOLR_COLLECTION}"

    # Pattern enclosed in (?<=) is zero-width look-behind and (?=) is zero-width look-ahead, we match everything in between
    STATUS=`curl -s "http://${SOLR_HOST}/solr/${SOLR_COLLECTION}/replication?command=restorestatus" | grep -oP '(?<="status":").*(?=")'`

    # We wait until "status" field is "success"
    while [ "${STATUS}" != "success" ]
    do
      sleep 1s
      STATUS=`curl -s "http://${SOLR_HOST}/solr/${SOLR_COLLECTION}/replication?command=restorestatus" | grep -oP '(?<="status":").*(?=")'`
    done
  done
done
