package uk.ac.ebi.atlas.experimentpage.baseline.coexpression;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(MockitoExtension.class)
class CoexpressedGenesServiceTest {
    private final static ThreadLocalRandom RNG = ThreadLocalRandom.current();
    private final static int MAX_COEXPRESSIONS_COUNT = 1000;

    @Mock
    private CoexpressedGenesDao coexpressedGenesDao;

    private CoexpressedGenesService subject;

    @BeforeEach
    void setUp() {
        subject = new CoexpressedGenesService(coexpressedGenesDao);
    }

    @Test
    void returnsRequestedNumberOfCoexpressions() {
        var randomGeneIds = generateNonEmptyCollectionOfRandomGeneIds();
        when(coexpressedGenesDao.coexpressedGenesFor(anyString(), anyString())).thenReturn(randomGeneIds);

        var requestedNumberOfCoexpressions = RNG.nextInt(MAX_COEXPRESSIONS_COUNT);
        assertThat(
                subject.fetchCoexpressions(
                        generateRandomExperimentAccession(),
                        generateRandomEnsemblGeneId(),
                        requestedNumberOfCoexpressions))
                .hasSize(Math.min(randomGeneIds.size(), requestedNumberOfCoexpressions));
    }

    @Test
    void ifNoCoexpressionsCanBeFoundReturnEmptyList() {
        when(coexpressedGenesDao.coexpressedGenesFor(anyString(), anyString())).thenReturn(ImmutableList.of());
        assertThat(
                subject.fetchCoexpressions(
                        generateRandomExperimentAccession(),
                        generateRandomEnsemblGeneId(),
                        RNG.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE)))
                .isEmpty();
    }

    @Test
    void ifZeroCoexpressionsAreRequestedReturnEmptyList() {
        when(coexpressedGenesDao.coexpressedGenesFor(anyString(), anyString()))
                .thenReturn(generateNonEmptyCollectionOfRandomGeneIds());
        assertThat(subject.fetchCoexpressions(generateRandomExperimentAccession(), generateRandomEnsemblGeneId(), 0))
                .isEmpty();
    }

    // Maybe this one is overly defensive
    @Test
    void ifNegativeAmountOfCoexpressionsAreRequestedReturnEmptyList() {
        when(coexpressedGenesDao.coexpressedGenesFor(anyString(), anyString()))
                .thenReturn(generateNonEmptyCollectionOfRandomGeneIds());
        assertThat(
                subject.fetchCoexpressions(
                        generateRandomExperimentAccession(),
                        generateRandomEnsemblGeneId(),
                        RNG.nextInt(Integer.MIN_VALUE, -1)))
                .isEmpty();
    }

    private ImmutableList<String> generateNonEmptyCollectionOfRandomGeneIds() {
        return IntStream.range(0, RNG.nextInt(1, MAX_COEXPRESSIONS_COUNT))
                .boxed()
                .map(__ -> generateRandomEnsemblGeneId())
                .collect(toImmutableList());
    }
}
