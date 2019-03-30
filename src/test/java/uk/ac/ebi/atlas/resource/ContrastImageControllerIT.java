package uk.ac.ebi.atlas.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.trader.ExpressionAtlasExperimentTrader;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContrastImageControllerIT {
    @Inject
    DataSource dataSource;

    @Inject
    JdbcUtils jdbcUtils;

    @Inject
    private ContrastImageTrader subject;

    @Inject
    private ExpressionAtlasExperimentTrader experimentTrader;

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

    @ParameterizedTest
    @MethodSource("rnaSeqDifferentialExperimentAccessionProvider")
    void rnaSeqExperiments(String accession) throws Exception {
            DifferentialExperiment differentialExperiment =
                    (DifferentialExperiment) experimentTrader.getPublicExperiment(accession);
            assertAboutResult(subject.contrastImages(differentialExperiment));
    }

    @ParameterizedTest
    @MethodSource("microarrayExperimentAccessionProvider")
    void microarrayExperiments(String accession) throws Exception {
        MicroarrayExperiment differentialExperiment =
                (MicroarrayExperiment) experimentTrader.getPublicExperiment(accession);
        assertAboutResult(subject.contrastImages(differentialExperiment));
    }

    private void assertAboutResult(Map<String, JsonArray> result) throws Exception {
        for (Map.Entry<String, JsonArray> entryPerContrast : result.entrySet()) {
            for (JsonElement e : entryPerContrast.getValue()) {
                testResourceExists(e.getAsJsonObject().get("uri").getAsString());
            }
        }
    }

    private void testResourceExists(String resourceURI) throws Exception {
        this.mockMvc.perform(get("/" + resourceURI)).andExpect(status().isOk());
    }

    private Stream<String> microarrayExperimentAccessionProvider() {
        return Stream.of(
                jdbcUtils.fetchRandomExpressionAtlasExperimentAccession(
                        MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL,
                        MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
                        MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL));
    }

    private Stream<String> rnaSeqDifferentialExperimentAccessionProvider() {
        return Stream.of(jdbcUtils.fetchRandomExpressionAtlasExperimentAccession(RNASEQ_MRNA_DIFFERENTIAL));
    }
}
