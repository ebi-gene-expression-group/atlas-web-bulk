package uk.ac.ebi.atlas.experimentpage;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.resource.DataFileHub;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;

@Component
@NonNullByDefault
public class ExperimentFileLocationService {
    private final DataFileHub dataFileHub;

    public ExperimentFileLocationService(DataFileHub dataFileHub) {
        this.dataFileHub = dataFileHub;
    }

    @Nullable
    public Path getFilePath(String experimentAccession, ExperimentFileType fileType) {
        switch (fileType) {
            case CONFIGURATION:
                return dataFileHub.getProteomicsBaselineExperimentFiles(experimentAccession).experimentFiles.configuration.getPath();
            case CONDENSE_SDRF:
                return dataFileHub.getProteomicsBaselineExperimentFiles(experimentAccession).experimentFiles.condensedSdrf.getPath();
            case IDF:
                return dataFileHub.getExperimentFiles(experimentAccession).idf.getPath();
            case SUMMARY_PDF:
                return dataFileHub.getExperimentFiles(experimentAccession).summaryPdf.getPath();
            case PROTEOMICS_B_MAIN:
                return dataFileHub.getProteomicsBaselineExperimentFiles(experimentAccession).main.getPath();
            case BASELINE_FACTORS:
                return dataFileHub.getBaselineExperimentFiles(experimentAccession).factors.getPath();
            case RNASEQ_B_TPM:
                return dataFileHub.getRnaSeqBaselineExperimentFiles(experimentAccession).tpms.getPath();
            case RNASEQ_D_ANALYTICS:
                return dataFileHub.getBulkDifferentialExperimentFiles(experimentAccession).analytics.getPath();
           default:
                return null;
        }
    }

    @Nullable
    public List<Path> getFilePathsForArchive(Experiment experiment, ExperimentFileType fileType) {
        switch (fileType) {
            case MICROARRAY_D_ANALYTICS:
                var paths = new ArrayList<Path>();
                paths.addAll(
                        ((MicroarrayExperiment) experiment).getArrayDesignAccessions()
                        .stream()
                        .map(arrayDesignAccession -> dataFileHub.getMicroarrayExperimentFiles(experiment.getAccession(), arrayDesignAccession).analytics.getPath())
                        .collect(toImmutableList()));
                return paths;
            default:
                return null;
        }
    }
}
