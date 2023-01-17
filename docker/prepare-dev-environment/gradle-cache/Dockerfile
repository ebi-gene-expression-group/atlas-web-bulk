FROM ubuntu:jammy

RUN apt -y update
RUN apt -y install git openjdk-11-jdk rsync

RUN git clone --recurse-submodules --depth 1 https://github.com/ebi-gene-expression-group/atlas-web-bulk.git /root/atlas-web-bulk
WORKDIR /root/atlas-web-bulk

VOLUME /gradle-wrapper-dists
VOLUME /gradle-ro-dep-cache
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
