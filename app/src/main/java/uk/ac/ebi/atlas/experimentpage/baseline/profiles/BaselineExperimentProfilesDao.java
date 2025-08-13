package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExpression;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.AnalyticsSchemaField;
import uk.ac.ebi.atlas.solr.cloud.fullanalytics.ExperimentRequestPreferencesSolrQueryFactory;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.ASSAY_GROUP_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.BIOENTITY_IDENTIFIER;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.BIOENTITY_IDENTIFIER_SEARCH;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.asAnalyticsSchemaField;

/**
 * DAO for fetching baseline expression profiles from the Solr analytics collection.
 * This is used when the 'specific' field in BaselineRequestPreferences is false.
 */
@Component
public class BaselineExperimentProfilesDao {
    private final BulkAnalyticsCollectionProxy bulkAnalyticsCollectionProxy;

    public BaselineExperimentProfilesDao(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        bulkAnalyticsCollectionProxy = collectionProxyFactory.create(BulkAnalyticsCollectionProxy.class);
    }

    /**
     * Fetches the count of distinct genes matching the criteria in a baseline experiment.
     *
     * @param experimentAccession The experiment accession
     * @param preferences The request preferences containing filtering criteria
     * @return The count of distinct genes matching the criteria
     */
    public long fetchCount(String experimentAccession, BaselineRequestPreferences<?> preferences) {
        SolrQuery solrQuery =
                ExperimentRequestPreferencesSolrQueryFactory.createSolrQuery(experimentAccession, preferences);

        return bulkAnalyticsCollectionProxy.fieldStats(BIOENTITY_IDENTIFIER, solrQuery).getCountDistinct();
    }

    /**
     * Fetches profiles for specific genes from the Solr analytics collection.
     *
     * @param geneIds The list of gene IDs to fetch
     * @param assayGroups The list of assay groups
     * @param preferences The request preferences containing filtering criteria
     * @param experimentAccession The experiment accession
     * @return A list of baseline profiles for the specified genes
     */
    public GeneProfilesList<BaselineProfile> fetchProfiles(List<String> geneIds,
                                                           List<AssayGroup> assayGroups,
                                                           BaselineRequestPreferences<?> preferences,
                                                           String experimentAccession) {
        int maximumNumberOfDocs = geneIds.size() *
                (preferences.getSelectedColumnIds().isEmpty() ?
                        assayGroups.size() :
                        preferences.getSelectedColumnIds().size());

        Pair<AnalyticsSchemaField, AnalyticsSchemaField> expressionLevelFieldNames =
                BulkAnalyticsCollectionProxy.getExpressionLevelFieldNames(preferences.getUnit());

        SolrQueryBuilder<BulkAnalyticsCollectionProxy> solrQueryBuilder = new SolrQueryBuilder<>();
        solrQueryBuilder.addFilterFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                .addFilterFieldByRangeMin(expressionLevelFieldNames.getLeft(), preferences.getCutoff())
                .addQueryFieldByTerm(BIOENTITY_IDENTIFIER_SEARCH, geneIds)
                .setFieldList(
                        ImmutableSet.of(
                            BIOENTITY_IDENTIFIER,
                            expressionLevelFieldNames.getLeft(),
                            expressionLevelFieldNames.getRight(),
                            ASSAY_GROUP_ID,
                            asAnalyticsSchemaField(BioentityPropertyName.SYMBOL)))
                .setRows(maximumNumberOfDocs);
        if (!preferences.getSelectedColumnIds().isEmpty()) {
            solrQueryBuilder.addQueryFieldByTerm(ASSAY_GROUP_ID, preferences.getSelectedColumnIds());
        }
        QueryResponse queryResponse = bulkAnalyticsCollectionProxy.query(solrQueryBuilder);

        Map<String, List<SolrDocument>> resultsMap =
                queryResponse.getResults().stream()
                        .collect(
                                groupingBy(solrDocument ->
                                        (String) solrDocument.getFieldValue(BIOENTITY_IDENTIFIER.name())));

        GeneProfilesList<BaselineProfile> baselineProfiles = new GeneProfilesList<>();

        for (String geneId: geneIds) {
            List<SolrDocument> thisGeneIdDocs = resultsMap.get(geneId);
            String geneName =
                    thisGeneIdDocs.get(0).containsKey("keyword_symbol") ?
                            (String) thisGeneIdDocs.get(0).getFirstValue("keyword_symbol") :
                            geneId;
            BaselineProfile bp = new BaselineProfile(geneId, geneName);

            thisGeneIdDocs.forEach(solrDoc -> {
                BaselineExpression baselineExpression =
                        solrDoc.containsKey(expressionLevelFieldNames.getRight().name()) ?
                                parseSolrFieldValue(
                                        solrDoc.getFieldValues(expressionLevelFieldNames.getRight().name())) :
                                parseSolrFieldValue(
                                        solrDoc.getFieldValue(expressionLevelFieldNames.getLeft().name()));

                String assayGroupId = (String) solrDoc.getFieldValue(ASSAY_GROUP_ID.name());
                AssayGroup thisAssayGroup = assayGroups.stream()
                        .filter(assayGroup -> assayGroup.getId().equals(assayGroupId))
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new);

                bp.add(thisAssayGroup, baselineExpression);
            });

            baselineProfiles.add(bp);
        }

        return baselineProfiles;
    }

    /**
     * Parses a collection of Solr field values into a BaselineExpression with quartiles.
     * This is used when the expression data includes quartile information.
     *
     * @param values The collection of values representing quartiles
     * @return A BaselineExpression containing the quartile data
     */
    private BaselineExpression parseSolrFieldValue(Collection<Object> values) {
        List<Double> quartiles =
                values.stream()
                        .mapToDouble(obj -> (Double) obj)
                        .sorted()
                        .boxed()
                        .collect(toList());

        return new BaselineExpression(quartiles.get(0),
                quartiles.get(1),
                quartiles.get(2),
                quartiles.get(3),
                quartiles.get(4));
    }

    /**
     * Parses a single Solr field value into a BaselineExpression.
     * This is used when the expression data is a single value without quartiles.
     *
     * @param value The expression value
     * @return A BaselineExpression containing the single value
     */
    private BaselineExpression parseSolrFieldValue(Object value) {
        return new BaselineExpression((double) value);
    }
}
