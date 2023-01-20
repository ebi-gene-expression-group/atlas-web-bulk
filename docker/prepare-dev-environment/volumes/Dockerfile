FROM ubuntu:jammy

RUN apt -y -qq update
RUN apt -y -qq install lftp

VOLUME /atlas-data/bioentity_properties /atlas-data/gxa /atlas-data/gxa-expdesign /webapp-properties
RUN mkdir -p /atlas-data/gxa/magetab

COPY atlas-data/gxa/* /atlas-data/gxa/
COPY webapp-properties/* /webapp-properties/

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
