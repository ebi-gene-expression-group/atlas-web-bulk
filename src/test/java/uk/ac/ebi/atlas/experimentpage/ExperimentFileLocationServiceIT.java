package uk.ac.ebi.atlas.experimentpage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.trader.GxaExperimentRepository;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExperimentFileLocationServiceIT {
    private static final String CONFIGURATION_FILE_PATH_TEMPLATE = "{0}-configuration.xml";
    private static final String CONDENSED_SDRF_FILE_PATH_TEMPLATE = "{0}.condensed-sdrf.tsv";
    private static final String IDF_FILE_PATH_TEMPLATE = "{0}.idf.txt";

    private static final String PROTEOMICS_BASELINE_EXPRESSION_FILE_PATH_TEMPLATE = "{0}.tsv";
    private static final String RNASEQ_BASELINE_TPMS_FILE_PATH_TEMPLATE = "{0}-tpms.tsv";

    private static final String FACTORS_FILE_PATH_TEMPLATE = "{0}-factors.xml";
    private static final String DIFFERENTIAL_ANALYTICS_FILE_PATH_TEMPLATE = "{0}-analytics.tsv";
    private static final String MICROARRAY_ANALYTICS_FILE_PATH_TEMPLATE = "{0}/{0}_{1}-analytics.tsv";

    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcTestUtils;

    @Inject
    private DataFileHub dataFileHub;

    @Inject
    private GxaExperimentRepository gxaExperimentRepository;

    private ExperimentFileLocationService subject;

    @BeforeAll
    void populateDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScripts(
                new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        this.subject = new ExperimentFileLocationService(dataFileHub);
    }

    @Test
    void existingIdfFile() {
        existingFileOfType(jdbcTestUtils.fetchRandomPublicExperimentAccession(),
                ExperimentFileType.IDF, IDF_FILE_PATH_TEMPLATE);
    }

    @Test
    void existingCondensedSdrfFile() {
        existingFileOfType(jdbcTestUtils.fetchRandomPublicExperimentAccession(),
                ExperimentFileType.CONDENSE_SDRF, CONDENSED_SDRF_FILE_PATH_TEMPLATE);
    }

    @Test
    void existingConfigurationFile() {
        existingFileOfType(jdbcTestUtils.fetchRandomPublicExperimentAccession(),
                ExperimentFileType.CONFIGURATION, CONFIGURATION_FILE_PATH_TEMPLATE);
    }

    @Test
    void existingProteomisticMainFile() {
        existingFileOfType(jdbcTestUtils.fetchRandomExperimentAccession(PROTEOMICS_BASELINE),
                ExperimentFileType.PROTEOMICS_B_MAIN, PROTEOMICS_BASELINE_EXPRESSION_FILE_PATH_TEMPLATE);
    }
    @Test
    void existingRnaseqBaselineTPMFile() {
        existingFileOfType(jdbcTestUtils.fetchRandomExperimentAccession(RNASEQ_MRNA_BASELINE),
                ExperimentFileType.RNASEQ_B_TPM, RNASEQ_BASELINE_TPMS_FILE_PATH_TEMPLATE);
    }

    @Test
    void existingRnaseqBaselineFactorsFile() {
        existingFileOfType(jdbcTestUtils.fetchRandomExperimentAccession(RNASEQ_MRNA_BASELINE),
                ExperimentFileType.BASELINE_FACTORS, FACTORS_FILE_PATH_TEMPLATE);
    }

    @Test
    void existingRnaseqDifferentialAnalyticsFile() {
        existingFileOfType(jdbcTestUtils.fetchRandomExperimentAccession(RNASEQ_MRNA_DIFFERENTIAL),
                ExperimentFileType.RNASEQ_D_ANALYTICS, DIFFERENTIAL_ANALYTICS_FILE_PATH_TEMPLATE);
    }

    @Test
    void existingMicroarrayAnalyticsFiles() {

        var microarrayExperimentAccession = jdbcTestUtils.fetchRandomExperimentAccession(MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL);
        var microarrayExperiment = gxaExperimentRepository.getExperiment(microarrayExperimentAccession);

        List<String> arrayDesignAccessions = ((MicroarrayExperiment) microarrayExperiment).getArrayDesignAccessions();

        List<String> expectedFileNames = arrayDesignAccessions
                .stream()
                .map(arrayDesignAccession -> MessageFormat.format(MICROARRAY_ANALYTICS_FILE_PATH_TEMPLATE, microarrayExperimentAccession, arrayDesignAccession))
                .collect(Collectors.toList());

        existingArchiveFilesOfType(microarrayExperiment, ExperimentFileType.MICROARRAY_D_ANALYTICS, expectedFileNames, true);
    }

    @Test
    void invalidExperimentAccession() {
        Path path = subject.getFilePath("", ExperimentFileType.CONDENSE_SDRF);
        File file = path.toFile();

        assertThat(file).doesNotExist();
    }

    @Test
    void invalidFileType() {
        Path path = subject.getFilePath(jdbcTestUtils.fetchRandomExperimentAccession(PROTEOMICS_BASELINE),
                ExperimentFileType.MICROARRAY_D_ANALYTICS);

        assertThat(path).isNull();

    }

    @Test
    void invalidArchiveFileType() {
        var microarrayExperiment = gxaExperimentRepository.getExperiment(jdbcTestUtils.fetchRandomExperimentAccession(MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL));
        List<Path> paths = subject.getFilePathsForArchive(microarrayExperiment,
                ExperimentFileType.CONDENSE_SDRF);

        assertThat(paths).isNull();
    }

    private void existingFileOfType(String experimentAccession, ExperimentFileType fileType, String fileNameTemplate) {
        Path path = subject.getFilePath(experimentAccession, fileType);
        File file = path.toFile();

        assertThat(file).hasName(MessageFormat.format(fileNameTemplate, experimentAccession));
        assertThat(file).exists();
        assertThat(file).isFile();
    }

    private void existingArchiveFilesOfType(Experiment experiment,
                                            ExperimentFileType fileType,
                                            List<String> expectedFileNames,
                                            Boolean isArchive) {
        List<Path> paths = subject.getFilePathsForArchive(experiment, fileType);

        // Some paths, e.g. marker genes, might not be all in the DB
        assertThat(paths.size()).isGreaterThanOrEqualTo(expectedFileNames.size());

        for (Path path : paths) {
            File file = path.toFile();

            assertThat(file).exists();
            assertThat(file).isFile();
        }

        List<String> fileNames = paths.stream()
                .map(Path::toFile)
                .map(File::getName)
                .map(entry -> isArchive ? experiment.getAccession() + "/" + entry : entry)
                .collect(Collectors.toList());

        assertThat(expectedFileNames)
                .isNotEmpty()
                .containsAnyElementsOf(fileNames);
    }

}
