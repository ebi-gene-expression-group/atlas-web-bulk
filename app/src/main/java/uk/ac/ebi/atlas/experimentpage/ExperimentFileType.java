package uk.ac.ebi.atlas.experimentpage;

import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;

import java.util.Arrays;

public enum ExperimentFileType {
    // Could include icon name (similar to Description class in ExternallyAvailableContent)
    CONFIGURATION(
            "configuration", "Experiment configuration file (XML format)", IconType.EXPERIMENT_DESIGN, false),
    SUMMARY_PDF(
            "summary pdf", "PRIDE summary file (PDF format)", IconType.EXPERIMENT_DESIGN, false),
    IDF(
            "idf", "IDF file (TSV format)", IconType.TSV, false),
    CONDENSE_SDRF(
            "condensed-sdrf", "Condensed SDRF file (TSV format)", IconType.TSV, false),
    BASELINE_FACTORS(
            "factors", "Experiment configuration file (XML format)", IconType.EXPERIMENT_DESIGN, false),
    PROTEOMICS_B_MAIN(
            "main", "Experiment file (TSV format)", IconType.TSV, false),
    PROTEOMICS_RAW_QUANT(
            "raw-quant", "Unprocessed raw output file (TXT format)", IconType.TXT, false),
    PROTEOMICS_PARAMETER_FILE(
            "parameter-file", "Parameter file (XML format)", IconType.XML, false),
    RNASEQ_B_TPM(
            "tpm", "TPM file (TSV format)", IconType.TSV, false),
    PROTEOMICS_D_ANALYTICS(
            "proteomics-analytics", "Proteomics differential analytics files (TSV format)", IconType.TSV, false),
    RNASEQ_D_ANALYTICS(
            "rnaseq-analytics", "RNASeq analytics files (TSV format)", IconType.TSV, false),
    MICROARRAY_D_ANALYTICS(
            "microarray-analytics", "Microarray analytics files (TSV format)", IconType.TSV, true);

    // IDs should be used when generating URLs
    private final String id;
    private final String description;
    private final IconType iconType;
    // Indicates if the file type has more than one associated files, which should be served as an archive
    private final boolean isArchive;

    ExperimentFileType(String id, String description, IconType iconType, boolean isArchive) {
        this.id = id;
        this.description = description;
        this.iconType = iconType;
        this.isArchive = isArchive;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public IconType getIconType() {
        return iconType;
    }

    public boolean isArchive() {
        return isArchive;
    }

    public static ExperimentFileType fromId(String id) {
        return Arrays.stream(ExperimentFileType.values())
                .filter(value -> value.id.equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(
                        () -> new ResourceNotFoundException("No experiment file type with ID " + id + " was found"));
    }
}
