package uk.ac.ebi.atlas.sitemaps;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static uk.ac.ebi.atlas.sitemaps.SitemapWriter.SITEMAP_URL_MAX_COUNT;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@Transactional
@Sql("/fixtures/experiment-fixture.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SitemapControllerWIT {
    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private SpeciesFactory speciesFactory;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void globalSitemapOfSitemaps() throws Exception {
        var publicReferenceSpecies =
                jdbcUtils.fetchPublicSpecies().stream()
                        .map(speciesFactory::create)
                        .map(Species::getReferenceName)
                        .collect(toImmutableSet());

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_XML_VALUE))
                // Adding one for /experiments/sitemap.xml
                .andExpect(xpath("/sitemapindex//sitemap").nodeCount(publicReferenceSpecies.size() + 1));
    }

    @Test
    void sitemapOfExperiments() throws Exception {
        var experimentSitemaps = ImmutableList.of("/experiments", "/baseline/experiments", "/plant/experiments");

        mockMvc.perform(get("/experiments/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(xpath("/urlset//url").nodeCount(experimentSitemaps.size()));
    }

    @Test
    void sitemapOfSpecies() throws Exception {
        mockMvc.perform(get("/species/{species}/sitemap.xml",
                            speciesFactory.create(jdbcUtils.fetchRandomPublicSpecies()).getEnsemblName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(xpath("/urlset//url").nodeCount(lessThanOrEqualTo(SITEMAP_URL_MAX_COUNT)));
    }

    @Test
    void sitemapOfSpeciesForAllEntries() throws Exception {
        mockMvc.perform(get("/species/{species}/sitemap.xml",
                            speciesFactory.create("Homo_sapiens").getEnsemblName())
                        .param("allEntries", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(xpath("/urlset//url").nodeCount(greaterThan(SITEMAP_URL_MAX_COUNT)));
    }
}
