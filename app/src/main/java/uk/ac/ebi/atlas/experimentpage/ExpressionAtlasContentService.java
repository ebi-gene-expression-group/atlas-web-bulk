package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.differential.download.DifferentialSecondaryDataFiles;
import uk.ac.ebi.atlas.experimentpage.link.LinkToArrayExpress;
import uk.ac.ebi.atlas.experimentpage.link.LinkToEga;
import uk.ac.ebi.atlas.experimentpage.link.LinkToEna;
import uk.ac.ebi.atlas.experimentpage.link.LinkToGeo;
import uk.ac.ebi.atlas.experimentpage.link.LinkToPride;
import uk.ac.ebi.atlas.experimentpage.qc.RnaSeqQcReport;
import uk.ac.ebi.atlas.model.download.ExternallyAvailableContent;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.resource.ContrastImageSupplier;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import static com.google.common.collect.ImmutableList.toImmutableList;
import java.util.function.Function;

@Component
public class ExpressionAtlasContentService {
    private final ExternallyAvailableContentService<BaselineExperiment>
            proteomicsBaselineExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<BaselineExperiment>
            rnaSeqBaselineExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<DifferentialExperiment>
            bulkDifferentialExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<MicroarrayExperiment>
            microarrayExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<BaselineExperiment>
            rnaSeqBaselineExperimentExternallyAvailableContentServiceGeo;
    private final ExternallyAvailableContentService<DifferentialExperiment>
            rnaSeqDifferentialExperimentExternallyAvailableContentServiceGeo;
    private final ExternallyAvailableContentService<MicroarrayExperiment>
            microarrayExperimentExternallyAvailableContentServiceGeo;
    private final ExternallyAvailableContentService<BaselineExperiment>
            rnaSeqBaselineExperimentExternallyAvailableContentServiceEna;
    private final ExternallyAvailableContentService<DifferentialExperiment>
            bulkDifferentialExperimentExternallyAvailableContentServiceEna;
    private final ExternallyAvailableContentService<MicroarrayExperiment>
            microarrayExperimentExternallyAvailableContentServiceEna;
    private final ExternallyAvailableContentService<BaselineExperiment>
            bulkBaselineExperimentExternallyAvailableContentServiceEga;
    private final ExternallyAvailableContentService<DifferentialExperiment>
            bulkDifferentialExperimentExternallyAvailableContentServiceEga;
    private final ExternallyAvailableContentService<MicroarrayExperiment>
            microarrayExperimentExternallyAvailableContentServiceEga;
    private final ExperimentTrader experimentTrader;

