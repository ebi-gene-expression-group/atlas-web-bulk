package uk.ac.ebi.atlas.widget;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.search.SemanticQuery;
import uk.ac.ebi.atlas.solr.analytics.baseline.BaselineAnalyticsSearchService;
import uk.ac.ebi.atlas.solr.analytics.differential.DifferentialAnalyticsSearchService;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesProperties;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BaselineAndDifferentialAnalyticsServiceIT {
    private static final SemanticQuery EMPTY_QUERY = SemanticQuery.create();

    static final String BASELINE_GENE = "ENSG00000000003";
    static final String DIFFERENTIAL_GENE = "ENSSSCG00000000024";
    static final String NON_EXISTENT_GENE = "FOOBAR";

    @Inject
    DataSource dataSource;

    @Inject
    private BaselineAnalyticsSearchService baselineAnalyticsSearchService;

    @Inject
    private DifferentialAnalyticsSearchService differentialAnalyticsSearchService;

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

    //These tests are really the assertions for ExpressionDataControllerEIT.
    @Test
    void geneExpressedInBaselineAndDifferentialExperiments() {
        JsonObject result = baselineAnalyticsSearchService.findFacetsForTreeSearch(SemanticQuery.create(BASELINE_GENE),
                SemanticQuery.create(), new Species("Foous baris", SpeciesProperties.UNKNOWN));
        assertThat(result.entrySet(), not(Matchers.empty()));
        Assertions.assertTrue(result.has("homo sapiens"), "This Ensembl gene has a homo sapiens result");
    }

    @Test
    void geneExpressedInDifferentialExperimentsOnly() {
        assertThat(
                baselineAnalyticsSearchService.findFacetsForTreeSearch(
                        SemanticQuery.create(DIFFERENTIAL_GENE),
                        SemanticQuery.create(),
                        new Species("Foous baris", SpeciesProperties.UNKNOWN)),
                is(new JsonObject()));

        assertThat(
                differentialAnalyticsSearchService.fetchFacets(SemanticQuery.create(DIFFERENTIAL_GENE), EMPTY_QUERY)
                        .entrySet(), not(Matchers.empty()));
    }

    @Test
    void nonExistentGene() {
        assertThat(
                baselineAnalyticsSearchService.findFacetsForTreeSearch(
                        SemanticQuery.create(NON_EXISTENT_GENE),
                        SemanticQuery.create(),
                        new Species("Foous baris", SpeciesProperties.UNKNOWN)),
                is(new JsonObject()));
        assertThat(
                differentialAnalyticsSearchService.fetchResults(SemanticQuery.create(NON_EXISTENT_GENE), EMPTY_QUERY)
                        .get("results").getAsJsonArray().size(),
                is(0));
    }

    @Test
    void differentialAnalyticsSearchServiceHasTheRightReturnFormat() {
        JsonObject result =
                differentialAnalyticsSearchService.fetchResults(SemanticQuery.create("GO:2000651"), EMPTY_QUERY);
        testDifferentialResultsAreInRightFormat(result);
    }

    private final ImmutableList<String> fieldsNeededInDifferentialResults = ImmutableList.of(
            "species",
            "kingdom",
            "experimentType",
            "numReplicates",
            "regulation",
            "factors",
            "bioentityIdentifier",
            "experimentAccession",
            "experimentName",
            "contrastId",
            "comparison",
            "foldChange",
            "pValue",
            "colour",
            "id");

    private final ImmutableList<String> fieldsNeededInMicroarrayDifferentialResults =
            ImmutableList.<String>builder()
                    .addAll(fieldsNeededInDifferentialResults)
                    .add("tStatistic")
                    .build();

    private void testDifferentialResultsAreInRightFormat(JsonObject result) {
        Assertions.assertTrue(result.has("results"), GSON.toJson(result));
        assertThat(result.get("results").getAsJsonArray().size(), greaterThan(0));

        for (JsonElement jsonElement: result.get("results").getAsJsonArray()) {
            ExperimentType experimentType =
                    ExperimentType.valueOf(jsonElement.getAsJsonObject().get("experimentType").getAsString());

            if (experimentType.isMicroarray()) {
                for (String fieldName: fieldsNeededInMicroarrayDifferentialResults) {
                    Assertions.assertTrue(jsonElement.getAsJsonObject().has(fieldName), "result has " + fieldName);
                }
            } else {
                for (String fieldName: fieldsNeededInDifferentialResults) {
                    Assertions.assertTrue(jsonElement.getAsJsonObject().has(fieldName), "result has " + fieldName);
                }
            }
        }
    }
}
