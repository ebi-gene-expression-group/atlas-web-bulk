package uk.ac.ebi.atlas.bioentity.properties;

import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;

import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.BIOENTITY_IDENTIFIER_SEARCH;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.IS_PRIVATE;

@Service
public class ExpressedBioentityFinderImpl implements ExpressedBioentityFinder {
    private final BulkAnalyticsCollectionProxy bulkAnalyticsCollectionProxy;

    public ExpressedBioentityFinderImpl(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        this.bulkAnalyticsCollectionProxy = collectionProxyFactory.create(BulkAnalyticsCollectionProxy.class);
    }

    @Override
    public boolean bioentityIsExpressedInAtLeastOneExperiment(String bioentityIdentifier) {
        var solrQueryBuilder =
                new SolrQueryBuilder<BulkAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(IS_PRIVATE, "false")
                        .addQueryFieldByTerm(BIOENTITY_IDENTIFIER_SEARCH, bioentityIdentifier);

        return bulkAnalyticsCollectionProxy.query(solrQueryBuilder).getResults().size() > 0;
    }
}
