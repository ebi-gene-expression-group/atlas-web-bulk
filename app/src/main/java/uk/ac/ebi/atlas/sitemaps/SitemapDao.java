package uk.ac.ebi.atlas.sitemaps;

import com.google.common.collect.ImmutableSet;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.FacetStreamBuilder;
import uk.ac.ebi.atlas.species.SpeciesProperties;
import uk.ac.ebi.atlas.species.SpeciesPropertiesTrader;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.BIOENTITY_IDENTIFIER;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.IS_PRIVATE;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.SPECIES;

@Component
public class SitemapDao {
    private final BulkAnalyticsCollectionProxy bulkAnalyticsCollectionProxy;
    private final SpeciesPropertiesTrader speciesPropertiesTrader;

    public SitemapDao(SolrCloudCollectionProxyFactory collectionProxyFactory,
                      SpeciesPropertiesTrader speciesPropertiesTrader) {
        bulkAnalyticsCollectionProxy = collectionProxyFactory.create(BulkAnalyticsCollectionProxy.class);
        this.speciesPropertiesTrader = speciesPropertiesTrader;
    }

    @Cacheable("publicBioentityIdentifiers")
    public ImmutableSet<String> getBioentityIdentifiersInPublicExperiments(String species) {
        var solrQuery =
                new SolrQueryBuilder<BulkAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(IS_PRIVATE, "false")
                        .addQueryFieldByTerm(SPECIES, speciesPropertiesTrader.get(species).referenceName())
                        .build();

        try (var tupleStreamer = TupleStreamer.of(
                new FacetStreamBuilder<>(bulkAnalyticsCollectionProxy, BIOENTITY_IDENTIFIER)
                        .withQuery(solrQuery)
                        .withCounts()
                        .sortByCountsDescending()
                        .build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(BIOENTITY_IDENTIFIER.name()))
                    .collect(toImmutableSet());
        }
    }

    @Cacheable("publicSpecies")
    public ImmutableSet<String> getSpeciesInPublicExperiments() {
        var solrQuery =
                new SolrQueryBuilder<BulkAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(IS_PRIVATE, "false")
                        .build();

        try(var tupleStreamer = TupleStreamer.of(
                new FacetStreamBuilder<>(bulkAnalyticsCollectionProxy, SPECIES)
                        .withQuery(solrQuery)
                        .withCounts()
                        .sortByCountsDescending()
                        .build())) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(SPECIES.name()))
                    .map(speciesPropertiesTrader::get)
                    .map(SpeciesProperties::ensemblName)
                    .collect(toImmutableSet());
        }
    }
}
