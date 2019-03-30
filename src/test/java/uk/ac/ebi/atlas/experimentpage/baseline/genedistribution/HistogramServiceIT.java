package uk.ac.ebi.atlas.experimentpage.baseline.genedistribution;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.experimentpage.context.BaselineRequestContext;
import uk.ac.ebi.atlas.model.ExpressionUnit;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExperiment;
import uk.ac.ebi.atlas.profiles.baseline.BaselineProfileStreamOptions;
import uk.ac.ebi.atlas.profiles.stream.RnaSeqBaselineProfileStreamFactory;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.trader.ExpressionAtlasExperimentTrader;
import uk.ac.ebi.atlas.web.RnaSeqBaselineRequestPreferences;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HistogramServiceIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private ExpressionAtlasExperimentTrader experimentTrader;

    @Inject
    private RnaSeqBaselineProfileStreamFactory rnaSeqBaselineProfileStreamFactory;

    @Inject
    private DataFileHub dataFileHub;

    private BaselineExperiment experiment;

    private CutoffScale.Scaled cutoffScale = new CutoffScale.Scaled();

    private HistogramService<BaselineProfileStreamOptions<ExpressionUnit.Absolute.Rna>, BaselineExperiment> subject;

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
        var accession= jdbcUtils.fetchRandomExpressionAtlasExperimentAccession(ExperimentType.RNASEQ_MRNA_BASELINE);
        experiment = (BaselineExperiment) experimentTrader.getExperiment(accession, "");
        rnaSeqBaselineProfileStreamFactory = Mockito.spy(new RnaSeqBaselineProfileStreamFactory(dataFileHub));
        subject = new HistogramService<>(rnaSeqBaselineProfileStreamFactory, experimentTrader, cutoffScale.get());
    }

    @Test
    void testGet() {
        var baselineProfileStreamOptions =
                new BaselineRequestContext<>(new RnaSeqBaselineRequestPreferences(), experiment);
        var result = subject.get(experiment.getAccession(), "", baselineProfileStreamOptions).asJson();

        assertThat(result.has("bins"), is(true));
        Mockito.verify(rnaSeqBaselineProfileStreamFactory)
                .histogram(experiment, baselineProfileStreamOptions, cutoffScale.get());
    }

    @Test
    void cachingWorks() {
        subject.get(
                experiment.getAccession(), "",
                new BaselineRequestContext<>(new RnaSeqBaselineRequestPreferences(), experiment)).asJson();
        subject.get(
                experiment.getAccession(), "",
                new BaselineRequestContext<>(new RnaSeqBaselineRequestPreferences(), experiment)).asJson();

        Mockito.verify(rnaSeqBaselineProfileStreamFactory, times(1))
                .histogram(eq(experiment), ArgumentMatchers.any(), eq(cutoffScale.get()));
    }
}
