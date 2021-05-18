package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.doesNotHave;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExperimentDownloadControllerWIT {
    private static final List<String> EXPERIMENT_ACCESSION_LIST = ImmutableList.of(
            "E-ERAD-475", //RNASEQ_MRNA_BASELINE
            "E-GEOD-43049", //MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL
            "E-MEXP-1968", //MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL
            "E-MTAB-3834", //RNASEQ_MRNA_DIFFERENTIAL
            "E-PROT-1", //PROTEOMICS_BASELINE
            "E-TABM-713"); //MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL
    private static final List<String> INVALID_EXPERIMENT_ACCESSION_LIST = ImmutableList.of("E-ERAD", "E-GEOD");
    private static final String ARCHIVE_NAME = "{0}-{1}-files.zip";
    private static final String ARCHIVE_DOWNLOAD_LIST_URL = "/experiments/download/zip";

    @Inject
    private DataSource dataSource;

    @Inject
    private ExperimentFileLocationService experimentFileLocationService;

    @Inject
    private ExperimentTrader experimentTrader;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void downloadArchiveForInvalidExperimentAccessions() throws Exception {
        this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_LIST_URL)
                .param("accession", INVALID_EXPERIMENT_ACCESSION_LIST.get(0))
                .param("accession", INVALID_EXPERIMENT_ACCESSION_LIST.get(1)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().string(""));
    }

    @Test
    void downloadArchiveForMixedValidAndInvalidExperimentAccessions() throws Exception {
        var result = this.mockMvc.perform(get(ARCHIVE_DOWNLOAD_LIST_URL)
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(0))
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(1))
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(2))
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(3))
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(4))
                .param("accession", EXPERIMENT_ACCESSION_LIST.get(5))
                .param("accession", INVALID_EXPERIMENT_ACCESSION_LIST.get(0))
                .param("accession", INVALID_EXPERIMENT_ACCESSION_LIST.get(1)));

        var expectedArchiveName =
                MessageFormat.format(
                        ARCHIVE_NAME, EXPERIMENT_ACCESSION_LIST.size(), "experiment");

        var contentBytes = result.andReturn().getResponse().getContentAsByteArray();
        var zipInputStream = new ZipInputStream(new ByteArrayInputStream(contentBytes));

        var contentFileNames = ImmutableList.<String>builder();
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            contentFileNames.add(Paths.get(entry.getName()).getFileName().toString());
        }

        var paths = new ArrayList<String>();
        paths.addAll(EXPERIMENT_ACCESSION_LIST.stream()
                .map(experimentAccession -> getSourceValidFileNames(experimentAccession))
                .flatMap(List::stream)
                .collect(toImmutableList()));

        assertThat(paths.containsAll(contentFileNames.build())).isTrue();

        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + expectedArchiveName))
                .andExpect(content().contentType("application/zip"));
    }

    private ImmutableList<String> getSourceValidFileNames(String accession) {
        Experiment experiment = experimentTrader.getPublicExperiment(accession);
        var experimentType = experiment.getType();
        var paths = ImmutableList.<Path>builder();
        switch (experimentType) {
            case PROTEOMICS_BASELINE:
                paths.add(experimentFileLocationService.getFilePath(
                        experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONFIGURATION))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.BASELINE_FACTORS))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.IDF))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.PROTEOMICS_B_MAIN));
                break;

            case RNASEQ_MRNA_DIFFERENTIAL:
                paths.add(experimentFileLocationService.getFilePath(
                        experiment.getAccession(), ExperimentFileType.CONFIGURATION))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.IDF))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.RNASEQ_D_ANALYTICS));
                break;

            case RNASEQ_MRNA_BASELINE:
                paths.add(experimentFileLocationService.getFilePath(
                        experiment.getAccession(), ExperimentFileType.CONFIGURATION))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.IDF))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.BASELINE_FACTORS))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.RNASEQ_B_TPM));
                break;
            //MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL, MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL, MICROARRAY_2COLOUR_MICRORNA_DIFFERENTIAL
            default:
                paths.addAll(experimentFileLocationService.getFilePathsForArchive(
                        experiment, ExperimentFileType.MICROARRAY_D_ANALYTICS))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONFIGURATION))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                        .add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.IDF))
                        .build();
                break;
        }
        
        return paths.build().stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(toImmutableList());
    }

}

