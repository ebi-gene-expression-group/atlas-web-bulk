package uk.ac.ebi.atlas.trader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE_DIA;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomExperimentAccession;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
@Sql("/fixtures/experiment-fixture.sql")
class GxaExperimentRepositoryIT {
    @Inject
    private JdbcUtils jdbcUtils;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private GxaExperimentRepository subject;

    @Test
    void throwIfExperimentCannotBeFound() {
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "experiment")).isGreaterThan(0);
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> subject.getExperiment(generateRandomExperimentAccession()));
    }

    @Test
    void baselineRnaSeqExperiments() {
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(RNASEQ_MRNA_BASELINE)))
                .isInstanceOf(BaselineExperiment.class)
                .hasNoNullFieldsOrProperties();
    }

    @Test
    void baselineProteomicsExperiments() {
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(PROTEOMICS_BASELINE)))
                .isInstanceOf(BaselineExperiment.class)
                .hasNoNullFieldsOrProperties();
    }

    @Test
    void differentialRnaSeqExperiments() {
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(RNASEQ_MRNA_DIFFERENTIAL)))
                .isInstanceOf(DifferentialExperiment.class)
                .hasNoNullFieldsOrProperties();
    }

    @Test
    void differentialProteomicsExperiments() {
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(PROTEOMICS_DIFFERENTIAL)))
                .isInstanceOf(DifferentialExperiment.class)
                .hasNoNullFieldsOrProperties();
    }

    @Test
    void differentialMicroarrayExperiments() {
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL)))
                .isInstanceOf(DifferentialExperiment.class)
                .hasNoNullFieldsOrProperties();
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL)))
                .isInstanceOf(DifferentialExperiment.class)
                .hasNoNullFieldsOrProperties();
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL)))
                .isInstanceOf(DifferentialExperiment.class)
                .hasNoNullFieldsOrProperties();
    }

    @Test
    void baselineProteomicsDiaSwathExperiments() {
        assertThat(subject.getExperiment(jdbcUtils.fetchRandomExperimentAccession(PROTEOMICS_BASELINE_DIA)))
                .isInstanceOf(BaselineExperiment.class)
                .hasNoNullFieldsOrProperties();
    }
}
