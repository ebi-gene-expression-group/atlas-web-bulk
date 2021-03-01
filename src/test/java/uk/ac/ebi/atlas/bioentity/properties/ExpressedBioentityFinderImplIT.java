package uk.ac.ebi.atlas.bioentity.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
@WebAppConfiguration
class ExpressedBioentityFinderImplIT {
    @Inject
    private ExpressedBioentityFinderImpl subject;

    @Test
    void unknownGeneIdIsNotExpressed() {
        assertThat(subject.bioentityIsExpressedInAtLeastOneExperiment(generateRandomEnsemblGeneId()))
                .isFalse();
    }
}
