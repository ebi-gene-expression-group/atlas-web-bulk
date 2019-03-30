package uk.ac.ebi.atlas.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.model.resource.ResourceType;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.trader.ExpressionAtlasExperimentTrader;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class  ContrastImageTraderIT {
    @Inject
    private DataSource dataSource;

    @Inject
    JdbcUtils jdbcUtils;

    @Inject
    private ContrastImageTrader subject;

    @Inject
    private ExpressionAtlasExperimentTrader experimentTrader;

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
    @MethodSource("rnaSeqDifferentialExperimentAccessionProvider")
    void dataHasRightFormatForRnaSeqExperiments(String accession) {
        DifferentialExperiment differentialExperiment =
                (DifferentialExperiment) experimentTrader.getPublicExperiment(accession);

        assertAboutResult(subject.contrastImages(differentialExperiment));
    }

    @ParameterizedTest
    @MethodSource("microarrayExperimentAccessionProvider")
    void dataHasRightFormatForMicroarrayExperiments(String accession) {
        MicroarrayExperiment differentialExperiment =
                (MicroarrayExperiment) experimentTrader.getPublicExperiment(accession);

        assertAboutResult(subject.contrastImages(differentialExperiment));
    }

    private void assertAboutResult(Map<String, JsonArray> result) {
        for (Map.Entry<String, JsonArray> entry : result.entrySet()) {
            assertTrue("Contrast: " + entry.getKey(), entry.getKey().matches("g\\d+_g\\d+"));

            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                assertTrue(element.getAsJsonObject().has("type"));
                assertTrue(element.getAsJsonObject().has("uri"));
                try {
                    ResourceType.forFileName(element.getAsJsonObject().get("type").getAsString());
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }
    }

    private Stream<String> microarrayExperimentAccessionProvider() {
        return Stream.of(
                jdbcUtils.fetchRandomExpressionAtlasExperimentAccession(
                        MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
                        MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
                        MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL));
    }

    private Stream<String> rnaSeqDifferentialExperimentAccessionProvider() {
        return Stream.of(jdbcUtils.fetchRandomExpressionAtlasExperimentAccession(RNASEQ_MRNA_DIFFERENTIAL));
    }
}
