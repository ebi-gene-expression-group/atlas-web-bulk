package uk.ac.ebi.atlas.utils;

import org.hamcrest.Matchers;
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
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.trader.ConfigurationTrader;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExperimentSorterIT {
    @Inject
    private DataSource dataSource;

    @Inject
    private DataFileHub dataFileHub;

    @Inject
    private ExperimentTrader experimentTrader;

    @Inject
    private ConfigurationTrader configurationTrader;

    private ExperimentSorter subject;

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
        subject = new ExperimentSorter(dataFileHub, experimentTrader, configurationTrader);
    }

    @Test
    void when_all_experiments_sorted_reverse_by_size_then_sorting_correct_by_size() {
        var sortedExperiments = subject.reverseSortAllExperimentsPerSize();

        // Check that sizes are in descending order
        var sizes = new ArrayList<>(sortedExperiments.keySet());

        // Skip the check if there's only one size
        if (sizes.size() > 1) {
            IntStream.range(0, sizes.size() - 1)
                .forEach(i -> assertTrue(sizes.get(i) >= sizes.get(i + 1),
                    "Experiments should be sorted in descending order by size"));
        }
    }

    @Test
    public void when_all_experiments_sorted_reverse_by_size_then_sorted_experiments_contains_all_public_experiments() {
        var sizeOfPublicExperiments = experimentTrader.getPublicExperiments().size();
        var sortedExperiments = subject.reverseSortAllExperimentsPerSize().values();
        assertThat(sortedExperiments.size(), Matchers.equalTo(sizeOfPublicExperiments));
    }
}
