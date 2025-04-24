package uk.ac.ebi.atlas.ebeyedump;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
class ExperimentsConditionsDetailsControllerWIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
            .alwaysDo(MockMvcResultHandlers.print())
            .build();
    }

    @Test
    void whenCallingAssayGroupsDetails_thenGenerateTsvResponse() throws Exception {
        var fileName = "assaygroupsdetails.tsv";
        this.mockMvc.perform(get("/api/" + fileName))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment;filename=" + fileName))
            .andExpect(content().contentType("text/tab-separated-values"));
    }

    @Test
    void whenCallingContrastDetails_thenGenerateTsvResponse() throws Exception {
        var fileName = "contrastdetails.tsv";
        this.mockMvc.perform(get("/api/" + fileName))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment;filename=" + fileName))
            .andExpect(content().contentType("text/tab-separated-values"));
    }
}