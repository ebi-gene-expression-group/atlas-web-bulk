package uk.ac.ebi.atlas.experimentpage.baseline.topgenes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.apache.solr.client.solrj.io.Tuple;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.search.bioentities.BioentitiesSearchDao;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Ordering.natural;
import static java.util.stream.Collectors.groupingBy;
import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_IDENTIFIER_DV;

// Get tuple streams from the DAO class and sorts them by specificity/average expression and return the gene IDs.
@Component
public class BaselineExperimentTopGenesService {
    public static final String GENE_KEY = "bioentity_identifier";
    public static final String AVERAGE_EXPRESSION_KEY = "avg_expression";
    public static final String SPECIFICITY_KEY = "specificity";

    private final BaselineExperimentTopGenesDao baselineExperimentTopGenesDao;
    private final BioentitiesSearchDao bioentitiesSearchDao;

    public BaselineExperimentTopGenesService(BaselineExperimentTopGenesDao baselineExperimentTopGenesDao,
                                             BioentitiesSearchDao bioentitiesSearchDao) {
        this.baselineExperimentTopGenesDao = baselineExperimentTopGenesDao;
        this.bioentitiesSearchDao = bioentitiesSearchDao;
    }

    public List<String> searchSpecificGenesInBaselineExperiment(String experimentAccession,
                                                                Species species,
                                                                BaselineRequestPreferences<?> preferences) {
        var candidateGeneIds =
                bioentitiesSearchDao.parseStringFieldFromMatchingDocs(
                        preferences.getGeneQuery(), species, BIOENTITY_IDENTIFIER_DV);

        try (var tupleStreamer =
                     baselineExperimentTopGenesDao.aggregateGeneIdsAndSortBySpecificity(
                             experimentAccession, preferences, candidateGeneIds)) {
            return mapBySpecificityAndSortByAverageExpression(tupleStreamer.get());
        }
    }

    public ImmutableList<String> searchMostExpressedGenesInBaselineExperiment(String experimentAccession,
                                                                              Species species,
                                                                              BaselineRequestPreferences<?> preferences) {
        var candidateGeneIds =
                bioentitiesSearchDao.parseStringFieldFromMatchingDocs(
                        preferences.getGeneQuery(), species, BIOENTITY_IDENTIFIER_DV);

        try (var tupleStreamer =
                     baselineExperimentTopGenesDao.aggregateGeneIdsAndSortByAverageExpression(
                             experimentAccession, preferences, candidateGeneIds)) {
            return tupleStreamer.get()
                    .map(tuple -> tuple.getString(GENE_KEY))
                    .collect(toImmutableList());
        }
    }

    private ImmutableList<String> mapBySpecificityAndSortByAverageExpression(Stream<Tuple> stream) {
        ToDoubleFunction<Tuple> avgExpressionExtractor = tuple -> tuple.getDouble(AVERAGE_EXPRESSION_KEY);
        Function<Tuple, String> geneIdExtractor = tuple -> tuple.getString(GENE_KEY);
        // I tried inlining the functions, but Java freaked out
        Comparator<Tuple> avgExpressionDescending =
                Comparator.comparingDouble(avgExpressionExtractor).reversed().thenComparing(geneIdExtractor);

        Multimap<Long, Tuple> mostExpressedGenesOnAverageGroupedBySpecificity =
                TreeMultimap.create(
                        natural(),
                        avgExpressionDescending);

        stream.collect(groupingBy(tuple -> tuple.getLong(SPECIFICITY_KEY)))
                .forEach(mostExpressedGenesOnAverageGroupedBySpecificity::putAll);

        return mostExpressedGenesOnAverageGroupedBySpecificity.values().stream()
                .map(tuple -> tuple.getString(GENE_KEY))
                .collect(toImmutableList());
    }
}
