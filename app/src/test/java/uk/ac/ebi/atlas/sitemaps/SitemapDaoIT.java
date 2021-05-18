package uk.ac.ebi.atlas.sitemaps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;
import uk.ac.ebi.atlas.testutils.JdbcUtils;
import uk.ac.ebi.atlas.testutils.SolrUtils;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.solr.cloud.collections.BulkAnalyticsCollectionProxy.SPECIES;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateBlankString;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@Transactional
@Sql("/fixtures/experiment-fixture.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SitemapDaoIT {
    @Inject
    SolrUtils solrUtils;

    @Inject
    JdbcUtils jdbcUtils;

    @Inject
    SpeciesFactory speciesFactory;

    @Inject
    private SitemapDao subject;

    @Test
    void publicSpecies() {
        var publicReferenceSpecies =
                jdbcUtils.fetchPublicSpecies().stream()
                        .map(speciesFactory::create)
                        .map(Species::getReferenceName)
                        .collect(toImmutableSet());

        assertThat(subject.getSpeciesInPublicExperiments())
                .isNotEmpty()
                .hasSameSizeAs(publicReferenceSpecies);
    }

    @Test
    void speciesSitemapContainsAnyGene() {
        var randomPublicSpecies = speciesFactory.create(jdbcUtils.fetchRandomPublicSpecies()).getReferenceName();
        assertThat(subject.getBioentityIdentifiersInPublicExperiments(randomPublicSpecies))
                // These are two paths down the same road, not the richest test but I can’t think of anything better
                // until we have the genes in Postgres with the same architecture we use in SC
                .contains(solrUtils.fetchRandomGeneIdFromAnalytics(SPECIES, randomPublicSpecies));
    }

    @Test
    void publicBioentityIdentifiersIsCaseInsensitive() {
        assertThat(subject.getBioentityIdentifiersInPublicExperiments("Homo sapiens"))
                .isEqualTo(subject.getBioentityIdentifiersInPublicExperiments("Homo_sapiens"))
                .isEqualTo(subject.getBioentityIdentifiersInPublicExperiments("hOmO SapIEns"))
                .isNotEmpty();
    }

    @Test
    void unknownOrBlankSpeciesReturnsEmpty() {
        assertThat(subject.getBioentityIdentifiersInPublicExperiments(generateRandomSpecies().getReferenceName()))
                .isEqualTo(subject.getBioentityIdentifiersInPublicExperiments(generateBlankString()))
                .isEmpty();
    }

    @Test
    void isNullSafe() {
        assertThat(subject.getBioentityIdentifiersInPublicExperiments(generateRandomSpecies().getReferenceName()))
                .isEqualTo(subject.getBioentityIdentifiersInPublicExperiments(null))
                .isEmpty();
    }
}
