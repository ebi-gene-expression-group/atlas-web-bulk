package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_BASELINE;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadControllerTest {

    private static final List<String> EXPERIMENT_ACCESSION_LIST = ImmutableList.of(
            "E-ERAD-475", //RNASEQ_MRNA_BASELINE
            "E-GEOD-43049"); //MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL
            //"E-MEXP-1968", //MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL
            //"E-MTAB-3834", //RNASEQ_MRNA_DIFFERENTIAL
           // "E-PROT-1", //PROTEOMICS_BASELINE
            //"E-TABM-713"); //MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL

    @Mock
    private ExperimentFileLocationService experimentFileLocationServiceMock;

    @Mock
    private ExperimentTrader experimentTraderMock;

    @Mock
    private ExperimentDownloadSupplier.Proteomics proteomicsExperimentDownloadSupplier;

    @Mock
    private ExperimentDownloadSupplier.RnaSeqBaseline rnaSeqBaselineExperimentDownloadSupplier;

    @Mock
    private ExperimentDownloadSupplier.RnaSeqDifferential rnaSeqDifferentialExperimentDownloadSupplier;

    @Mock
    private ExperimentDownloadSupplier.Microarray microarrayExperimentDownloadSupplier;

    @Mock
    private ExperimentDownloadSupplier.ProteomicsDifferential proteomicsDifferentialExperimentDownloadSupplier;


    private ExperimentDownloadController subject;

    @Before
    public void setUp() {
        subject = new ExperimentDownloadController(
                experimentTraderMock,
                proteomicsExperimentDownloadSupplier,
                rnaSeqBaselineExperimentDownloadSupplier,
                rnaSeqDifferentialExperimentDownloadSupplier,
                microarrayExperimentDownloadSupplier,
                proteomicsDifferentialExperimentDownloadSupplier,
                experimentFileLocationServiceMock);
    }

    @Test
    public void testInvalidFilesForDownloading() {

        Experiment experiment = mock(Experiment.class);
        when(experiment.getAccession()).thenReturn(EXPERIMENT_ACCESSION_LIST.get(0));
        when(experiment.getType()).thenReturn(RNASEQ_MRNA_BASELINE);
        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION_LIST.get(0))).thenReturn(experiment);

        Experiment experiment_microarray = mock(Experiment.class);
        when(experiment_microarray.getAccession()).thenReturn(EXPERIMENT_ACCESSION_LIST.get(1));
        when(experiment_microarray.getType()).thenReturn(MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL);
        when(experimentTraderMock.getPublicExperiment(EXPERIMENT_ACCESSION_LIST.get(1))).thenReturn(experiment_microarray);

        String textPath = "file:dir/filename";
        Path path = Paths.get(textPath);

        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(0),
                ExperimentFileType.CONDENSE_SDRF))
                .thenReturn(path);
        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(0),
                ExperimentFileType.CONFIGURATION))
                .thenReturn(path);
        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(0),
                ExperimentFileType.IDF))
                .thenReturn(path);
        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(0),
                ExperimentFileType.BASELINE_FACTORS))
                .thenReturn(path);
        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(0),
                ExperimentFileType.RNASEQ_B_TPM))
                .thenReturn(path);

        when(experimentFileLocationServiceMock.getFilePathsForArchive(
                experiment_microarray,
                ExperimentFileType.MICROARRAY_D_ANALYTICS))
                .thenReturn(ImmutableList.of(path, path));
        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(1),
                ExperimentFileType.CONDENSE_SDRF))
                .thenReturn(path);
        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(1),
                ExperimentFileType.CONFIGURATION))
                .thenReturn(path);
        when(experimentFileLocationServiceMock.getFilePath(
                EXPERIMENT_ACCESSION_LIST.get(1),
                ExperimentFileType.IDF))
                .thenReturn(path);

        String jsonResponse = subject.checkMultipleExperimentsFileValid(EXPERIMENT_ACCESSION_LIST);
        ReadContext ctx = JsonPath.parse(jsonResponse);

        assertThat(ctx.<Map<String, Object>>read("$"))
                .extracting("invalidFiles")
                .extracting(EXPERIMENT_ACCESSION_LIST.get(0), EXPERIMENT_ACCESSION_LIST.get(1))
                .contains(
                        tuple(List.of("filename", "filename", "filename", "filename", "filename"),
                                List.of("filename", "filename", "filename", "filename", "filename")));

    }

}
