FROM alpine:3.12

RUN apk --no-cache add bash curl git grep

RUN git clone https://github.com/ebi-gene-expression-group/index-gxa.git && \
    git clone https://github.com/ebi-gene-expression-group/solr-bulk.git

COPY gxa-solrcloud-bootstrap-entrypoint.sh /entrypoint.sh
RUN chmod a+x /entrypoint.sh

ENV SOLR_COLLECTIONS="bioentities-v1 bulk-analytics-v1"
ENV SOLR_HOSTS="gxa-solrcloud-1:8983 gxa-solrcloud-2:8983"
ENV SOLR_HOST="gxa-solrcloud-1:8983"
ENV SOLR_NUM_SHARDS=2

ENTRYPOINT ["/entrypoint.sh"]
