package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExpression;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkerGeneDao.class);
    private static final int MAX_NUMBER_OF_MARKER_GENES = 50;
    private final JdbcTemplate jdbcTemplate;

    private static final String FETCH_MARKER_GENES =
        "WITH filtered_data AS ( " +
            "SELECT gene_id, gene_name, assay, expression_level, marker_gene_rank " +
            "FROM gxa_marker_gene " +
            "WHERE experiment_accession = ?" +
            "  AND assay IN (%s)" +
            "  AND expression_unit = ?" +
            "  AND expression_level >= ?" +
            "  AND marker_gene_rank IS NOT NULL" +
            "), " +
            "ranked_genes AS (" +
            "  SELECT DISTINCT gene_id" +
            "  FROM filtered_data" +
            "  WHERE marker_gene_rank <= ? " +
            ") " +
        "SELECT gene_id, gene_name, assay, expression_level " +
        "FROM filtered_data " +
        "WHERE gene_id IN (SELECT gene_id FROM ranked_genes) " +
        "ORDER BY ARRAY_POSITION(ARRAY[%s]::text[], assay::text), expression_level DESC";

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
        @NotNull JsonArray columnHeaders,
        @NotNull String assayParamName) {

        var markerGeneRankLimit = (double) (MAX_NUMBER_OF_MARKER_GENES / columnHeaders.size());
        if (markerGeneRankLimit < 1) {
            markerGeneRankLimit = 1;
        }

        final List<String> assayNames = getAssayNames(columnHeaders, assayParamName);

        var assayInClause = String.join(",", Collections.nCopies(assayNames.size(), "?"));

        var sql = String.format(FETCH_MARKER_GENES, assayInClause, assayInClause);

        List<Object> queryParams =  createQueryParams(experimentAccession, preferences, assayNames, markerGeneRankLimit);

        var results = executeQuery(sql, queryParams);

        return createGeneProfilesList(results, assayGroups, columnHeaders);
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
        queryParams.add(preferences.getUnit().getDatabaseValue());
        queryParams.add(preferences.getCutoff());
        queryParams.add(markerGeneRankLimit);
        queryParams.addAll(assayNames);

        return queryParams;
    }
    private static List<String> getAssayNames(JsonArray columnHeaders, String assayParamName) {
        return IntStream.range(0, columnHeaders.size())
            .mapToObj(i -> columnHeaders.get(i).getAsJsonObject())
            .filter(header -> header.has(assayParamName) && !header.get(assayParamName).isJsonNull())
            .map(header -> header.get(assayParamName).getAsString())
            .filter(name -> !name.isEmpty())
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
        List<AssayGroup> assayGroups,
        JsonArray columnHeaders) {

        var profilesMap = new LinkedHashMap<String, BaselineProfile>();

        for (var row : results) {
            var geneId = (String) row.get("gene_id");
            var geneName = (String) row.get("gene_name");
            var assayName = (String) row.get("assay");
            var expressionLevel = ((Number) row.get("expression_level")).doubleValue();

            var profile = profilesMap.computeIfAbsent(
                geneId, id -> new BaselineProfile(id, geneName));

            var assayGroup = findAssayGroupByAssayName(assayName, assayGroups, columnHeaders);
            if (assayGroup != null) {
                profile.add(assayGroup, new BaselineExpression(expressionLevel));
            }
        }

        return new GeneProfilesList<>(profilesMap.values());
    }

    /**
     * Finds an assay group that contains an assay with the given name.
     * This is used to map assay names from the database to assay groups.
     *
     * The assay name in the database is not the same as the assay ID in the assay group.
     * We need to try different approaches to find the correct assay group.
     *
     * @param assayName The assay name to look for (from the database)
     * @param assayGroups The list of assay groups to search in
     * @param columnHeaders The column headers containing the mapping between assay names and assay group IDs
     * @return The assay group that contains an assay with the given name, or null if not found
     */
    private AssayGroup findAssayGroupByAssayName(String assayName, List<AssayGroup> assayGroups, JsonArray columnHeaders) {
        // First, try to find the assay group ID from the column headers
        String assayGroupId = null;

        // Iterate through column headers to find a match between factorValue and assayName
        for (int i = 0; i < columnHeaders.size(); i++) {
            var header = columnHeaders.get(i).getAsJsonObject();
            var assayNameFromHeader = getFirstNonEmptyValue(header, "factorValue", "name");

            // Check if the factor assayNameFromHeader matches the assay name
            if (assayNameFromHeader.equals(assayName)) {
                assayGroupId = getFirstNonEmptyValue(header, "assayGroupId", "id");
                LOGGER.debug("Found assay group ID '{}' for assay name '{}'", assayGroupId, assayName);
                break;
            }
        }

        // If we found an assay group ID, find the corresponding assay group
        if (assayGroupId != null) {
            for (AssayGroup assayGroup : assayGroups) {
                if (assayGroup.getId().equals(assayGroupId)) {
                    return assayGroup;
                }
            }
        }

        // If we couldn't find a match, log a warning and return null
        LOGGER.warn("Could not find assay group for assay name '{}' in column headers", assayName);
        return null;
    }

    /**
     * Safely extracts a string value from a JsonObject by trying multiple keys in order.
     * Returns the first non-empty value found, or an empty string if none of the keys exist
     * or all values are null/empty.
     *
     * @param jsonObject The JsonObject to extract from
     * @param keys The ordered list of keys to try
     * @return The first non-empty string value found, or empty string if none found
     */
    private String getFirstNonEmptyValue(JsonObject jsonObject, String... keys) {
        if (jsonObject == null || keys == null) {
            return "";
        }

        for (String key : keys) {
            if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
                try {
                    String value = jsonObject.get(key).getAsString();
                    if (!value.isEmpty()) {
                        return value;
                    }
                } catch (IllegalStateException e) {
                    return "";
                }
            }
        }

        return "";
    }
}