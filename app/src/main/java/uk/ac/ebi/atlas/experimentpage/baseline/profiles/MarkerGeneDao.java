package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import com.google.gson.JsonArray;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExpression;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DAO for fetching marker gene data from the gxa_marker_gene table in PostgreSQL.
 * This is used when the 'specific' field in BaselineRequestPreferences is true.
 */
@Component
public class MarkerGeneDao {

    private static final int MAX_NUMBER_OF_MARKER_GENES = 50;
    private final JdbcTemplate jdbcTemplate;

    private static final String FETCH_MARKER_GENES =
        "SELECT gene_id, gene_name, assay_id, expression_level " +
        "FROM gxa_marker_gene " +
        "WHERE experiment_accession = ? " +
            "AND assay_id IN (%s) " +
            "AND gene_id IN ( " +
                "SELECT DISTINCT gene_id " +
                "FROM gxa_marker_gene " +
                "WHERE experiment_accession = ? " +
                "AND assay_id IN (%s) " +
                "AND marker_gene_rank <= ?) " +
            "AND expression_unit = ? " +
            "AND expression_level >= ? " +
        "ORDER BY ARRAY_POSITION(ARRAY[%s]::text[], assay_id::text), expression_level DESC";

    private static final String COUNT_MARKER_GENES =
        "SELECT COUNT(DISTINCT gene_id) " +
            "FROM gxa_marker_gene " +
            "WHERE experiment_accession = ? " +
            "AND expression_unit = ? " +
            "AND expression_level >= ?";

    public MarkerGeneDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Fetches marker gene profiles from the database.
     * These are the most highly expressed genes for the experiment.
     *
     * @param experimentAccession The experiment accession
     * @param assayGroups The list of assay groups
     * @param preferences The request preferences containing filtering criteria
     * @return A list of baseline profiles for marker genes
     */
    public GeneProfilesList<BaselineProfile> fetchMarkerGeneProfiles(
        @NotNull String experimentAccession,
        @NotNull List<AssayGroup> assayGroups,
        @NotNull BaselineRequestPreferences<?> preferences,
        @NotNull JsonArray columnHeaders) {

        var markerGeneRankLimit = (double) (MAX_NUMBER_OF_MARKER_GENES / columnHeaders.size());
        if (markerGeneRankLimit < 1) {
            markerGeneRankLimit = 1;
        }

        final List<String> assayGroupIDs = getAssayGroupIDs(columnHeaders);

        var assayIDInClause = String.join(",", Collections.nCopies(assayGroupIDs.size(), "?"));

        var sql = String.format(FETCH_MARKER_GENES, assayIDInClause, assayIDInClause, assayIDInClause);

        List<Object> queryParams =  createQueryParams(experimentAccession, preferences, assayGroupIDs, markerGeneRankLimit);

        var results = executeQuery(sql, queryParams);

        return createGeneProfilesList(results, assayGroups);
    }

    /**
     * Fetches the count of distinct marker genes for an experiment.
     * This is used to determine the total number of results available.
     *
     * @param experimentAccession The experiment accession
     * @param preferences The request preferences containing filtering criteria
     * @return The count of distinct marker genes matching the criteria
     */
    public long fetchCount(
        @NotNull String experimentAccession,
        @NotNull BaselineRequestPreferences<?> preferences) {

        var queryParams = new Object[] {
            experimentAccession,
            preferences.getUnit().getDatabaseValue(),
            preferences.getCutoff()
        };

        return jdbcTemplate.queryForObject(COUNT_MARKER_GENES, Long.class, queryParams);
    }

    private static List<Object> createQueryParams(@NotNull String experimentAccession, @NotNull BaselineRequestPreferences<?> preferences,
                                                  List<String> assayNames, double markerGeneRankLimit) {
        List<Object> queryParams = new ArrayList<>();
        queryParams.add(experimentAccession);
        queryParams.addAll(assayNames);
        queryParams.add(experimentAccession);
        queryParams.addAll(assayNames);
        queryParams.add(markerGeneRankLimit);
        queryParams.add(preferences.getUnit().getDatabaseValue());
        queryParams.add(preferences.getCutoff());
        queryParams.addAll(assayNames);

        return queryParams;
    }

    private static List<String> getAssayGroupIDs(JsonArray columnHeaders) {
        return IntStream.range(0, columnHeaders.size())
            .mapToObj(i -> columnHeaders.get(i).getAsJsonObject())
            .filter(header -> header.has("assayGroupId") && !header.get("assayGroupId").isJsonNull())
            .map(header -> header.get("assayGroupId").getAsString())
            .filter(id -> !id.isEmpty())
            .collect(Collectors.toList());
    }


    /**
     * Executes a SQL query and returns the results.
     *
     * @param sql The SQL query to execute
     * @param params The parameters for the SQL query
     * @return The query results as a list of maps
     */
    private List<Map<String, Object>> executeQuery(String sql, List<?> params) {
        return jdbcTemplate.queryForList(sql, params.toArray(new Object[0]));
    }

    /**
     * Processes database query results into a GeneProfilesList.
     * This method transforms raw database rows into structured gene profiles with expression data.
     *
     * @param results Database query results containing gene expression data
     * @param assayGroups The list of assay groups for the experiment
     * @return A GeneProfilesList containing the processed gene profiles
     */
    private GeneProfilesList<BaselineProfile> createGeneProfilesList(
        List<Map<String, Object>> results,
        List<AssayGroup> assayGroups) {

        var profilesMap = new LinkedHashMap<String, BaselineProfile>();

        for (var row : results) {
            var geneId = (String) row.get("gene_id");
            var geneName = (String) row.get("gene_name");
            var assayGroupID = (String) row.get("assay_id");
            var expressionLevel = ((Number) row.get("expression_level")).doubleValue();

            var profile = profilesMap.computeIfAbsent(
                geneId, id -> new BaselineProfile(id, geneName));

            var assayGroup = findAssayGroupByAssayID(assayGroupID, assayGroups);
            if (assayGroupID != null) {
                profile.add(assayGroup, new BaselineExpression(expressionLevel));
            }
        }

        return new GeneProfilesList<>(profilesMap.values());
    }

    /**
     * Finds an AssayGroup by its ID from a list of assay groups.
     * 
     * This method searches through the provided list of AssayGroup objects
     * to find the one with an ID matching the specified assayGroupID.
     * This is used to map assay IDs from the database query results to their
     * corresponding AssayGroup objects needed for creating BaselineProfiles.
     *
     * @param assayGroupID The ID of the assay group to find, as retrieved from the database
     * @param assayGroups The list of assay groups to search within
     * @return The matching AssayGroup object, or null if no matching assay group is found
     *         or if the provided assayGroupID is null
     */
    private AssayGroup findAssayGroupByAssayID(String assayGroupID, List<AssayGroup> assayGroups) {
        if (assayGroupID != null) {
            for (AssayGroup assayGroup : assayGroups) {
                if (assayGroup.getId().equals(assayGroupID)) {
                    return assayGroup;
                }
            }
        }

        return null;
    }
}