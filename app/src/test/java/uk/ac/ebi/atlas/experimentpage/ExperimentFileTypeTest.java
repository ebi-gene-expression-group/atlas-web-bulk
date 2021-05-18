package uk.ac.ebi.atlas.experimentpage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ExperimentFileTypeTest {
    @ParameterizedTest
    @MethodSource("stringAndExperimentFileTypeProvider")
    void fileTypeFromValidId(String fileTypeId, ExperimentFileType fileType) {
        assertThat(ExperimentFileType.fromId(fileTypeId)).isEqualByComparingTo(fileType);
    }

    @Test
    void fileTypeFromEmptyIdThrowsException() {
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> ExperimentFileType.fromId(""));

    }

    @Test
    void fileTypeFromNonexistentIdThrowsException() {
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> ExperimentFileType.fromId("foo-bar"));

    }

    private static Stream<Arguments> stringAndExperimentFileTypeProvider() {
        return Stream.of(
                Arguments.of("configuration", ExperimentFileType.CONFIGURATION),
                Arguments.of("condensed-sdrf", ExperimentFileType.CONDENSE_SDRF),
                Arguments.of("idf", ExperimentFileType.IDF),
                Arguments.of("factors", ExperimentFileType.BASELINE_FACTORS),
                Arguments.of("main", ExperimentFileType.PROTEOMICS_B_MAIN),
                Arguments.of("tpm", ExperimentFileType.RNASEQ_B_TPM),
                Arguments.of("rnaseq-analytics", ExperimentFileType.RNASEQ_D_ANALYTICS),
                Arguments.of("microarray-analytics", ExperimentFileType.MICROARRAY_D_ANALYTICS)
        );
    }
}
