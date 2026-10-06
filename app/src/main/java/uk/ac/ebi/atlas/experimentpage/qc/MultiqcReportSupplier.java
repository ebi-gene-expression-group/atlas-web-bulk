package uk.ac.ebi.atlas.experimentpage.qc;

import uk.ac.ebi.atlas.model.download.ExternallyAvailableContent;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.resource.DataFileHub;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;

@Named
public class MultiqcReportSupplier extends ExternallyAvailableContent.Supplier<DifferentialExperiment> {
    private final DataFileHub dataFileHub;

    @Inject
    public MultiqcReportSupplier(DataFileHub dataFileHub) {
        this.dataFileHub = dataFileHub;
    }

    @Override
    public ExternallyAvailableContent.ContentType contentType() {
        return ExternallyAvailableContent.ContentType.PLOTS;
    }

    @Override
    public Collection<ExternallyAvailableContent> get(DifferentialExperiment experiment) {
        if (!experiment.getType().isRnaSeqDifferential() ||
                !dataFileHub.getExperimentFiles(experiment.getAccession()).multiqcReport.exists()) {
            return Collections.emptySet();
        }

        return Collections.singleton(
                new ExternallyAvailableContent(
                        MultiqcReportController.getMultiqcReportUrl(experiment.getAccession(), ""),
                        ExternallyAvailableContent.Description.create("link", "MultiQC report")));
    }
}
