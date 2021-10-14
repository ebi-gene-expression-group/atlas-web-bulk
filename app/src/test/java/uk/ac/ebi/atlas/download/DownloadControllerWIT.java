package uk.ac.ebi.atlas.download;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.atlas.configuration.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadControllerWIT {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    void downloadReturnsValidModel() throws Exception {
        this.mockMvc.perform(get("/download"))
                .andExpect(status().isOk())
                .andExpect(view().name("download"));
    }

    @Test
    void downloadModelHaveFtpFileInfo() throws Exception {
        this.mockMvc.perform(get("/download"))
                .andExpect(status().isOk())
                .andExpect(view().name("download"))
                .andExpect(model().attributeExists("fileName", "fileSize", "fileTimestamp"));
    }

    @Test
    void downloadModelHaveEmptyFtpFileInfoForInvalidHost() throws Exception {
        this.mockMvc.perform(get("/download").param("ftpHost","foo"))
                .andExpect(status().isOk())
                .andExpect(view().name("download"))
                .andExpect(model().attributeDoesNotExist("fileName", "fileSize", "fileTimestamp"));
    }
}