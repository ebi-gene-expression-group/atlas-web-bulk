package uk.ac.ebi.atlas.trader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrudDao;
import uk.ac.ebi.atlas.experimentimport.ExperimentDto;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParserOutput;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesign;
import uk.ac.ebi.atlas.trader.factory.BaselineExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.MicroarrayExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.RnaSeqDifferentialExperimentFactory;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomDoi;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomPubmedId;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomSpecies;

@ExtendWith(MockitoExtension.class)
class GxaExperimentRepositoryTest {
    private static final Random RNG = ThreadLocalRandom.current();

    @Mock
    private ExperimentCrudDao experimentCrudDaoMock;

    @Mock
    private ExperimentDesignParser experimentDesignParserMock;

    @Mock
    private IdfParser idfParserMock;

    @Mock
    private BaselineExperimentFactory baselineExperimentFactoryMock;

    @Mock
    private RnaSeqDifferentialExperimentFactory rnaSeqDifferentialExperimentFactoryMock;

    @Mock
    private MicroarrayExperimentFactory microarrayExperimentFactoryMock;

    private GxaExperimentRepository subject;

    @BeforeEach
    void setUp() {
        subject =
                new GxaExperimentRepository(
                        experimentCrudDaoMock,
                        experimentDesignParserMock,
                        idfParserMock,
                        baselineExperimentFactoryMock,
                        rnaSeqDifferentialExperimentFactoryMock,
                        microarrayExperimentFactoryMock);
    }

    @Test
    void cannotBuildNonBulkExperiments() {
        var experimentAccession = generateRandomExperimentAccession();

        when(experimentCrudDaoMock.readExperiment(experimentAccession))
                .thenReturn(new ExperimentDto(
                        experimentAccession,
                        SINGLE_CELL_RNASEQ_MRNA_BASELINE,
                        generateRandomSpecies().getName(),
                        ImmutableList.of(generateRandomPubmedId()),
                        ImmutableList.of(generateRandomDoi()),
                        new Timestamp(new Date().getTime()),
                        new Timestamp(new Date().getTime()),
                        RNG.nextBoolean(),
                        UUID.randomUUID().toString()));

        when(experimentDesignParserMock.parse(experimentAccession))
                .thenReturn(new ExperimentDesign());
        when(idfParserMock.parse(experimentAccession))
                .thenReturn(new IdfParserOutput(
                        randomAlphanumeric(500),
                        ImmutableSet.of(),
                        randomAlphanumeric(100),
                        ImmutableList.of(),
                        RNG.nextInt(50),
                        ImmutableList.of()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> subject.getExperiment(experimentAccession));
    }
}
