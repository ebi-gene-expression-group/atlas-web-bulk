#!/usr/bin/env bash
set -e

require_env_var() {
  if [[ -z ${!1} ]]
  then
    echo "$1 env var is needed." && exit 1
  fi
}

require_env_var INPUT_JSONL
require_env_var SOLR_COLLECTION
require_env_var SCHEMA_VERSION

COLLECTION=${SOLR_COLLECTION}-v${SCHEMA_VERSION}
HOST=${SOLR_HOST:-'localhost:8983'}
# SOLR_PROCESSORS must be null or a comma-separated list of processors to use during an update
if [[ $SOLR_PROCESSORS ]]
then
  PROCESSOR="?processor=${SOLR_PROCESSORS}"
fi

# Creates a new file descriptor 3 that redirects to 1 (STDOUT) to see curl progress and also to cleanly capture http_code, see commit and post_json below
exec 3>&1

commit() {
  echo "Committing files..."
  HTTP_STATUS=$(curl -o >(cat >&3) -w "%{http_code}" "http://${HOST}/solr/${COLLECTION}/update" --data-binary '{ "commit": {} }' -H 'Content-type:application/json')

  if [[ ! ${HTTP_STATUS} == 2* ]]
  then
    echo "Error during commit!" && exit 1
  fi
}

post_json() {
  # Run curl in a separate command, capturing output of -w "%{http_code}" into HTTP_STATUS
  # and sending the content to this command's STDOUT with -o >(cat >&3)

  # The update/json/docs handler supports both regular JSON and JSON Lines:
  # https://solr.apache.org/guide/7_1/transforming-and-indexing-custom-json.html#multiple-documents-in-a-single-payload
  local HTTP_STATUS=$(curl -o >(cat >&3) -w "%{http_code}" "http://${HOST}/solr/${COLLECTION}/update/json/docs$PROCESSOR" --data-binary "@${1}" -H 'Content-type:application/json')

  if [[ ! ${HTTP_STATUS} == 2* ]]
  then
    echo "Error during update!" && exit 1
  fi
}


COMMIT_DOCS=${SOLR_COMMIT_DOCS:-1000000}
echo "Loading $INPUT_JSONL into host $HOST collection $COLLECTION committing every ${COMMIT_DOCS} docs..."

CHUNK_PREFIX=${CHUNK_PREFIX:-`basename -s .jsonl ${INPUT_JSONL}`-}

NUM_DOCS_PER_BATCH=${NUM_DOCS_PER_BATCH:-50000}
split -a 3 -l $NUM_DOCS_PER_BATCH $INPUT_JSONL $CHUNK_PREFIX --additional-suffix .jsonl
CHUNK_FILES=$(ls $CHUNK_PREFIX*)

cleanup() {
  exec 3>&-
  rm ${CHUNK_FILES}
}


trap cleanup exit
I=O
for CHUNK_FILE in $CHUNK_FILES
do
  I=$(( $I + 1 ))

  echo "$CHUNK_FILE ${I}/$(wc -w <<< $CHUNK_FILES)"

  post_json ${CHUNK_FILE}

  if [[ $(( $I % ( $COMMIT_DOCS / $NUM_DOCS_PER_BATCH) )) == 0 ]]
  then
    commit
  fi
done
commit

