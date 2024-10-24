package uk.ac.ebi.atlas.experimentpage.tooltip;

import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesign;
import uk.ac.ebi.atlas.model.experiment.sample.Contrast;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.summary.ContrastSummary;
import uk.ac.ebi.atlas.model.experiment.summary.ContrastSummaryBuilder;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Controller
@Scope("request")
public class ContrastSummaryController {

    private final ExperimentTrader experimentTrader;

    @Inject
    public ContrastSummaryController(ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;
    }

    @RequestMapping(value = "/rest/contrast-summary", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getTooltipContrastContent(@RequestParam(value = "experimentAccession") String experimentAccession,
                                            @RequestParam(value = "contrastId") String contrastId,
                                            @RequestParam(value = "accessKey", required = false) String accessKey) {

        DifferentialExperiment differentialExperiment =
                (DifferentialExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);

        Contrast contrast = differentialExperiment.getDataColumnDescriptor(contrastId);
        if (contrast == null) {
            throw new IllegalArgumentException("No contrast with ID " + contrastId + " found.");
        }

        ExperimentDesign experimentDesign = experimentTrader.getExperimentDesign(experimentAccession);

        ContrastSummary contrastSummary = new ContrastSummaryBuilder()
                .withExperimentDesign(experimentDesign)
                .forContrast(contrast)
                .withExperimentDescription(differentialExperiment.getDescription())
                .build();

        return GSON.toJson(contrastSummary);
    }

}
