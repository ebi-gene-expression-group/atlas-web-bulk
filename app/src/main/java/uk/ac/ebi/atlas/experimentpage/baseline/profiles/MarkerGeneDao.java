package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExpression;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

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

    private static final String FETCH_MARKER_GENES_SQL =
            "SELECT gene_id, gene_name, assay, expression_level " +
            "FROM gxa_marker_gene " +
            "WHERE experiment_accession = ? " +
            "AND expression_unit = ? " +
            "AND expression_level >= ? " +
            "ORDER BY marker_gene_rank " +
            "LIMIT ?";

    private static final String COUNT_MARKER_GENES_SQL =
            "SELECT COUNT(DISTINCT gene_id) " +
            "FROM gxa_marker_gene " +
            "WHERE experiment_accession = ? " +
            "AND expression_unit = ? " +
            "AND expression_level >= ?";

    private static final String FETCH_SPECIFIC_GENES_SQL =
            "SELECT gene_id, gene_name, assay, expression_level " +
            "FROM gxa_marker_gene " +
            "WHERE experiment_accession = ? " +
            "AND gene_id IN (%s) " +
            "AND expression_unit = ? " +
            "AND expression_level >= ?";

    public MarkerGeneDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Fetches marker gene profiles from the database.
     *
     * @param experimentAccession The experiment accession
     * @param assayGroups The list of assay groups
     * @param preferences The request preferences
     * @return A list of baseline profiles
     */
    public GeneProfilesList<BaselineProfile> fetchMarkerGeneProfiles(String experimentAccession,
                                                                    List<AssayGroup> assayGroups,
                                                                    BaselineRequestPreferences<?> preferences) {
        // Map of assay IDs to assay groups
        Map<String, AssayGroup> assayGroupMap = createAssayGroupMap(assayGroups);

        // Fetch data from database
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                FETCH_MARKER_GENES_SQL,
                experimentAccession,
                preferences.getUnit().getDatabaseValue(),
                preferences.getCutoff(),
                preferences.getHeatmapMatrixSize());

        // Process results into a map of gene ID to BaselineProfile
        Map<String, BaselineProfile> profilesMap = new HashMap<>();

        for (Map<String, Object> row : results) {
            String geneId = (String) row.get("gene_id");
            String geneName = (String) row.get("gene_name");
            String assayId = (String) row.get("assay");
            double expressionLevel = ((Number) row.get("expression_level")).doubleValue();

            // Get or create profile
            BaselineProfile profile = profilesMap.computeIfAbsent(
                    geneId, id -> new BaselineProfile(id, geneName));

            // Find the assay group for this assay
            AssayGroup assayGroup = assayGroupMap.get(assayId);
            if (assayGroup != null) {
                // Add expression to profile
                profile.add(assayGroup, new BaselineExpression(expressionLevel));
            }
        }

        // Create and return the gene profiles list
        GeneProfilesList<BaselineProfile> geneProfilesList = new GeneProfilesList<>(profilesMap.values());
        geneProfilesList.setTotalResultCount(fetchCount(experimentAccession, preferences));

        return geneProfilesList;
    }

    /**
     * Fetches specific gene profiles from the database.
     *
     * @param geneIds The list of gene IDs to fetch
     * @param experimentAccession The experiment accession
     * @param assayGroups The list of assay groups
     * @param preferences The request preferences
     * @return A list of baseline profiles
     */
    public GeneProfilesList<BaselineProfile> fetchSpecificGeneProfiles(List<String> geneIds,
                                                                      String experimentAccession,
                                                                      List<AssayGroup> assayGroups,
                                                                      BaselineRequestPreferences<?> preferences) {
        if (geneIds.isEmpty()) {
            return new GeneProfilesList<>();
        }

        // Map of assay IDs to assay groups
        Map<String, AssayGroup> assayGroupMap = createAssayGroupMap(assayGroups);

        // Create placeholders for the IN clause
        String placeholders = geneIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        // Create the SQL query with the IN clause
        String sql = String.format(FETCH_SPECIFIC_GENES_SQL, placeholders);

        // Create the parameters array
        Object[] params = new Object[geneIds.size() + 3];
        params[0] = experimentAccession;
        for (int i = 0; i < geneIds.size(); i++) {
            params[i + 1] = geneIds.get(i);
        }
        params[geneIds.size() + 1] = preferences.getUnit().getDatabaseValue();
        params[geneIds.size() + 2] = preferences.getCutoff();

        // Fetch data from database
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);

        // Process results into a map of gene ID to BaselineProfile
        Map<String, BaselineProfile> profilesMap = new HashMap<>();

        for (Map<String, Object> row : results) {
            String geneId = (String) row.get("gene_id");
            String geneName = (String) row.get("gene_name");
            String assayId = (String) row.get("assay");
            double expressionLevel = ((Number) row.get("expression_level")).doubleValue();

            // Get or create profile
            BaselineProfile profile = profilesMap.computeIfAbsent(
                    geneId, id -> new BaselineProfile(id, geneName));

            // Find the assay group for this assay
            AssayGroup assayGroup = assayGroupMap.get(assayId);
            if (assayGroup != null) {
                // Add expression to profile
                profile.add(assayGroup, new BaselineExpression(expressionLevel));
            }
        }

        // Create and return the gene profiles list
        return new GeneProfilesList<>(profilesMap.values());
    }

    /**
     * Fetches the count of marker genes for an experiment.
     *
     * @param experimentAccession The experiment accession
     * @param preferences The request preferences
     * @return The count of marker genes
     */
    public long fetchCount(String experimentAccession, BaselineRequestPreferences<?> preferences) {
        return jdbcTemplate.queryForObject(
                COUNT_MARKER_GENES_SQL,
                Long.class,
                experimentAccession,
                preferences.getUnit().getDatabaseValue(),
                preferences.getCutoff());
    }

    /**
     * Creates a map of assay IDs to assay groups.
     *
     * @param assayGroups The list of assay groups
     * @return A map of assay IDs to assay groups
     */
    private Map<String, AssayGroup> createAssayGroupMap(List<AssayGroup> assayGroups) {
        Map<String, AssayGroup> assayGroupMap = new HashMap<>();
        for (AssayGroup assayGroup : assayGroups) {
            for (String assayId : assayGroup.getAssayIds()) {
                assayGroupMap.put(assayId, assayGroup);
            }
        }
        return assayGroupMap;
    }
}
