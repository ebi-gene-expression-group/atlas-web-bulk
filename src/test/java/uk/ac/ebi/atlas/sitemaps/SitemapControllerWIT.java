package uk.ac.ebi.atlas.sitemaps;

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
import uk.ac.ebi.atlas.species.SpeciesPropertiesTrader;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SitemapControllerWIT {
    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private SpeciesPropertiesTrader speciesPropertiesTrader;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void globalSitemapOfSitemaps() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(xpath("/sitemapindex//sitemap").nodeCount(speciesPropertiesTrader.getAll().size()));
    }

    @Test
    @Sql("/fixtures/experiment-fixture.sql")
    void sitemapOfSpecies() throws Exception {
        mockMvc.perform(get("/species/{species}/sitemap.xml",
                            speciesPropertiesTrader.get(jdbcUtils.fetchRandomSpecies()).ensemblName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_XML_VALUE))
                .andExpect(xpath("/urlset//url").nodeCount(greaterThan(0)));
    }
}
