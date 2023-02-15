FROM ubuntu:jammy

RUN apt -y -qq update
# Beware! Python 3 is required by index-bioentities but comes as part of Ubuntu
RUN apt -y -qq install git openjdk-11-jdk jq rsync curl

RUN mkdir -p /root/bioentity-properties-jsonl
RUN mkdir -p /root/experiments-jsonl

WORKDIR /root
RUN git clone --depth 1 https://github.com/ebi-gene-expression-group/index-bioentities.git
RUN git clone --recurse-submodules --depth 1 -b move_to_solr8_pg11 --single-branch https://github.com/ebi-gene-expression-group/atlas-web-bulk.git /root/atlas-web-bulk
RUN git clone --depth 1 https://github.com/ebi-gene-expression-group/solr-bulk.git

VOLUME /atlas-data/bioentity_properties /atlas-data/gxa /atlas-data/gxa-expdesign

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
