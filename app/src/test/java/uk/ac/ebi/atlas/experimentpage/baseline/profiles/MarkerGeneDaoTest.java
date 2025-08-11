package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.experiment.sample.BiologicalReplicate;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.model.ExpressionUnit;
import uk.ac.ebi.atlas.web.RnaSeqBaselineRequestPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MarkerGeneDaoTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    private RnaSeqBaselineRequestPreferences preferences;
    private AssayGroup assayGroup1;
    private AssayGroup assayGroup2;

    private MarkerGeneDao subject;
    private static final String EXPERIMENT_ACCESSION = "E-MTAB-1234";
    private static final String GENE_ID_1 = "ENSG00000001";
    private static final String GENE_ID_2 = "ENSG00000002";
    private static final String GENE_NAME_1 = "Gene1";
    private static final String GENE_NAME_2 = "Gene2";
    private static final String ASSAY_ID_1 = "assay1";
    private static final String ASSAY_ID_2 = "assay2";
    private static final double EXPRESSION_LEVEL_1 = 10.5;
    private static final double EXPRESSION_LEVEL_2 = 20.3;
    private static final double CUTOFF = 0.5;
    private static final int HEATMAP_SIZE = 50;

    @Before
    public void setUp() {
        subject = new MarkerGeneDao(jdbcTemplate);

        // Create real preferences
        preferences = new RnaSeqBaselineRequestPreferences();
        preferences.setUnit(ExpressionUnit.Absolute.Rna.TPM);
        preferences.setCutoff(CUTOFF);
        preferences.setHeatmapMatrixSize(HEATMAP_SIZE);
        preferences.setSpecific(true);

        // Create real AssayGroup instances
        BiologicalReplicate replicate1 = BiologicalReplicate.create(ASSAY_ID_1);
        BiologicalReplicate replicate2 = BiologicalReplicate.create(ASSAY_ID_2);

        assayGroup1 = new AssayGroup("g1", Collections.singleton(replicate1));
        assayGroup2 = new AssayGroup("g2", Collections.singleton(replicate2));
    }

    @Test
    public void fetchMarkerGeneProfilesReturnsCorrectProfiles() {
        // Mock database results
        List<Map<String, Object>> mockResults = Arrays.asList(
                createResultRow(GENE_ID_1, GENE_NAME_1, ASSAY_ID_1, EXPRESSION_LEVEL_1),
                createResultRow(GENE_ID_2, GENE_NAME_2, ASSAY_ID_2, EXPRESSION_LEVEL_2)
        );

        when(jdbcTemplate.queryForList(
                anyString(), eq(EXPERIMENT_ACCESSION), eq("TPM"), eq(CUTOFF), eq(HEATMAP_SIZE)))
                .thenReturn(mockResults);

        when(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(EXPERIMENT_ACCESSION), eq("TPM"), eq(CUTOFF)))
                .thenReturn(2L);

        // Call the method under test
        GeneProfilesList<BaselineProfile> result = subject.fetchMarkerGeneProfiles(
                EXPERIMENT_ACCESSION,
                Arrays.asList(assayGroup1, assayGroup2),
                preferences);

        // Verify the results
        assertThat(result).hasSize(2);
        assertThat(result.properties().get("searchResultTotal")).isEqualTo("2");

        // Verify the first profile
        BaselineProfile profile1 = result.stream()
                .filter(p -> p.getId().equals(GENE_ID_1))
                .findFirst()
                .orElse(null);
        assertThat(profile1).isNotNull();
        assertThat(profile1.getName()).isEqualTo(GENE_NAME_1);
        assertThat(profile1.getExpression(assayGroup1).getLevel()).isEqualTo(EXPRESSION_LEVEL_1);

        // Verify the second profile
        BaselineProfile profile2 = result.stream()
                .filter(p -> p.getId().equals(GENE_ID_2))
                .findFirst()
                .orElse(null);
        assertThat(profile2).isNotNull();
        assertThat(profile2.getName()).isEqualTo(GENE_NAME_2);
        assertThat(profile2.getExpression(assayGroup2).getLevel()).isEqualTo(EXPRESSION_LEVEL_2);
    }

    @Test
    public void fetchSpecificGeneProfilesReturnsCorrectProfiles() {
        // Mock database results
        List<Map<String, Object>> mockResults = Arrays.asList(
                createResultRow(GENE_ID_1, GENE_NAME_1, ASSAY_ID_1, EXPRESSION_LEVEL_1),
                createResultRow(GENE_ID_2, GENE_NAME_2, ASSAY_ID_2, EXPRESSION_LEVEL_2)
        );

        when(jdbcTemplate.queryForList(
                anyString(), any(Object[].class)))
                .thenReturn(mockResults);

        // Call the method under test
        GeneProfilesList<BaselineProfile> result = subject.fetchSpecificGeneProfiles(
                Arrays.asList(GENE_ID_1, GENE_ID_2),
                EXPERIMENT_ACCESSION,
                Arrays.asList(assayGroup1, assayGroup2),
                preferences);

        // Verify the results
        assertThat(result).hasSize(2);

        // Verify the first profile
        BaselineProfile profile1 = result.stream()
                .filter(p -> p.getId().equals(GENE_ID_1))
                .findFirst()
                .orElse(null);
        assertThat(profile1).isNotNull();
        assertThat(profile1.getName()).isEqualTo(GENE_NAME_1);
        assertThat(profile1.getExpression(assayGroup1).getLevel()).isEqualTo(EXPRESSION_LEVEL_1);

        // Verify the second profile
        BaselineProfile profile2 = result.stream()
                .filter(p -> p.getId().equals(GENE_ID_2))
                .findFirst()
                .orElse(null);
        assertThat(profile2).isNotNull();
        assertThat(profile2.getName()).isEqualTo(GENE_NAME_2);
        assertThat(profile2.getExpression(assayGroup2).getLevel()).isEqualTo(EXPRESSION_LEVEL_2);
    }

    @Test
    public void fetchCountReturnsCorrectCount() {
        when(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(EXPERIMENT_ACCESSION), eq("TPM"), eq(CUTOFF)))
                .thenReturn(42L);

        long count = subject.fetchCount(EXPERIMENT_ACCESSION, preferences);
        assertThat(count).isEqualTo(42L);
    }

    @Test
    public void fetchSpecificGeneProfilesReturnsEmptyListForEmptyInput() {
        GeneProfilesList<BaselineProfile> result = subject.fetchSpecificGeneProfiles(
                Collections.emptyList(),
                EXPERIMENT_ACCESSION,
                Arrays.asList(assayGroup1, assayGroup2),
                preferences);

        assertThat(result).isEmpty();
    }

    private Map<String, Object> createResultRow(String geneId, String geneName, String assayId, double expressionLevel) {
        Map<String, Object> row = new HashMap<>();
        row.put("gene_id", geneId);
        row.put("gene_name", geneName);
        row.put("assay", assayId);
        row.put("expression_level", expressionLevel);
        return row;
    }
}
