package uk.ac.ebi.atlas.experimentpage.qc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.model.download.ExternallyAvailableContent;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;

class MultiqcReportTest {
    private static final String ACCESSION = "E-GEOD-237989";
    private static final String REPORT_HTML = "<html><body>MultiQC report</body></html>";

    @TempDir
    Path experimentsDir;

    private DataFileHub dataFileHub;

    @BeforeEach
    void setUp() {
        dataFileHub = new DataFileHub(experimentsDir, experimentsDir.resolve("expdesign"));
    }

    private void writeReport() throws IOException {
        var qcDir = Files.createDirectories(experimentsDir.resolve("magetab").resolve(ACCESSION).resolve("qc"));
        Files.writeString(qcDir.resolve(ACCESSION + "-multiqc_report.html"), REPORT_HTML);
    }

    private static DifferentialExperiment experiment(ExperimentType experimentType) {
        var experiment = mock(DifferentialExperiment.class);
        when(experiment.getAccession()).thenReturn(ACCESSION);
        when(experiment.getType()).thenReturn(experimentType);
        return experiment;
    }

    @Test
    void reportUrlIncludesAccessKeyOnlyWhenGiven() {
        assertThat(MultiqcReportController.getMultiqcReportUrl(ACCESSION, ""))
                .isEqualTo("experiments-content/E-GEOD-237989/multiqc-report");
        assertThat(MultiqcReportController.getMultiqcReportUrl(ACCESSION, "abc"))
                .isEqualTo("experiments-content/E-GEOD-237989/multiqc-report?accessKey=abc");
    }

    @Test
    void dataFileHubFindsReportAtExpectedPath() throws IOException {
        assertThat(dataFileHub.getExperimentFiles(ACCESSION).multiqcReport.exists()).isFalse();
        writeReport();
        assertThat(dataFileHub.getExperimentFiles(ACCESSION).multiqcReport.exists()).isTrue();
    }

    @Test
    void controllerStreamsReportAsHtml() throws IOException {
        writeReport();
        var experimentTrader = mock(ExperimentTrader.class);
        when(experimentTrader.getExperiment(ACCESSION, null)).thenReturn(mock(Experiment.class));
        var response = new MockHttpServletResponse();

        new MultiqcReportController(experimentTrader, dataFileHub).getMultiqcReport(response, ACCESSION, null);

        assertThat(response.getContentType()).startsWith("text/html");
        assertThat(response.getContentAsString()).isEqualTo(REPORT_HTML);
    }

    @Test
    void controllerReturnsNotFoundWhenReportIsMissing() {
        var experimentTrader = mock(ExperimentTrader.class);
        when(experimentTrader.getExperiment(ACCESSION, null)).thenReturn(mock(Experiment.class));

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() ->
                new MultiqcReportController(experimentTrader, dataFileHub)
                        .getMultiqcReport(new MockHttpServletResponse(), ACCESSION, null));
    }

    @Test
    void supplierAddsPlotsLinkForRnaSeqDifferentialWithReport() throws IOException {
        writeReport();

        var content = new MultiqcReportSupplier(dataFileHub).get(experiment(RNASEQ_MRNA_DIFFERENTIAL));

        assertThat(content).hasSize(1);
        var link = content.iterator().next();
        assertThat(link.uri.toString()).isEqualTo("redirect:experiments-content/E-GEOD-237989/multiqc-report");
        assertThat(link.description.type()).isEqualTo("link");
        assertThat(new MultiqcReportSupplier(dataFileHub).contentType())
                .isEqualTo(ExternallyAvailableContent.ContentType.PLOTS);
    }

    @Test
    void supplierAddsNothingWithoutReportOrForNonRnaSeq() throws IOException {
        assertThat(new MultiqcReportSupplier(dataFileHub).get(experiment(RNASEQ_MRNA_DIFFERENTIAL))).isEmpty();

        writeReport();
        assertThat(new MultiqcReportSupplier(dataFileHub).get(experiment(PROTEOMICS_DIFFERENTIAL))).isEmpty();
    }
}
