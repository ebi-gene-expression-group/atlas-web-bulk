#!/usr/bin/env bash

cd /root/atlas-web-bulk

./gradlew \
-PexperimentFilesLocation=/atlas-data/gxa \
-PexperimentDesignLocation=/atlas-data/gxa-expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
:cli:bootRun --args="create-update-experiment -e $(echo ${EXP_IDS} | sed -e "s/ /,/g")"

./gradlew \
-PexperimentFilesLocation=/atlas-data/gxa \
-PexperimentDesignLocation=/atlas-data/gxa-expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
:cli:bootRun --args="update-baseline-coexpression -e $(echo ${EXP_IDS} | sed -e "s/ /,/g")"

cd /root/db-scxa/bin
export PATH=.:${PATH}
export dbConnection=postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@${POSTGRES_HOST}:5432/${POSTGRES_DB}

for EXP_ID in ${EXP_IDS}
do
  EXP_ID=${EXP_ID} \
  CONDENSED_SDRF_FILE=/atlas-data/gxa/magetab/${EXP_ID}/${EXP_ID}.condensed-sdrf.tsv \
  SDRF_FILE=/atlas-data/gxa/magetab/${EXP_ID}/${EXP_ID}.sdrf.txt \
  ./load_exp_design.sh
done