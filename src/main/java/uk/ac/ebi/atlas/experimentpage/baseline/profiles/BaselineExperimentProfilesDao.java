package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExpression;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.solr.BioentityPropertyName;
import uk.ac.ebi.atlas.solr.cloud.SolrCloudCollectionProxyFactory;
import uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy;
import uk.ac.ebi.atlas.solr.cloud.search.SolrQueryBuilder;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.groupingBy;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.ASSAY_GROUP_ID;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.BIOENTITY_IDENTIFIER;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.EXPERIMENT_ACCESSION;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.asAnalyticsSchemaField;

@Component
public class BaselineExperimentProfilesDao {
    private final BulkAnalyticsCollectionProxy bulkAnalyticsCollectionProxy;

    public BaselineExperimentProfilesDao(SolrCloudCollectionProxyFactory collectionProxyFactory) {
        bulkAnalyticsCollectionProxy = collectionProxyFactory.create(BulkAnalyticsCollectionProxy.class);
    }

    public GeneProfilesList<BaselineProfile> fetchProfiles(List<String> matchingGeneIdsInExperiment,
                                                           List<AssayGroup> assayGroups,
                                                           BaselineRequestPreferences<?> preferences,
                                                           String experimentAccession) {
        // An upper bound of the maximum number of Solr docs we can find: rows multiplied by all or a subset of columns
        var maximumNumberOfDocs = Math.min(matchingGeneIdsInExperiment.size(), preferences.getHeatmapMatrixSize()) *
                (preferences.getSelectedColumnIds().isEmpty() ?
                        assayGroups.size() :
                        preferences.getSelectedColumnIds().size());

        var expressionLevelFieldNames = BulkAnalyticsCollectionProxy.getExpressionLevelFieldNames(preferences.getUnit());
        var solrQueryBuilder =
                new SolrQueryBuilder<BulkAnalyticsCollectionProxy>()
                        .addFilterFieldByTerm(EXPERIMENT_ACCESSION, experimentAccession)
                        .addFilterFieldByRangeMin(expressionLevelFieldNames.getLeft(), preferences.getCutoff())
                        .addQueryFieldByTerm(BIOENTITY_IDENTIFIER, matchingGeneIdsInExperiment)
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
        var queryResponse = bulkAnalyticsCollectionProxy.query(solrQueryBuilder);

        var resultsMap =
                queryResponse.getResults().stream()
                        .collect(groupingBy(
                                solrDocument -> (String) solrDocument.getFieldValue(BIOENTITY_IDENTIFIER.name())));

        var baselineProfiles = new GeneProfilesList<BaselineProfile>();
        baselineProfiles.setTotalResultCount(matchingGeneIdsInExperiment.size());

        // matchingGeneIdsInExperiment contains gene IDs sorted by relevance, that’s why we use it for iteration
        matchingGeneIdsInExperiment.stream()
                .filter(resultsMap::containsKey)
                .limit(preferences.getHeatmapMatrixSize())
                .forEach(geneId -> {
                    var thisGeneIdDocs = resultsMap.get(geneId);
                    var geneName =
                            thisGeneIdDocs.get(0).containsKey("keyword_symbol") ?
                                    (String) thisGeneIdDocs.get(0).getFirstValue("keyword_symbol") :
                                    geneId;
                    var baselineProfile = new BaselineProfile(geneId, geneName);

                    thisGeneIdDocs.forEach(solrDoc -> {
                        var baselineExpression =
                                solrDoc.containsKey(expressionLevelFieldNames.getRight().name()) ?
                                        parseSolrFieldValue(
                                                solrDoc.getFieldValues(expressionLevelFieldNames.getRight().name())) :
                                        parseSolrFieldValue(
                                                solrDoc.getFieldValue(expressionLevelFieldNames.getLeft().name()));

                        var assayGroupId = (String) solrDoc.getFieldValue(ASSAY_GROUP_ID.name());
                        var thisAssayGroup = assayGroups.stream()
                                .filter(assayGroup -> assayGroup.getId().equals(assayGroupId))
                                .findFirst()
                                .orElseThrow(IllegalArgumentException::new);

                        baselineProfile.add(thisAssayGroup, baselineExpression);
                    });

                    baselineProfiles.add(baselineProfile);
                });

        return baselineProfiles;
    }

    private BaselineExpression parseSolrFieldValue(Collection<Object> values) {
        var quartiles =
                values.stream()
                        .mapToDouble(obj -> (Double) obj)
                        .sorted()
                        .boxed()
                        .collect(toImmutableList());

        return new BaselineExpression(quartiles.get(0),
                quartiles.get(1),
                quartiles.get(2),
                quartiles.get(3),
                quartiles.get(4));
    }

    private BaselineExpression parseSolrFieldValue(Object value) {
        return new BaselineExpression((double) value);
    }
}
