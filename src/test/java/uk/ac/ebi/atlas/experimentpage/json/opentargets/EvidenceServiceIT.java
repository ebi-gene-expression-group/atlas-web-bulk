package uk.ac.ebi.atlas.experimentpage.json.opentargets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
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
import uk.ac.ebi.atlas.experimentpage.context.MicroarrayRequestContext;

import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExpression;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayProfile;
import uk.ac.ebi.atlas.profiles.stream.MicroarrayProfileStreamFactory;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.web.MicroarrayRequestPreferences;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EvidenceServiceIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private ExperimentTrader experimentTrader;

    @Inject
    private MicroarrayProfileStreamFactory microarrayProfileStreamFactory;

    @Inject
    private DataFileHub dataFileHub;

    private EvidenceService<MicroarrayExpression, MicroarrayExperiment, MicroarrayRequestContext, MicroarrayProfile>
            subject;

    @BeforeAll
    void populateDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-fixture.sql"));
        populator.execute(dataSource);
    }

    @AfterAll
    void cleanDatabaseTables() {
        var populator = new ResourceDatabasePopulator();
        populator.addScripts(new ClassPathResource("fixtures/experiment-delete.sql"));
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        subject = new EvidenceService<>(microarrayProfileStreamFactory, dataFileHub, "test");
    }

    @Test
    void organismPartIsIncludedIfAvailable() {
        var listBuilder = ImmutableList.<JsonObject>builder();
        var experiment = (MicroarrayExperiment) experimentTrader.getPublicExperiment("E-GEOD-40611");
        subject.evidenceForExperiment(
                experiment, contrast -> {
                    var requestPreferences = new MicroarrayRequestPreferences();
                    requestPreferences.setHeatmapMatrixSize(1000);
                    requestPreferences.setSelectedColumnIds(ImmutableSet.of(contrast.getId()));
                    return new MicroarrayRequestContext(requestPreferences, experiment); },
                listBuilder::add);

        var result = listBuilder.build();
        assertThat(result)
                .isNotEmpty()
                .allSatisfy(evidence ->
                        assertThat(evidence.get("evidence").getAsJsonObject().get("organism_part").getAsString())
                                .isNotEmpty());
    }

    @Test
    void diseaseIdIsIncludedAsUniqueAssociation() {
        var listBuilder = ImmutableList.<JsonObject>builder();
        var experiment = (MicroarrayExperiment) experimentTrader.getPublicExperiment("E-GEOD-40611");
        subject.evidenceForExperiment(
                experiment, contrast -> {
                    var requestPreferences = new MicroarrayRequestPreferences();
                    requestPreferences.setHeatmapMatrixSize(1000);
                    requestPreferences.setSelectedColumnIds(ImmutableSet.of(contrast.getId()));
                    return new MicroarrayRequestContext(requestPreferences, experiment); },
                listBuilder::add);

        var result = listBuilder.build();
        assertThat(result)
                .isNotEmpty()
                .allSatisfy(evidence ->
                        assertThat(evidence.get("unique_association_fields").getAsJsonObject().get("disease_id").getAsString())
                                .isNotEmpty());
    }
}
