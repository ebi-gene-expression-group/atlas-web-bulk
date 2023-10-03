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