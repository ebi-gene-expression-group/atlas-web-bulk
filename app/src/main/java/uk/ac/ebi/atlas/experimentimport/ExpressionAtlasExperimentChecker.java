package uk.ac.ebi.atlas.experimentimport;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.commons.streams.ObjectInputStream;
import uk.ac.ebi.atlas.model.experiment.sample.BiologicalReplicate;
import uk.ac.ebi.atlas.model.experiment.sample.ReportsGeneExpression;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.model.resource.AtlasResource;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.trader.ConfigurationTrader;
import uk.ac.ebi.atlas.utils.StringArrayUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

@Component
public class ExpressionAtlasExperimentChecker implements ExperimentChecker {
    private final DataFileHub dataFileHub;
    private final ConfigurationTrader configurationTrader;

    @Inject
    public ExpressionAtlasExperimentChecker(DataFileHub dataFileHub,
                                            ConfigurationTrader configurationTrader) {
        this.dataFileHub = dataFileHub;
        this.configurationTrader = configurationTrader;
    }

    @Override
    public void checkAllFiles(String experimentAccession, ExperimentType experimentType) {
        // every experiment should have configuration, condensed SDRF and analysis methods file
        checkConfigurationFile(experimentAccession);
        checkResourceExistsAndIsReadable(dataFileHub.getExperimentFiles(experimentAccession).analysisMethods);
        checkResourceExistsAndIsReadable(dataFileHub.getExperimentFiles(experimentAccession).condensedSdrf);

        switch (experimentType) {
            case RNASEQ_MRNA_BASELINE:
                checkRnaSeqBaselineFiles(experimentAccession);
                break;
            case PROTEOMICS_BASELINE:
            case PROTEOMICS_BASELINE_DIA:
                checkProteomicsBaselineFiles(experimentAccession);
                break;
            case PROTEOMICS_DIFFERENTIAL:
                checkDifferentialFiles(experimentAccession, false);
                break;
            case RNASEQ_MRNA_DIFFERENTIAL:
                checkDifferentialFiles(experimentAccession, true);
                break;
            case MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL:
            case MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL:
                checkMicroarray1ColourFiles(experimentAccession,
                        configurationTrader.getExperimentConfiguration(experimentAccession)
                                .getArrayDesignAccessions());
                break;
            case MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL:
                checkMicroarray2ColourFiles(experimentAccession,
                        configurationTrader.getExperimentConfiguration(experimentAccession)
                                .getArrayDesignAccessions());
                break;
            default:
                throw new IllegalArgumentException("The specified experiment type is not supported.");
        }
    }

    void checkRnaSeqBaselineFiles(String experimentAccession) {
        var experimentFiles = dataFileHub.getRnaSeqBaselineExperimentFiles(experimentAccession);
        checkBaselineFiles(experimentFiles.baselineExperimentFiles);
        var dataFiles = experimentFiles.dataFiles();
        Preconditions.checkState(
                dataFiles.size() > 0,
                MessageFormat.format("No data files (FPKM/TPM) present for {0}!", experimentAccession));
        for (var dataFile: dataFiles) {
            checkResourceExistsAndIsReadable(experimentFiles.dataFile(dataFile));
            assayGroupIdsInHeaderMatchConfigurationXml(
                    rnaSeqIdsFromHeader(extractFirstElement(experimentFiles.dataFile(dataFile))), experimentAccession);
        }

        var transcripts = experimentFiles.transcriptsTpms;
        if (transcripts.exists()) {
            biologicalReplicateIdsInHeaderMatchConfigurationXml(
                    transcriptIdsFromHeader(extractFirstElement(transcripts)), experimentAccession);
        }
    }

    private String[] rnaSeqIdsFromHeader(String[] header) {
        return ArrayUtils.subarray(header, 2, header.length);
    }

    private String[] transcriptIdsFromHeader(String[] header) {
        return ArrayUtils.subarray(header, 3, header.length);
    }

