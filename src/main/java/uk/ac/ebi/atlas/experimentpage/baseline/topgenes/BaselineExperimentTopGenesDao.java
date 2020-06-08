package uk.ac.ebi.atlas.experimentpage.baseline.topgenes;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.TupleStreamer;
import uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.AnalyticsSchemaField;
import uk.ac.ebi.atlas.solr.cloud.fullanalytics.ExperimentRequestPreferencesSolrQueryFactory;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.TupleStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.InnerJoinStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.SelectStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.decorator.SortStreamBuilder;
import uk.ac.ebi.atlas.solr.cloud.search.streamingexpressions.source.FacetStreamBuilder;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

import static uk.ac.ebi.atlas.experimentpage.differential.topgenes.DifferentialExperimentTopGenesService.AVERAGE_EXPRESSION_KEY;
import static uk.ac.ebi.atlas.experimentpage.differential.topgenes.DifferentialExperimentTopGenesService.GENE_KEY;
import static uk.ac.ebi.atlas.experimentpage.differential.topgenes.DifferentialExperimentTopGenesService.SPECIFICITY_KEY;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.BIOENTITY_IDENTIFIER;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;

// Produces streams of Solr tuples which contain gene IDs that match the search criteria in the experiment page
// sidebar. They include average expression over selected assay groups and specificity (counts) if specific is checked.
@Component
public class BaselineExperimentTopGenesDao {
    private final BulkAnalyticsCollectionProxy bulkAnalyticsCollectionProxy;

    public BaselineExperimentTopGenesDao(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        bulkAnalyticsCollectionProxy = collectionProxyFactory.create(BulkAnalyticsCollectionProxy.class);
    }

    public TupleStreamer aggregateGeneIdsAndSortByAverageExpression(String experimentAccession,
                                                                    BaselineRequestPreferences<?> preferences,
                                                                    ImmutableCollection<String> geneIds) {
        var expressionLevelField =
                BulkAnalyticsCollectionProxy.getExpressionLevelFieldNames(preferences.getUnit()).getLeft();

        var solrQuery =
                ExperimentRequestPreferencesSolrQueryFactory.createSolrQuery(experimentAccession, preferences, geneIds);

        var facetStreamBuilder =
                new FacetStreamBuilder<>(bulkAnalyticsCollectionProxy, BIOENTITY_IDENTIFIER)
                        .withQuery(solrQuery)
                        .sortByAbsoluteAverageDescending(expressionLevelField);

        var selectStreamBuilder = mapMetricFieldNames(facetStreamBuilder, expressionLevelField);

        return TupleStreamer.of(selectStreamBuilder.build());
    }

    public TupleStreamer aggregateGeneIdsAndSortBySpecificity(String experimentAccession,
                                                              BaselineRequestPreferences<?> preferences,
                                                              ImmutableCollection<String> geneIds) {
        var expressionLevelField =
                BulkAnalyticsCollectionProxy.getExpressionLevelFieldNames(preferences.getUnit()).getLeft();

        // Get all experiment gene IDs, applying cutoff, with global specificity
        var experimentFilter =
                new SolrQueryBuilder<BulkAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addFilterFieldByRangeMin(expressionLevelField, preferences.getCutoff())
                        .build();

        var blahFacetStreamBuilder =
                new FacetStreamBuilder<>(bulkAnalyticsCollectionProxy, BIOENTITY_IDENTIFIER)
                        .withQuery(experimentFilter)
                        .withCounts()
                        .sortByAscending(BIOENTITY_IDENTIFIER);

        // Get experiment gene IDs expressed in the selected assay groups with average expression sorted by gene ID
        var solrQuery =
                ExperimentRequestPreferencesSolrQueryFactory.createSolrQuery(experimentAccession, preferences, geneIds);

        var facetStreamBuilder =
                new FacetStreamBuilder<>(bulkAnalyticsCollectionProxy, BIOENTITY_IDENTIFIER)
                        .withQuery(solrQuery)
                        .sortByAscending(BIOENTITY_IDENTIFIER)
                        .withAbsoluteAverageOf(expressionLevelField);

        // Join previous two streams, creating a stream of the gene IDs expressed in the selected assay group with the
        // average expression and the global specificity
        var innerJoinStreamBuilder =
                new InnerJoinStreamBuilder(facetStreamBuilder, blahFacetStreamBuilder, BIOENTITY_IDENTIFIER.name());

        var selectStreamBuilder = mapMetricFieldNames(innerJoinStreamBuilder, expressionLevelField);

        // Sort resulting stream by specificity
        var sortStreamBuilder = new SortStreamBuilder(selectStreamBuilder, SPECIFICITY_KEY);

        return TupleStreamer.of(sortStreamBuilder.build());
    }

    // Rename fields of the resulting stream. We do this to manipulate the resulting tuples regardless of the units or
    // experiment type (TPM, FPKM, log-2 fold change).
    private static SelectStreamBuilder mapMetricFieldNames(TupleStreamBuilder tupleStreamBuilder,
                                                           AnalyticsSchemaField expressionLevelField) {

        return new SelectStreamBuilder(tupleStreamBuilder)
                        .addFieldMapping(
                                ImmutableMap.of(
                                        BIOENTITY_IDENTIFIER.name(), GENE_KEY,
                                        "avg(abs(" + expressionLevelField.name() + "))", AVERAGE_EXPRESSION_KEY,
                                        "count(*)", SPECIFICITY_KEY));
    }
}
