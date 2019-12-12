package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.GsonBuilder;
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
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import javax.sql.DataSource;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LatestExperimentsServiceIT {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    @Inject
    private ExperimentTrader experimentTrader;

    @Inject
    private LatestExperimentsDao latestExperimentsDao;

    @Inject
    private DataSource dataSource;

    private LatestExperimentsService subject;

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
    public void setUp() {
        subject =
                new LatestExperimentsService(
                        latestExperimentsDao, experimentTrader,
                        ImmutableSet.of(
                                ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL,
                                ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
                                ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
                                ExperimentType.RNASEQ_MRNA_DIFFERENTIAL,
                                ExperimentType.PROTEOMICS_BASELINE,
                                ExperimentType.RNASEQ_MRNA_BASELINE
                        ));
    }

    @Test
    public void fetchLatestExperimentsAttributes() throws Exception {
        var latestExperimentsJson =
                new GsonBuilder().create().toJsonTree(subject.fetchLatestExperimentsAttributes().get("latestExperiments"));

        var builder = ImmutableList.<Date>builder();
        for (var element : latestExperimentsJson.getAsJsonArray()) {
            builder.add(DATE_FORMAT.parse(element.getAsJsonObject().get("lastUpdate").getAsString()));
        }
        var dates = builder.build();
        assertThat(dates.reverse()).isSorted();
    }
}
