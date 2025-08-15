package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.atlas.experimentpage.baseline.topgenes.BaselineExperimentTopGenesService;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.experiment.sample.BiologicalReplicate;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.model.ExpressionUnit;
import uk.ac.ebi.atlas.web.RnaSeqBaselineRequestPreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BaselineExperimentProfilesServiceTest {
    @Mock
    private BaselineExperimentTopGenesService topGenesService;

    @Mock
    private BaselineExperimentProfilesDao solrDao;

    @Mock
    private MarkerGeneDao postgresDao;

    @Mock
    private BaselineProfile mockProfile1;

    @Mock
    private BaselineProfile mockProfile2;

    private JsonArray mockColumnHeaders;

    private BaselineExperimentProfilesService subject;
    private RnaSeqBaselineRequestPreferences requestPrefs;
    private List<AssayGroup> assayGroups;
    private GeneProfilesList<BaselineProfile> expectedProfiles;
    private List<String> testGeneIds;

    private static final String EXPERIMENT_ACCESSION = "E-MTAB-1234";
    private static final String GENE_ID_1 = "ENSG00000001";
    private static final String GENE_ID_2 = "ENSG00000002";
    private static final String ASSAY_ID_1 = "assay1";
    private static final String ASSAY_ID_2 = "assay2";
    private static final double CUTOFF = 0.5;
    private static final int HEATMAP_SIZE = 50;
    private static final long EXPECTED_COUNT = 42L;

    @Before
    public void setUp() {
        initializeService();
        initializeRequestPreferences();
        initializeAssayGroups();
        initializeTestGeneIds();
        initializeSampleProfiles();
        initializeMockColumnHeaders();
    }

    private void initializeService() {
        subject = new BaselineExperimentProfilesService(
                topGenesService, solrDao, postgresDao);
    }

    private void initializeRequestPreferences() {
        requestPrefs = new RnaSeqBaselineRequestPreferences();
        requestPrefs.setUnit(ExpressionUnit.Absolute.Rna.TPM);
        requestPrefs.setCutoff(CUTOFF);
        requestPrefs.setHeatmapMatrixSize(HEATMAP_SIZE);
    }

    private void initializeAssayGroups() {
        BiologicalReplicate replicate1 = BiologicalReplicate.create(ASSAY_ID_1);
        BiologicalReplicate replicate2 = BiologicalReplicate.create(ASSAY_ID_2);
        var assayGroup1 = new AssayGroup("g1", Collections.singleton(replicate1));
        var assayGroup2 = new AssayGroup("g2", Collections.singleton(replicate2));
        assayGroups = Arrays.asList(assayGroup1, assayGroup2);
    }

    private void initializeTestGeneIds() {
        testGeneIds = Arrays.asList(GENE_ID_1, GENE_ID_2);
    }

    private void initializeSampleProfiles() {
        expectedProfiles = new GeneProfilesList<>();
        expectedProfiles.add(mockProfile1);
        expectedProfiles.add(mockProfile2);
        expectedProfiles.setTotalResultCount(2);
    }

    private void initializeMockColumnHeaders() {
        mockColumnHeaders = new JsonArray();
        for (AssayGroup assayGroup : assayGroups) {
            JsonObject header = new JsonObject();
            header.addProperty("assayGroupId", assayGroup.getId());
            // Add factorValue to match what's expected in MarkerGeneDao.findAssayGroupByAssayName
            header.addProperty("factorValue", "Sample " + assayGroup.getId());
            mockColumnHeaders.add(header);
        }
    }

    @Test
    public void topGeneProfilesUsesPostgresWhenSpecificIsTrue() {
        requestPrefs.setSpecific(true);
        when(postgresDao.fetchMarkerGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, requestPrefs, mockColumnHeaders))
                .thenReturn(expectedProfiles);

        GeneProfilesList<BaselineProfile> result = subject.getTopGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, requestPrefs, mockColumnHeaders);

        assertThat(result).isEqualTo(expectedProfiles);
        verify(postgresDao).fetchMarkerGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, requestPrefs, mockColumnHeaders);
        verify(topGenesService, never())
                .searchMostExpressedGenesInBaselineExperiment(anyString(), any());
        verify(solrDao, never())
                .fetchProfiles(anyList(), anyList(), any(), anyString());
    }

    @Test
    public void topGeneProfilesUsesSolrWhenSpecificIsFalse() {
        requestPrefs.setSpecific(false);
        when(topGenesService.searchMostExpressedGenesInBaselineExperiment(
                EXPERIMENT_ACCESSION, requestPrefs))
                .thenReturn(testGeneIds);
        when(solrDao.fetchProfiles(
                testGeneIds, assayGroups, requestPrefs, EXPERIMENT_ACCESSION))
                .thenReturn(expectedProfiles);

        GeneProfilesList<BaselineProfile> result = subject.getTopGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, requestPrefs, mockColumnHeaders);

        assertThat(result).isEqualTo(expectedProfiles);
        verify(topGenesService)
                .searchMostExpressedGenesInBaselineExperiment(EXPERIMENT_ACCESSION, requestPrefs);
        verify(solrDao)
                .fetchProfiles(testGeneIds, assayGroups, requestPrefs, EXPERIMENT_ACCESSION);
        verify(postgresDao, never())
                .fetchMarkerGeneProfiles(anyString(), anyList(), any(), any());
    }

    @Test
    public void specificGeneProfilesUsesPostgresWhenSpecificIsTrue() {
        requestPrefs.setSpecific(true);
        when(postgresDao.fetchSpecificGeneProfiles(
                eq(testGeneIds), eq(EXPERIMENT_ACCESSION), eq(assayGroups), eq(requestPrefs), eq(mockColumnHeaders)))
                .thenReturn(expectedProfiles);

        GeneProfilesList<BaselineProfile> result = subject.getGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, requestPrefs, mockColumnHeaders, GENE_ID_1, GENE_ID_2);

        assertThat(result).isEqualTo(expectedProfiles);
        verify(postgresDao).fetchSpecificGeneProfiles(
                eq(testGeneIds), eq(EXPERIMENT_ACCESSION), eq(assayGroups), eq(requestPrefs), eq(mockColumnHeaders));
        verify(solrDao, never())
                .fetchProfiles(anyList(), anyList(), any(), anyString());
    }

    @Test
    public void specificGeneProfilesUsesSolrWhenSpecificIsFalse() {
        requestPrefs.setSpecific(false);
        when(solrDao.fetchProfiles(
                eq(testGeneIds), eq(assayGroups), eq(requestPrefs), eq(EXPERIMENT_ACCESSION)))
                .thenReturn(expectedProfiles);

        GeneProfilesList<BaselineProfile> result = subject.getGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, requestPrefs, mockColumnHeaders, GENE_ID_1, GENE_ID_2);

        assertThat(result).isEqualTo(expectedProfiles);
        verify(solrDao)
                .fetchProfiles(eq(testGeneIds), eq(assayGroups), eq(requestPrefs), eq(EXPERIMENT_ACCESSION));
        verify(postgresDao, never())
                .fetchSpecificGeneProfiles(anyList(), anyString(), anyList(), any(), any());
    }

    @Test
    public void countUsesPostgresWhenSpecificIsTrue() {
        requestPrefs.setSpecific(true);
        when(postgresDao.fetchCount(EXPERIMENT_ACCESSION, requestPrefs))
                .thenReturn(EXPECTED_COUNT);

        long result = subject.fetchCount(EXPERIMENT_ACCESSION, requestPrefs);

        assertThat(result).isEqualTo(EXPECTED_COUNT);
        verify(postgresDao).fetchCount(EXPERIMENT_ACCESSION, requestPrefs);
        verify(solrDao, never())
                .fetchCount(anyString(), any());
    }

    @Test
    public void countUsesSolrWhenSpecificIsFalse() {
        requestPrefs.setSpecific(false);
        when(solrDao.fetchCount(EXPERIMENT_ACCESSION, requestPrefs))
                .thenReturn(EXPECTED_COUNT);

        long result = subject.fetchCount(EXPERIMENT_ACCESSION, requestPrefs);

        assertThat(result).isEqualTo(EXPECTED_COUNT);
        verify(solrDao).fetchCount(EXPERIMENT_ACCESSION, requestPrefs);
        verify(postgresDao, never())
                .fetchCount(anyString(), any());
    }
}
