package uk.ac.ebi.atlas.experimentpage.qc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.Assert.assertThat;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicroarrayQCFilesIT {
    @Inject
    DataSource dataSource;

    @Inject
    JdbcUtils jdbcUtils;

    @Inject
    private ExperimentTrader experimentTrader;

    @Inject
    private DataFileHub dataFileHub;

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

    @ParameterizedTest
    @MethodSource("microarrayExperimentAccessionProvider")
    void testExperiment(String accession) {
        MicroarrayExperiment experiment =
                (MicroarrayExperiment) experimentTrader.getPublicExperiment(accession);
        MicroarrayQcFiles microarrayQCFiles =
                new MicroarrayQcFiles(dataFileHub.getExperimentFiles(accession).qcFolder);

        for (String arrayDesignReadOffFromFolderName : microarrayQCFiles.getArrayDesignsThatHaveQcReports()) {
            assertThat(arrayDesignReadOffFromFolderName,
                    is(oneOf(new ArrayList<>(experiment.getArrayDesignAccessions()).toArray())));
        }
    }

    private Stream<String> microarrayExperimentAccessionProvider() {
        return Stream.of(
                jdbcUtils.fetchRandomExperimentAccession(
                        MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
                        MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
                        MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL));
    }
}