    public ExpressionAtlasContentService(
            ExperimentDownloadSupplier.Proteomics proteomicsExperimentDownloadSupplier,
            ExperimentDownloadSupplier.RnaSeqBaseline rnaSeqBaselineExperimentDownloadSupplier,
            ExperimentDownloadSupplier.BulkDifferential bulkDifferential,
            ExperimentDownloadSupplier.Microarray microarrayExperimentDownloadSupplier,
            ContrastImageSupplier.RnaSeq rnaSeqDifferentialContrastImageSupplier,
            ContrastImageSupplier.Microarray microarrayContrastImageSupplier,
            StaticFilesDownload.Baseline baselineStaticFilesDownload,
            StaticFilesDownload.RnaSeq rnaSeqDifferentialStaticFilesDownload,
            StaticFilesDownload.Microarray microarrayStaticFilesDownload,
            DifferentialSecondaryDataFiles.RnaSeq rnaSeqDifferentialSecondaryDataFiles,
            DifferentialSecondaryDataFiles.Microarray microarraySecondaryDataFiles,
            ExperimentDesignFile.Baseline baselineExperimentDesignFile,
            ExperimentDesignFile.RnaSeq rnaSeqDifferentialExperimentDesignFile,
            ExperimentDesignFile.Microarray microarrayExperimentDesignFile,
            RnaSeqQcReport rnaSeqQCReport,
            LinkToArrayExpress.RnaSeqBaseline rnaSeqBaselineLinkToArrayExpress,
            LinkToArrayExpress.ProteomicsBaseline proteomicsBaselineLinkToArrayExpress,
            LinkToArrayExpress.Differential differentialLinkToArrayExpress,
            LinkToArrayExpress.Microarray microarrayLinkToArrayExpress,
            LinkToPride linkToPride,
            LinkToEna.RnaSeqBaseline rnaSeqBaselineLinkToEna,
            LinkToEna.Differential differentialLinkToEna,
            LinkToEna.Microarray microarrayLinkToEna,
            LinkToEga.RnaSeqBaseline rnaSeqBaselineLinkToEga,
            LinkToEga.Differential differentialLinkToEga,
            LinkToEga.Microarray microarrayLinkToEga,
            LinkToGeo.RnaSeqBaseline rnaSeqBaselineLinkToGeo,
            LinkToGeo.Differential differentialLinkToGeo,
            LinkToGeo.Microarray microarrayLinkToGeo,
            ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;

        this.proteomicsBaselineExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                proteomicsExperimentDownloadSupplier,
                                baselineStaticFilesDownload,
                                baselineExperimentDesignFile,
                                linkToPride,
                                proteomicsBaselineLinkToArrayExpress));

        this.rnaSeqBaselineExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                rnaSeqBaselineExperimentDownloadSupplier,
                                baselineStaticFilesDownload,
                                baselineExperimentDesignFile,
                                rnaSeqBaselineLinkToArrayExpress));

        this.bulkDifferentialExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                bulkDifferential,
                                rnaSeqDifferentialSecondaryDataFiles,
                                rnaSeqDifferentialStaticFilesDownload,
                                rnaSeqDifferentialExperimentDesignFile,
                                rnaSeqQCReport,
                                differentialLinkToArrayExpress,
                                rnaSeqDifferentialContrastImageSupplier));

        this.microarrayExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                microarrayExperimentDownloadSupplier,
                                microarraySecondaryDataFiles,
                                microarrayStaticFilesDownload,
                                microarrayExperimentDesignFile,
                                microarrayLinkToArrayExpress,
                                microarrayContrastImageSupplier));

        this.rnaSeqBaselineExperimentExternallyAvailableContentServiceGeo =
                new ExternallyAvailableContentService<>(ImmutableList.of(rnaSeqBaselineLinkToGeo));

        this.rnaSeqDifferentialExperimentExternallyAvailableContentServiceGeo =
                new ExternallyAvailableContentService<>(ImmutableList.of(differentialLinkToGeo));

        this.microarrayExperimentExternallyAvailableContentServiceGeo =
                new ExternallyAvailableContentService<>(ImmutableList.of(microarrayLinkToGeo));

        this.rnaSeqBaselineExperimentExternallyAvailableContentServiceEna =
                new ExternallyAvailableContentService<>(ImmutableList.of(rnaSeqBaselineLinkToEna));

        this.bulkDifferentialExperimentExternallyAvailableContentServiceEna =
                new ExternallyAvailableContentService<>(ImmutableList.of(differentialLinkToEna));

        this.microarrayExperimentExternallyAvailableContentServiceEna =
                new ExternallyAvailableContentService<>(ImmutableList.of(microarrayLinkToEna));

        this.bulkBaselineExperimentExternallyAvailableContentServiceEga =
                new ExternallyAvailableContentService<>(ImmutableList.of(rnaSeqBaselineLinkToEga));

        this.bulkDifferentialExperimentExternallyAvailableContentServiceEga =
                new ExternallyAvailableContentService<>(ImmutableList.of(differentialLinkToEga));

        this.microarrayExperimentExternallyAvailableContentServiceEga =
                new ExternallyAvailableContentService<>(ImmutableList.of(microarrayLinkToEga));
    }

    public Function<HttpServletResponse, Void> stream(String experimentAccession, String accessKey, final URI uri) {
        Experiment<?> experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        if (experiment.getType().isProteomicsBaseline()) {
            return proteomicsBaselineExperimentExternallyAvailableContentService.stream(
                    (BaselineExperiment) experiment, uri);
        } else if (experiment.getType().isRnaSeqBaseline()) {
            return rnaSeqBaselineExperimentExternallyAvailableContentService.stream(
                    (BaselineExperiment) experiment, uri);
        } else if (experiment.getType().isBulkDifferential()) {
            return bulkDifferentialExperimentExternallyAvailableContentService.stream(
                    (DifferentialExperiment) experiment, uri);
        } else {
            return microarrayExperimentExternallyAvailableContentService.stream(
                    (MicroarrayExperiment) experiment, uri);
        }
    }

    public ImmutableList<ExternallyAvailableContent> list(String experimentAccession,
                                                 String accessKey,
                                                 ExternallyAvailableContent.ContentType contentType) {
        Experiment<?> experiment = experimentTrader.getExperiment(experimentAccession, accessKey);
        String externalResourceType = externalResourceLinksPriority(experiment);
        ImmutableList.Builder<ExternallyAvailableContent> arrayExpressAndOtherExternalResourcesLinks = ImmutableList.builder();
        ImmutableList.Builder<ExternallyAvailableContent> otherExternalResourceLinks = ImmutableList.builder();

        switch (experiment.getType()) {
            case PROTEOMICS_BASELINE:
                arrayExpressAndOtherExternalResourcesLinks.addAll(proteomicsBaselineExperimentExternallyAvailableContentService.list(
                        (BaselineExperiment) experiment, contentType));
                break;
            case RNASEQ_MRNA_BASELINE:
                arrayExpressAndOtherExternalResourcesLinks.addAll(rnaSeqBaselineExperimentExternallyAvailableContentService.list(
                        (BaselineExperiment) experiment, contentType));
                otherExternalResourceLinks.addAll(externalResourceType.equals("geo") ?
                        rnaSeqBaselineExperimentExternallyAvailableContentServiceGeo.list(
                                (BaselineExperiment) experiment, contentType) :
                        externalResourceType.equals("ega") ?
                                bulkBaselineExperimentExternallyAvailableContentServiceEga.list(
                                        (BaselineExperiment) experiment, contentType) :
                                rnaSeqBaselineExperimentExternallyAvailableContentServiceEna.list(
                                        (BaselineExperiment) experiment, contentType));
                arrayExpressAndOtherExternalResourcesLinks.addAll(otherExternalResourceLinks.build());
                break;
            case RNASEQ_MRNA_DIFFERENTIAL:
            case PROTEOMICS_DIFFERENTIAL:
                arrayExpressAndOtherExternalResourcesLinks.addAll(bulkDifferentialExperimentExternallyAvailableContentService.list(
                        (DifferentialExperiment) experiment, contentType));
                otherExternalResourceLinks.addAll(externalResourceType.equals("geo") ?
                        rnaSeqDifferentialExperimentExternallyAvailableContentServiceGeo.list(
                                (DifferentialExperiment) experiment, contentType) :
                        externalResourceType.equals("ega") ?
                                bulkDifferentialExperimentExternallyAvailableContentServiceEga.list(
                                        (DifferentialExperiment) experiment, contentType) :
                                bulkDifferentialExperimentExternallyAvailableContentServiceEna.list(
                                        (DifferentialExperiment) experiment, contentType));
                arrayExpressAndOtherExternalResourcesLinks.addAll(otherExternalResourceLinks.build());
                break;
            case MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL:
            case MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL:
            case MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL:
                arrayExpressAndOtherExternalResourcesLinks.addAll(microarrayExperimentExternallyAvailableContentService.list(
                        (MicroarrayExperiment) experiment, contentType));
                otherExternalResourceLinks.addAll(externalResourceType.equals("geo") ?
                        microarrayExperimentExternallyAvailableContentServiceGeo.list(
                                (MicroarrayExperiment) experiment, contentType) :
                        externalResourceType.equals("ega") ?
                                microarrayExperimentExternallyAvailableContentServiceEga.list(
                                        (MicroarrayExperiment) experiment, contentType) :
                                microarrayExperimentExternallyAvailableContentServiceEna.list(
                                        (MicroarrayExperiment) experiment, contentType));
                arrayExpressAndOtherExternalResourcesLinks.addAll(otherExternalResourceLinks.build());
                break;
            default:
                throw new IllegalArgumentException(experiment.getType() + ": experiment type not supported.");
        }

        return arrayExpressAndOtherExternalResourcesLinks.build();
    }

    private String externalResourceLinksPriority(Experiment<?> experiment) {
        var geoAccessions = experiment.getSecondaryAccessions().stream()
                .filter(accession -> accession.matches("GSE.*"))
                .collect(toImmutableList());
        var egaAccessions = experiment.getSecondaryAccessions().stream()
                .filter(accession -> accession.matches("EGA.*"))
                .collect(toImmutableList());

        return geoAccessions.isEmpty() ? egaAccessions.isEmpty() ? "ena" : "ega" : "geo";
    }
}
