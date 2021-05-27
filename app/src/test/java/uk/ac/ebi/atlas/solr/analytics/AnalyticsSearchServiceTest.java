package uk.ac.ebi.atlas.solr.analytics;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import uk.ac.ebi.atlas.search.SemanticQuery;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsSearchServiceTest {
    private static final SemanticQuery EMPTY_CONDITION_QUERY = SemanticQuery.create();
    private static final SemanticQuery ZINC_FINGER_QUERY = SemanticQuery.create("zinc finger");
    private static final SemanticQuery FOOBAR_GENE_QUERY = SemanticQuery.create("foobar");

    @Mock
    private MiscellaneousAnalyticsSearchDao miscellaneousAnalyticsSearchDaoMock;

    private AnalyticsSearchService subject;

    @BeforeEach
    void setUp() throws Exception {
        subject = new AnalyticsSearchService(miscellaneousAnalyticsSearchDaoMock);
    }

    @Test
    void findSpecies() throws Exception {
        var zincFingerGeneQuerySearchResponse =
                new ClassPathResource("/uk/ac/ebi/atlas/solr/analytics/zinc-finger-gene-query-search-response.json");
        when(miscellaneousAnalyticsSearchDaoMock.getSpecies(ZINC_FINGER_QUERY, EMPTY_CONDITION_QUERY))
                .thenReturn(
                        IOUtils.toString(zincFingerGeneQuerySearchResponse.getInputStream(), StandardCharsets.UTF_8));

        var speciesList = subject.findSpecies(ZINC_FINGER_QUERY, EMPTY_CONDITION_QUERY);

        assertThat(speciesList).first().isEqualTo("homo sapiens");
        assertThat(speciesList).last().isEqualTo("drosophila melanogaster");
        assertThat(speciesList).hasSize(22);
    }

    @Test
    void findNoSpecies() throws Exception {
        var foobarGeneQuerySearchResponse =
                new ClassPathResource("/uk/ac/ebi/atlas/solr/analytics/foobar-gene-query-search-response.json");
        when(miscellaneousAnalyticsSearchDaoMock.getSpecies(FOOBAR_GENE_QUERY, EMPTY_CONDITION_QUERY))
                .thenReturn(
                        IOUtils.toString(foobarGeneQuerySearchResponse.getInputStream(), StandardCharsets.UTF_8));

        var speciesList = subject.findSpecies(FOOBAR_GENE_QUERY, EMPTY_CONDITION_QUERY);

        System.out.println(speciesList.size());


        assertThat(speciesList).isEmpty();
    }
}
