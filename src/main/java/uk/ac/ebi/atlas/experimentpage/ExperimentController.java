package uk.ac.ebi.atlas.experimentpage;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.experimentpage.content.ExperimentPageContentService;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

@Controller
public class ExperimentController extends HtmlExceptionHandlingController {
    private final ExperimentTrader experimentTrader;
    private final ExperimentAttributesService experimentAttributesService;
    private final ExperimentPageContentService experimentPageContentService;

    public ExperimentController(ExperimentTrader experimentTrader,
                                ExperimentPageContentService experimentPageContentService,
                                ExperimentAttributesService experimentAttributesService) {
        this.experimentTrader = experimentTrader;
        this.experimentPageContentService = experimentPageContentService;
        this.experimentAttributesService = experimentAttributesService;
    }

    @RequestMapping(value = {"/experiments/{experimentAccession}", "/experiments/{experimentAccession}/**"},
                    produces = "text/html;charset=UTF-8")
    public String showExperimentPage(Model model,
                                     @PathVariable String experimentAccession,
                                     @RequestParam(defaultValue = "") String accessKey) {

        Experiment<?> experiment = experimentTrader.getExperiment(experimentAccession, accessKey);

        model.addAllAttributes(experimentAttributesService.getAttributes(experiment));
        model.addAttribute(
                "content",
                experimentPageContentService.jsonSerializeContentForExperiment(experiment, accessKey));

        return "experiment-page";
    }
}
