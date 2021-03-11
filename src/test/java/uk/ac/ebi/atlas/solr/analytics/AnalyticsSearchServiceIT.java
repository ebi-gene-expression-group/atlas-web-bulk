package uk.ac.ebi.atlas.solr.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.search.SemanticQuery;
import uk.ac.ebi.atlas.search.SemanticQueryTerm;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
class AnalyticsSearchServiceIT {
    @Inject
    private SpeciesFactory speciesFactory;

    @Inject
    private AnalyticsSearchService subject;

    private final SemanticQuery query = SemanticQuery.create("zinc finger");
    private final SemanticQuery condition = SemanticQuery.create("watering");
    private Species species;

    @BeforeEach
    void setUp() {
        species = speciesFactory.create("oryza sativa");
    }

    @Test
    public void fetchExperimentTypesForUnqualifiedGeneQuery() {
        var result = subject.fetchExperimentTypes("ENSG00000006432");
        assertThat(result).isNotEmpty();
    }

    @Test
    void fetchExperimentTypesInAnyField() {
        var result = subject.fetchExperimentTypesInAnyField(query);
        assertThat(result).isNotEmpty();
    }

    @Test
    void fetchExperimentTypesForGeneQueryWithSpecies() {
        var result = subject.fetchExperimentTypes(query, species.getReferenceName());
        assertThat(result).isNotEmpty();
    }

    @Test
    void fetchExperimentTypesForGeneQueryWithConditionQueryAndSpecies() {
        var result = subject.fetchExperimentTypes(query, condition, species.getReferenceName());
        assertThat(result).isNotEmpty();
    }

    @Test
    void searchMoreThanOneBioentityIdentifier() {
        var result = subject.searchMoreThanOneBioentityIdentifier(query, condition, species.getReferenceName());
        assertThat(result).isNotEmpty();
    }

    @Test
    void searchBioentityIdentifiers() {
        var result = subject.searchBioentityIdentifiers(query, condition, species.getReferenceName());
        assertThat(result).isNotEmpty();
    }

    @Test
    void tissueExpressionAvailableFor() {
        var result = subject.tissueExpressionAvailableInBaselineExperiments(query);
        assertThat(result).isTrue();
    }

    @Test
    void speciesOfEmptyQuery() {
        var speciesList = subject.findSpecies(SemanticQuery.create(), SemanticQuery.create());
        assertThat(speciesList).isNotEmpty();
    }

    @Test
    void speciesWhenNoResults() {
        var foobarQueryTerm = SemanticQueryTerm.create("Foo", "Bar");
        var speciesList = subject.findSpecies(SemanticQuery.create(), SemanticQuery.create(foobarQueryTerm));
        assertThat(speciesList).isEmpty();
    }

    @Test
    void speciesSpecificSearch() {
        var reactomeQueryTerm = SemanticQueryTerm.create("R-MMU-69002", "pathwayid");
        var speciesList = subject.findSpecies(SemanticQuery.create(reactomeQueryTerm), SemanticQuery.create());
        assertThat(speciesList).containsExactly("mus musculus");
    }

    @Test
    void multipleSpeciesSearch() {
        var reactomeQueryTerm = SemanticQueryTerm.create("GO:0008150", "go");
        var speciesList = subject.findSpecies(SemanticQuery.create(reactomeQueryTerm), SemanticQuery.create());
        assertThat(speciesList).isNotEmpty();
        assertThat(speciesFactory.create(speciesList.get(0)).isUnknown()).isFalse();
    }
}
