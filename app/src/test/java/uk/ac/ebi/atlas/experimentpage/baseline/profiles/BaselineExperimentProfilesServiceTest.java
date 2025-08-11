package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

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
    private BaselineExperimentTopGenesService baselineExperimentTopGenesService;

    @Mock
    private BaselineExperimentProfilesDao baselineExperimentProfilesDao;

    @Mock
    private MarkerGeneDao markerGeneDao;

    @Mock
    private BaselineProfile baselineProfile1;

    @Mock
    private BaselineProfile baselineProfile2;

    private BaselineExperimentProfilesService subject;
    private RnaSeqBaselineRequestPreferences preferences;
    private List<AssayGroup> assayGroups;
    private GeneProfilesList<BaselineProfile> expectedProfiles;
    private List<String> geneIds;

    private static final String EXPERIMENT_ACCESSION = "E-MTAB-1234";
    private static final String GENE_ID_1 = "ENSG00000001";
    private static final String GENE_ID_2 = "ENSG00000002";
    private static final String ASSAY_ID_1 = "assay1";
    private static final String ASSAY_ID_2 = "assay2";
    private static final double CUTOFF = 0.5;
    private static final int HEATMAP_SIZE = 50;

    @Before
    public void setUp() {
        subject = new BaselineExperimentProfilesService(
                baselineExperimentTopGenesService, baselineExperimentProfilesDao, markerGeneDao);

        // Create preferences
        preferences = new RnaSeqBaselineRequestPreferences();
        preferences.setUnit(ExpressionUnit.Absolute.Rna.TPM);
        preferences.setCutoff(CUTOFF);
        preferences.setHeatmapMatrixSize(HEATMAP_SIZE);

        // Create assay groups
        BiologicalReplicate replicate1 = BiologicalReplicate.create(ASSAY_ID_1);
        BiologicalReplicate replicate2 = BiologicalReplicate.create(ASSAY_ID_2);
        var assayGroup1 = new AssayGroup("g1", Collections.singleton(replicate1));
        var assayGroup2 = new AssayGroup("g2", Collections.singleton(replicate2));
        assayGroups = Arrays.asList(assayGroup1, assayGroup2);

        // Create gene IDs
        geneIds = Arrays.asList(GENE_ID_1, GENE_ID_2);

        // Create expected profiles
        expectedProfiles = new GeneProfilesList<>();
        expectedProfiles.add(baselineProfile1);
        expectedProfiles.add(baselineProfile2);
        expectedProfiles.setTotalResultCount(2);
    }

    @Test
    public void getTopGeneProfilesUsesMarkerGeneDaoWhenSpecificIsTrue() {
        // Set specific to true
        preferences.setSpecific(true);

        // Mock marker gene DAO
        when(markerGeneDao.fetchMarkerGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, preferences))
                .thenReturn(expectedProfiles);

        // Call the method under test
        GeneProfilesList<BaselineProfile> result = subject.getTopGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, preferences);

        // Verify the result
        assertThat(result).isEqualTo(expectedProfiles);

        // Verify that the marker gene DAO was used
        verify(markerGeneDao).fetchMarkerGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, preferences);

        // Verify that the Solr-based implementation was not used
        verify(baselineExperimentTopGenesService, never())
                .searchMostExpressedGenesInBaselineExperiment(anyString(), any());
        verify(baselineExperimentProfilesDao, never())
                .fetchProfiles(anyList(), anyList(), any(), anyString());
    }

    @Test
    public void getTopGeneProfilesUsesSolrWhenSpecificIsFalse() {
        // Set specific to false
        preferences.setSpecific(false);

        // Mock Solr-based implementation
        when(baselineExperimentTopGenesService.searchMostExpressedGenesInBaselineExperiment(
                EXPERIMENT_ACCESSION, preferences))
                .thenReturn(geneIds);
        when(baselineExperimentProfilesDao.fetchProfiles(
                geneIds, assayGroups, preferences, EXPERIMENT_ACCESSION))
                .thenReturn(expectedProfiles);

        // Call the method under test
        GeneProfilesList<BaselineProfile> result = subject.getTopGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, preferences);

        // Verify the result
        assertThat(result).isEqualTo(expectedProfiles);

        // Verify that the Solr-based implementation was used
        verify(baselineExperimentTopGenesService)
                .searchMostExpressedGenesInBaselineExperiment(EXPERIMENT_ACCESSION, preferences);
        verify(baselineExperimentProfilesDao)
                .fetchProfiles(geneIds, assayGroups, preferences, EXPERIMENT_ACCESSION);

        // Verify that the marker gene DAO was not used
        verify(markerGeneDao, never())
                .fetchMarkerGeneProfiles(anyString(), anyList(), any());
    }

    @Test
    public void getGeneProfilesUsesMarkerGeneDaoWhenSpecificIsTrue() {
        // Set specific to true
        preferences.setSpecific(true);

        // Mock marker gene DAO
        when(markerGeneDao.fetchSpecificGeneProfiles(
                eq(Arrays.asList(GENE_ID_1, GENE_ID_2)), eq(EXPERIMENT_ACCESSION), eq(assayGroups), eq(preferences)))
                .thenReturn(expectedProfiles);

        // Call the method under test
        GeneProfilesList<BaselineProfile> result = subject.getGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, preferences, GENE_ID_1, GENE_ID_2);

        // Verify the result
        assertThat(result).isEqualTo(expectedProfiles);

        // Verify that the marker gene DAO was used
        verify(markerGeneDao).fetchSpecificGeneProfiles(
                eq(Arrays.asList(GENE_ID_1, GENE_ID_2)), eq(EXPERIMENT_ACCESSION), eq(assayGroups), eq(preferences));

        // Verify that the Solr-based implementation was not used
        verify(baselineExperimentProfilesDao, never())
                .fetchProfiles(anyList(), anyList(), any(), anyString());
    }

    @Test
    public void getGeneProfilesUsesSolrWhenSpecificIsFalse() {
        // Set specific to false
        preferences.setSpecific(false);

        // Mock Solr-based implementation
        when(baselineExperimentProfilesDao.fetchProfiles(
                eq(Arrays.asList(GENE_ID_1, GENE_ID_2)), eq(assayGroups), eq(preferences), eq(EXPERIMENT_ACCESSION)))
                .thenReturn(expectedProfiles);

        // Call the method under test
        GeneProfilesList<BaselineProfile> result = subject.getGeneProfiles(
                EXPERIMENT_ACCESSION, assayGroups, preferences, GENE_ID_1, GENE_ID_2);

        // Verify the result
        assertThat(result).isEqualTo(expectedProfiles);

        // Verify that the Solr-based implementation was used
        verify(baselineExperimentProfilesDao)
                .fetchProfiles(eq(Arrays.asList(GENE_ID_1, GENE_ID_2)), eq(assayGroups), eq(preferences), eq(EXPERIMENT_ACCESSION));

        // Verify that the marker gene DAO was not used
        verify(markerGeneDao, never())
                .fetchSpecificGeneProfiles(anyList(), anyString(), anyList(), any());
    }

    @Test
    public void fetchCountUsesMarkerGeneDaoWhenSpecificIsTrue() {
        // Set specific to true
        preferences.setSpecific(true);

        // Mock marker gene DAO
        when(markerGeneDao.fetchCount(EXPERIMENT_ACCESSION, preferences))
                .thenReturn(42L);

        // Call the method under test
        long result = subject.fetchCount(EXPERIMENT_ACCESSION, preferences);

        // Verify the result
        assertThat(result).isEqualTo(42L);

        // Verify that the marker gene DAO was used
        verify(markerGeneDao).fetchCount(EXPERIMENT_ACCESSION, preferences);

        // Verify that the Solr-based implementation was not used
        verify(baselineExperimentProfilesDao, never())
                .fetchCount(anyString(), any());
    }

    @Test
    public void fetchCountUsesSolrWhenSpecificIsFalse() {
        // Set specific to false
        preferences.setSpecific(false);

        // Mock Solr-based implementation
        when(baselineExperimentProfilesDao.fetchCount(EXPERIMENT_ACCESSION, preferences))
                .thenReturn(42L);

        // Call the method under test
        long result = subject.fetchCount(EXPERIMENT_ACCESSION, preferences);

        // Verify the result
        assertThat(result).isEqualTo(42L);

        // Verify that the Solr-based implementation was used
        verify(baselineExperimentProfilesDao).fetchCount(EXPERIMENT_ACCESSION, preferences);

        // Verify that the marker gene DAO was not used
        verify(markerGeneDao, never())
                .fetchCount(anyString(), any());
    }
}