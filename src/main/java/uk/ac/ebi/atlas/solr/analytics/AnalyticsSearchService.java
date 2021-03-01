package uk.ac.ebi.atlas.solr.analytics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.search.SemanticQuery;
import uk.ac.ebi.atlas.species.SpeciesFinder;

import java.util.List;
import java.util.Map;

@Component
public class AnalyticsSearchService implements SpeciesFinder {
    private final MiscellaneousAnalyticsSearchDao miscellaneousAnalyticsSearchDao;

    public AnalyticsSearchService(MiscellaneousAnalyticsSearchDao miscellaneousAnalyticsSearchDao) {
        this.miscellaneousAnalyticsSearchDao = miscellaneousAnalyticsSearchDao;
    }

    public ImmutableSet<String> fetchExperimentTypes(String bioentityIdentifier) {
        return fetchExperimentTypes(SemanticQuery.create(bioentityIdentifier), SemanticQuery.create(), "");
    }

    public static ImmutableSet<String> readBuckets(String response) {
        List<Map<String, Object>> res = JsonPath.read(response, "$..buckets[*]");

        ImmutableSet.Builder<String> b = ImmutableSet.builder();
        for (Map<String, Object> m: res) {
            b.add(m.get("val").toString());
        }
        return b.build();
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

    public boolean tissueExpressionAvailableFor(SemanticQuery geneQuery) {
        String response =
                miscellaneousAnalyticsSearchDao.searchBioentityIdentifiersForTissuesInBaselineExperiments(geneQuery);

        return !readBuckets(response).isEmpty();
    }

    @Override
    public ImmutableList<String> findSpecies(SemanticQuery geneQuery, SemanticQuery conditionQuery) {
        return readSpecies(miscellaneousAnalyticsSearchDao.getSpecies(geneQuery, conditionQuery));
    }

    private static ImmutableList<String> readSpecies(String response) {
        JSONArray res = JsonPath.read(response, "$..species[*]");

        ImmutableList.Builder<String> b = ImmutableList.builder();
        for (int i = 0; i < res.size(); i += 2) {
            b.add(res.get(i).toString());
        }
        return b.build();
    }
}
