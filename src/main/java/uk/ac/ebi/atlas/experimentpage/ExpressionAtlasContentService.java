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
import java.util.List;
import static com.google.common.collect.ImmutableList.toImmutableList;
import java.util.function.Function;

@Component
public class ExpressionAtlasContentService {
    private final ExternallyAvailableContentService<BaselineExperiment>
            proteomicsBaselineExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<BaselineExperiment>
            rnaSeqBaselineExperimentExternallyAvailableContentService;
    private final ExternallyAvailableContentService<DifferentialExperiment>
            rnaSeqDifferentialExperimentExternallyAvailableContentService;
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
            rnaSeqDifferentialExperimentExternallyAvailableContentServiceEna;
    private final ExternallyAvailableContentService<MicroarrayExperiment>
            microarrayExperimentExternallyAvailableContentServiceEna;
    private final ExternallyAvailableContentService<BaselineExperiment>
            rnaSeqBaselineExperimentExternallyAvailableContentServiceEga;
    private final ExternallyAvailableContentService<DifferentialExperiment>
            rnaSeqDifferentialExperimentExternallyAvailableContentServiceEga;
    private final ExternallyAvailableContentService<MicroarrayExperiment>
            microarrayExperimentExternallyAvailableContentServiceEga;
    private final ExperimentTrader experimentTrader;

    public ExpressionAtlasContentService(
            ExperimentDownloadSupplier.Proteomics proteomicsExperimentDownloadSupplier,
            ExperimentDownloadSupplier.RnaSeqBaseline rnaSeqBaselineExperimentDownloadSupplier,
            ExperimentDownloadSupplier.RnaSeqDifferential rnaSeqDifferentialExperimentDownloadSupplier,
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

        this.rnaSeqDifferentialExperimentExternallyAvailableContentService =
                new ExternallyAvailableContentService<>(
                        ImmutableList.of(
                                rnaSeqDifferentialExperimentDownloadSupplier,
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

        this.rnaSeqDifferentialExperimentExternallyAvailableContentServiceEna =
                new ExternallyAvailableContentService<>(ImmutableList.of(differentialLinkToEna));

        this.microarrayExperimentExternallyAvailableContentServiceEna =
                new ExternallyAvailableContentService<>(ImmutableList.of(microarrayLinkToEna));

        this.rnaSeqBaselineExperimentExternallyAvailableContentServiceEga =
                new ExternallyAvailableContentService<>(ImmutableList.of(rnaSeqBaselineLinkToEga));

        this.rnaSeqDifferentialExperimentExternallyAvailableContentServiceEga =
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
        } else if (experiment.getType().isRnaSeqDifferential()) {
            return rnaSeqDifferentialExperimentExternallyAvailableContentService.stream(
                    (DifferentialExperiment) experiment, uri);
        } else {
            return microarrayExperimentExternallyAvailableContentService.stream(
                    (MicroarrayExperiment) experiment, uri);
        }
    }

    public List<ExternallyAvailableContent> list(String experimentAccession,
                                                 String accessKey,
                                                 ExternallyAvailableContent.ContentType contentType) {
        Experiment<?> experiment = experimentTrader.getExperiment(experimentAccession, accessKey);
        String externalResourceType = externalResourceLinksPriority(experiment);

        if (experiment.getType().isProteomicsBaseline()) {
            return proteomicsBaselineExperimentExternallyAvailableContentService.list(
                    (BaselineExperiment) experiment, contentType);
        } else if (experiment.getType().isRnaSeqBaseline()) {
            List list = rnaSeqBaselineExperimentExternallyAvailableContentService.list(
                    (BaselineExperiment) experiment, contentType);
            List list2 =  externalResourceType.equals("geo") ?
                    rnaSeqBaselineExperimentExternallyAvailableContentServiceGeo.list(
                    (BaselineExperiment) experiment, contentType) :
                    externalResourceType.equals("ega") ?
                    rnaSeqBaselineExperimentExternallyAvailableContentServiceEga.list(
                            (BaselineExperiment) experiment, contentType) :
                    rnaSeqBaselineExperimentExternallyAvailableContentServiceEna.list(
                            (BaselineExperiment) experiment, contentType);
            list.addAll(list2);
            return list;
        } else if (experiment.getType().isRnaSeqDifferential()) {
            List list = rnaSeqDifferentialExperimentExternallyAvailableContentService.list(
                    (DifferentialExperiment) experiment, contentType);
            List list2 =  externalResourceType.equals("geo") ?
                    rnaSeqDifferentialExperimentExternallyAvailableContentServiceGeo.list(
                            (DifferentialExperiment) experiment, contentType) :
                    externalResourceType.equals("ega") ?
                    rnaSeqDifferentialExperimentExternallyAvailableContentServiceEga.list(
                            (DifferentialExperiment) experiment, contentType) :
                    rnaSeqDifferentialExperimentExternallyAvailableContentServiceEna.list(
                            (DifferentialExperiment) experiment, contentType);
            list.addAll(list2);
            return list;
        } else {
            List list = microarrayExperimentExternallyAvailableContentService.list(
                    (MicroarrayExperiment) experiment, contentType);
            List list2 =  externalResourceType.equals("geo") ?
                    microarrayExperimentExternallyAvailableContentServiceGeo.list(
                            (MicroarrayExperiment) experiment, contentType) :
                    externalResourceType.equals("ega") ?
                    microarrayExperimentExternallyAvailableContentServiceEga.list(
                            (MicroarrayExperiment) experiment, contentType) :
                    microarrayExperimentExternallyAvailableContentServiceEna.list(
                            (MicroarrayExperiment) experiment, contentType);
            list.addAll(list2);
            return list;
        }
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
