FROM ubuntu:jammy

RUN apt -y -qq update
RUN apt -y -qq install wget gnupg lsb-core
# https://www.postgresql.org/download/linux/ubuntu/
RUN sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
RUN wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc > /etc/apt/trusted.gpg.d/apt.postgresql.org.asc
RUN apt -y -qq update
RUN DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt -y -qq install openjdk-11-jdk git sed gawk jq r-base r-cran-optparse r-cran-tidyr r-cran-data.table nodejs postgresql-client-11

VOLUME /atlas-data/gxa /atlas-data/gxa-expdesign

WORKDIR /root
RUN git clone --recurse-submodules --depth 1 -b move_to_solr8_pg11 --single-branch https://github.com/ebi-gene-expression-group/atlas-web-bulk.git
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
