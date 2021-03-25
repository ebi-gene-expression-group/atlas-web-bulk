package uk.ac.ebi.atlas.solr.analytics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.SemanticQuery;
import uk.ac.ebi.atlas.species.SpeciesFinder;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Service
public class AnalyticsSearchService implements SpeciesFinder {
    private final MiscellaneousAnalyticsSearchDao miscellaneousAnalyticsSearchDao;

    public AnalyticsSearchService(MiscellaneousAnalyticsSearchDao miscellaneousAnalyticsSearchDao) {
        this.miscellaneousAnalyticsSearchDao = miscellaneousAnalyticsSearchDao;
    }

    public ImmutableSet<String> fetchExperimentTypes(String bioentityIdentifier) {
        return fetchExperimentTypes(SemanticQuery.create(bioentityIdentifier), SemanticQuery.create(), "");
    }

    public static ImmutableSet<String> readBuckets(String response) {
        return JsonPath.<List<Map<String, Object>>>read(response, "$..buckets[*]").stream()
                .map(bucketMap -> bucketMap.get("val"))
                .map(Object::toString)
                .collect(toImmutableSet());
    }

    public ImmutableSet<String> fetchExperimentTypesInAnyField(SemanticQuery query) {
        String response = miscellaneousAnalyticsSearchDao.fetchExperimentTypesInAnyField(query);
        return readBuckets(response);
    }

    public ImmutableSet<String> fetchExperimentTypes(SemanticQuery geneQuery, String speciesReferenceName) {

        return fetchExperimentTypes(geneQuery, SemanticQuery.create(), speciesReferenceName);

    }

    public ImmutableSet<String> fetchExperimentTypes(SemanticQuery geneQuery,
                                                     SemanticQuery conditionQuery,
                                                     String speciesReferenceName) {

        String response =
                miscellaneousAnalyticsSearchDao.fetchExperimentTypes(geneQuery, conditionQuery, speciesReferenceName);

        return readBuckets(response);
    }

    public ImmutableSet<String> searchMoreThanOneBioentityIdentifier(SemanticQuery geneQuery,
                                                                     SemanticQuery conditionQuery,
                                                                     String speciesReferenceName) {

        String response =
                miscellaneousAnalyticsSearchDao.searchBioentityIdentifiers(
                        geneQuery, conditionQuery, speciesReferenceName, 2);

        return readBuckets(response);
    }

    public ImmutableSet<String> searchBioentityIdentifiers(SemanticQuery geneQuery,
                                                           SemanticQuery conditionQuery,
                                                           String speciesReferenceName) {

        String response =
                miscellaneousAnalyticsSearchDao.searchBioentityIdentifiers(
                        geneQuery, conditionQuery, speciesReferenceName, -1);
        return readBuckets(response);
    }

    public boolean tissueExpressionAvailableInBaselineExperiments(SemanticQuery geneQuery) {
        String response =
                miscellaneousAnalyticsSearchDao.searchBioentityIdentifiersForTissuesInBaselineExperiments(geneQuery);

        return !readBuckets(response).isEmpty();
    }

    @Override
    public ImmutableList<String> findSpecies(SemanticQuery geneQuery, SemanticQuery conditionQuery) {
        return readSpecies(miscellaneousAnalyticsSearchDao.getSpecies(geneQuery, conditionQuery));
    }

    private static ImmutableList<String> readSpecies(String response) {
        JSONArray speciesWithCounts = JsonPath.read(response, "$..species[*]");

        // The JSON array is a list of species in even positions followed by a doc count in odd positions
        ImmutableList.Builder<String> b = ImmutableList.builder();
        for (int i = 0; i < speciesWithCounts.size(); i += 2) {
            b.add(speciesWithCounts.get(i).toString());
        }
        return b.build();
    }
}
