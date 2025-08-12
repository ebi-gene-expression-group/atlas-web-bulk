package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExpression;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DAO for fetching marker gene data from the gxa_marker_gene table in PostgreSQL.
 * This is used when the 'specific' field in BaselineRequestPreferences is true.
 */
@Component
public class MarkerGeneDao {
    private final JdbcTemplate jdbcTemplate;

    // SQL query constants
    private static final String BASE_QUERY =
        "SELECT gene_id, gene_name, assay, expression_level " +
            "FROM gxa_marker_gene " +
            "WHERE experiment_accession = ? ";

    private static final String FETCH_MARKER_GENES = BASE_QUERY +
        "AND expression_unit = ? " +
        "AND expression_level >= ? " +
        "ORDER BY marker_gene_rank " +
        "LIMIT ?";

    private static final String FETCH_SPECIFIC_GENES = BASE_QUERY +
        "AND gene_id IN (%s) " +
        "AND expression_unit = ? " +
        "AND expression_level >= ?";

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
        @NotNull BaselineRequestPreferences<?> preferences) {

        var queryParams = new Object[] {
            experimentAccession,
            preferences.getUnit().getDatabaseValue(),
            preferences.getCutoff(),
            preferences.getHeatmapMatrixSize()
        };

        var results = executeQuery(FETCH_MARKER_GENES, queryParams);

        var geneProfilesList = createGeneProfilesList(results, assayGroups);
        geneProfilesList.setTotalResultCount(fetchCount(experimentAccession, preferences));

        return geneProfilesList;
    }

    /**
     * Fetches profiles for specific genes from the database.
     * This method is used when querying for a predefined list of gene IDs.
     *
     * @param geneIds The list of gene IDs to fetch
     * @param experimentAccession The experiment accession
     * @param assayGroups The list of assay groups
     * @param preferences The request preferences containing filtering criteria
     * @return A list of baseline profiles for the specified genes
     */
    public GeneProfilesList<BaselineProfile> fetchSpecificGeneProfiles(
        @NotNull List<String> geneIds,
        @NotNull String experimentAccession,
        @NotNull List<AssayGroup> assayGroups,
        @NotNull BaselineRequestPreferences<?> preferences) {

        if (geneIds.isEmpty()) {
            return new GeneProfilesList<>();
        }

        var sql = buildSpecificGenesQuery(geneIds);
        var params = buildSpecificGenesParams(geneIds, experimentAccession, preferences);

        var results = executeQuery(sql, params.toArray());
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

    /**
     * Executes a SQL query and returns the results.
     *
     * @param sql The SQL query to execute
     * @param params The parameters for the SQL query
     * @return The query results as a list of maps
     */
    private List<Map<String, Object>> executeQuery(String sql, Object[] params) {
        return jdbcTemplate.queryForList(sql, params);
    }

    /**
     * Builds SQL query for specific genes with the appropriate placeholders.
     *
     * @param geneIds List of gene IDs to query for
     * @return SQL query string with placeholders for the gene IDs
     */
    private String buildSpecificGenesQuery(List<String> geneIds) {
        var placeholders = geneIds.stream()
            .map(id -> "?")
            .collect(Collectors.joining(","));
        return String.format(FETCH_SPECIFIC_GENES, placeholders);
    }

    /**
     * Builds parameters for the specific genes query.
     *
     * @param geneIds List of gene IDs to query for
     * @param experimentAccession The experiment accession
     * @param preferences The request preferences
     * @return List of parameters for the query
     */
    private List<Object> buildSpecificGenesParams(
        List<String> geneIds,
        String experimentAccession,
        BaselineRequestPreferences<?> preferences) {

        var params = new ArrayList<>(geneIds.size() + 3);
        params.add(experimentAccession);
        params.addAll(geneIds);
        params.add(preferences.getUnit().getDatabaseValue());
        params.add(preferences.getCutoff());

        return params;
    }

    /**
     * Creates a lookup map of assay IDs to their parent assay groups.
     * This is used to efficiently find the assay group for a given assay ID.
     *
     * @param assayGroups The list of assay groups
     * @return A map where keys are assay IDs and values are their parent assay groups
     */
    private Map<String, AssayGroup> createAssayGroupMap(List<AssayGroup> assayGroups) {
        var assayGroupMap = new HashMap<String, AssayGroup>();
        for (var assayGroup : assayGroups) {
            for (var assayId : assayGroup.getAssayIds()) {
                assayGroupMap.put(assayId, assayGroup);
            }
        }
        return assayGroupMap;
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

        var assayGroupMap = createAssayGroupMap(assayGroups);
        var profilesMap = new HashMap<String, BaselineProfile>();

        for (var row : results) {
            var geneId = (String) row.get("gene_id");
            var geneName = (String) row.get("gene_name");
            var assayId = (String) row.get("assay");
            var expressionLevel = ((Number) row.get("expression_level")).doubleValue();

            var profile = profilesMap.computeIfAbsent(
                geneId, id -> new BaselineProfile(id, geneName));

            var assayGroup = assayGroupMap.get(assayId);
            if (assayGroup != null) {
                profile.add(assayGroup, new BaselineExpression(expressionLevel));
            }
        }

        return new GeneProfilesList<>(profilesMap.values());
    }
}