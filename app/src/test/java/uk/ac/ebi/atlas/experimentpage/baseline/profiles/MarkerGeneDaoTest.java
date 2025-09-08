package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
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
    private JsonArray mockColumnHeaders;


    private MarkerGeneDao subject;
    private static final String EXPERIMENT_ACCESSION = "E-MTAB-1234";
    private static final String GENE_ID_1 = "ENSG00000001";
    private static final String GENE_ID_2 = "ENSG00000002";
    private static final String GENE_NAME_1 = "Gene1";
    private static final String GENE_NAME_2 = "Gene2";
    private static final String ASSAY_GROUP_ID_1 = "g1";
    private static final String ASSAY_GROUP_ID_2 = "g2";
    private static final double EXPRESSION_LEVEL_1 = 10.5;
    private static final double EXPRESSION_LEVEL_2 = 20.3;
    private static final double CUTOFF = 0.5;
    private static final int MAX_NUMBER_OF_MARKER_GENES = 50;

    @Before
    public void setUp() {
        subject = new MarkerGeneDao(jdbcTemplate);

        preferences = new RnaSeqBaselineRequestPreferences();
        preferences.setUnit(ExpressionUnit.Absolute.Rna.TPM);
        preferences.setCutoff(CUTOFF);
        preferences.setHeatmapMatrixSize(MAX_NUMBER_OF_MARKER_GENES);
        preferences.setSpecific(true);

        BiologicalReplicate replicate1 = BiologicalReplicate.create(ASSAY_GROUP_ID_1);
        BiologicalReplicate replicate2 = BiologicalReplicate.create(ASSAY_GROUP_ID_2);

        assayGroup1 = new AssayGroup(ASSAY_GROUP_ID_1, Collections.singleton(replicate1));
        assayGroup2 = new AssayGroup(ASSAY_GROUP_ID_2, Collections.singleton(replicate2));

        initializeMockColumnHeaders();
    }

    private void initializeMockColumnHeaders() {
        mockColumnHeaders = new JsonArray();
        mockColumnHeaders.add(getHeader(assayGroup1, ASSAY_GROUP_ID_1));
        mockColumnHeaders.add(getHeader(assayGroup2, ASSAY_GROUP_ID_2));
    }

    private @NotNull JsonObject getHeader(AssayGroup assayGroup1, String assayId1) {
        JsonObject header1 = new JsonObject();
        header1.addProperty("assayGroupId", assayGroup1.getId());
        header1.addProperty("factorValue", assayId1);
        return header1;
    }

    @Test
    public void fetchGeneProfilesReturnsEmptyListForEmptyInput() {
        GeneProfilesList<BaselineProfile> result = subject.fetchMarkerGeneProfiles(
            EXPERIMENT_ACCESSION,
            Arrays.asList(assayGroup1, assayGroup2),
            preferences,
            mockColumnHeaders);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMarkerGeneProfilesReturnsCorrectProfiles() {
        List<Map<String, Object>> mockResults = Arrays.asList(
                createResultRow(GENE_ID_1, GENE_NAME_1, ASSAY_GROUP_ID_1, EXPRESSION_LEVEL_1),
                createResultRow(GENE_ID_2, GENE_NAME_2, ASSAY_GROUP_ID_2, EXPRESSION_LEVEL_2)
        );

        when(jdbcTemplate.queryForList(anyString())).thenReturn(mockResults);

        GeneProfilesList<BaselineProfile> result = subject.fetchMarkerGeneProfiles(
            EXPERIMENT_ACCESSION,
            Arrays.asList(assayGroup1, assayGroup2),
            preferences,
            mockColumnHeaders);

        assertThat(result).hasSize(mockResults.size());

        BaselineProfile profile1 = result.stream()
                .filter(p -> p.getId().equals(GENE_ID_1))
                .findFirst()
                .orElse(null);

        assertThat(profile1).isNotNull();
        assertThat(profile1.getName()).isEqualTo(GENE_NAME_1);
        assertThat(profile1.getExpression(assayGroup1).getLevel()).isEqualTo(EXPRESSION_LEVEL_1);

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
        final long expectedCount = 42L;
        when(jdbcTemplate.queryForObject(
            anyString(), eq(Long.class), eq(EXPERIMENT_ACCESSION),
            eq(ExpressionUnit.Absolute.Rna.TPM.getDatabaseValue()), eq(CUTOFF)))
                .thenReturn(expectedCount);

        long count = subject.fetchCount(EXPERIMENT_ACCESSION, preferences);
        assertThat(count).isEqualTo(expectedCount);
    }

    private Map<String, Object> createResultRow(String geneId, String geneName, String assayId, double expressionLevel) {
        Map<String, Object> row = new HashMap<>();
        row.put("gene_id", geneId);
        row.put("gene_name", geneName);
        row.put("assay_id", assayId);
        row.put("expression_level", expressionLevel);
        return row;
    }
}