    void checkProteomicsBaselineFiles(String experimentAccession) {
        var experimentFiles = dataFileHub.getProteomicsBaselineExperimentFiles(experimentAccession);
        checkBaselineFiles(experimentFiles.baselineExperimentFiles);
        checkResourceExistsAndIsReadable(experimentFiles.main);
        assayGroupIdsInHeaderMatchConfigurationXml(
                proteomicsIdsFromHeader(extractFirstElement(experimentFiles.main)), experimentAccession);
    }

    String[] proteomicsIdsFromHeader(String[] header) {
        return StringArrayUtil.substringBefore(StringArrayUtil.filterBySubstring(header, "WithInSampleAbundance"), ".");
    }

    private void biologicalReplicateIdsInHeaderMatchConfigurationXml(String[] biologicalReplicateIds,
                                                                     String experimentAccession) {
        var idsInConfiguration =
                configurationTrader.getExperimentConfiguration(experimentAccession).getAssayGroups().stream()
                        .flatMap(a -> a.getAssays().stream())
                        .map(BiologicalReplicate::getId)
                        .collect(Collectors.toSet());
        Preconditions.checkState(
                ImmutableSet.copyOf(biologicalReplicateIds).equals(idsInConfiguration),
                MessageFormat.format(
                        "Biological replicate IDs in data file (#:{1}) not matching ids in " +
                        "{0}-configuration.xml (#:{2})",
                        experimentAccession, biologicalReplicateIds.length, idsInConfiguration.size()));
    }

    private void assayGroupIdsInHeaderMatchConfigurationXml(String[] assayGroupIds, String experimentAccession) {
        var idsInConfiguration =
                configurationTrader.getExperimentConfiguration(experimentAccession).getAssayGroups().stream()
                        .map(ReportsGeneExpression::getId)
                        .collect(Collectors.toSet());
        Preconditions.checkState(
                ImmutableSet.copyOf(assayGroupIds).equals(idsInConfiguration),
                MessageFormat.format(
                        "Assay group ids in data file (#:{1}) not matching ids in " +
                        "{0}-configuration.xml (#:{2})",
                        experimentAccession, assayGroupIds.length, idsInConfiguration.size()));
    }


    private void checkBaselineFiles(DataFileHub.BaselineExperimentFiles experimentFiles) {
        checkResourceExistsAndIsReadable(experimentFiles.factors);
    }

    private void checkDifferentialFiles(String experimentAccession, boolean checkRawCounts) {
        var experimentFiles = dataFileHub.getBulkDifferentialExperimentFiles(experimentAccession);
        checkResourceExistsAndIsReadable(experimentFiles.analytics);
        if (checkRawCounts) {
            checkResourceExistsAndIsReadable(experimentFiles.rawCounts);
        }
    }

    private void checkMicroarray1ColourFiles(String experimentAccession, Set<String> arrayDesigns) {
        for (var arrayDesign : arrayDesigns) {
            var experimentFiles = dataFileHub.getMicroarrayExperimentFiles(experimentAccession, arrayDesign);

            checkResourceExistsAndIsReadable(experimentFiles.analytics);
            checkResourceExistsAndIsReadable(experimentFiles.normalizedExpressions);
        }
    }

    private void checkMicroarray2ColourFiles(String experimentAccession, Set<String> arrayDesigns) {
        for (var arrayDesign : arrayDesigns) {
            var experimentFiles = dataFileHub.getMicroarrayExperimentFiles(experimentAccession, arrayDesign);

            checkResourceExistsAndIsReadable(experimentFiles.analytics);
            checkResourceExistsAndIsReadable(experimentFiles.logFoldChanges);
        }
    }

    private void checkConfigurationFile(String accession) {
        checkResourceExistsAndIsReadable(dataFileHub.getExperimentFiles(accession).configuration);
    }

    private void checkResourceExistsAndIsReadable(AtlasResource<?> resource) {
        checkState(resource.exists(), "Required file does not exist: " + resource.toString());
        checkState(resource.isReadable(), "Required file can not be read: " + resource.toString());
    }

    private <T> T extractFirstElement(AtlasResource<ObjectInputStream<T>> resource) {
        ObjectInputStream<T> stream = resource.get();
        T first = stream.readNext();
        try {
            stream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return first;
    }
}
