package uk.ac.ebi.atlas.experimentpage.baseline.grouping;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.model.OntologyTerm;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
class OrganismPartGroupingServiceIT {
    @Inject
    private OrganismPartGroupingService subject;

    @Test
    void testGetAnatomicalSystems() {
        assertThat(subject.getAnatomicalSystemsGrouping(ImmutableSet.of(OntologyTerm.create("UBERON_0000006"))))
                .containsKey(ColumnGroup.create("UBERON_0000949", "endocrine system"));
    }

    /*
        original distribution (count / number of overlapping systems)
        7777 1
        1610 3
        1560 2
        260 4
        99 6
        98 5
        12 7
        2 8
    */
    @Test
    void someIdsAreInMultipleTissues() {
//        assertThat(subject.getAnatomicalSystemsGrouping(ImmutableSet.of(OntologyTerm.create("UBERON_0022292"))).size())
//                .isGreaterThan(4);
        assertThat(subject.getAnatomicalSystemsGrouping(ImmutableSet.of(OntologyTerm.create("UBERON_0022292"))))
                .size().isGreaterThan(4);

    }

    @Test
    void nonexistentIdsAreInNoTissues() {
        assertThat(subject.getAnatomicalSystemsGrouping(ImmutableSet.of(OntologyTerm.create("UBERON_1234567"))))
                .isEmpty();
    }

    @Test
    void gotYourNose() {
        assertThat(
                subject.getAnatomicalSystemsGrouping(ImmutableSet.of(OntologyTerm.create("UBERON_0000004", "nose"))))
                .isNotEmpty();
        assertThat(subject.getOrgansGrouping(ImmutableSet.of(OntologyTerm.create("UBERON_0000004", "nose"))))
                .isNotEmpty();
    }
}
