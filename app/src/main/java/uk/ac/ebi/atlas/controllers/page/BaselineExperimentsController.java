package uk.ac.ebi.atlas.controllers.page;

import com.google.common.collect.TreeMultimap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Comparator;
import java.util.HashMap;

@Controller
public class BaselineExperimentsController extends HtmlExceptionHandlingController {
    private final ExperimentTrader experimentTrader;

    public BaselineExperimentsController(ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;
    }

    @RequestMapping(value = "/baseline/experiments", produces = "text/html;charset=UTF-8")
    public String getBaselineExperimentsPage(Model model) {
        var experimentDisplayNames = new HashMap<String, String>();

        Comparator<String> displayNameComparator = (o1, o2) -> {
            boolean hasProt = o1.contains("-PROT-");
            return hasProt ? (o2.contains("-PROT-") ? 0 : 1) :
                (o2.contains("-PROT-") ? -1 :
                    experimentDisplayNames.get(o1).compareTo(experimentDisplayNames.get(o2)));
        };
        Comparator<String> speciesComparator = (o1, o2) ->
            o1.equals("Homo sapiens") ?
                (o2.equals("Homo sapiens") ? 0 : -1) :
                (o2.equals("Homo sapiens") ? 1 :
                    o1.compareTo(o2));

        var experimentAccessionsBySpecies = TreeMultimap.create(speciesComparator, displayNameComparator);

        var publicBaselineExperiments =
                experimentTrader.getPublicExperiments(
                        ExperimentType.RNASEQ_MRNA_BASELINE, ExperimentType.PROTEOMICS_BASELINE, ExperimentType.PROTEOMICS_BASELINE_DIA);

        for (var experiment : publicBaselineExperiments) {
            var accession = experiment.getAccession();
            var displayName = experiment.getDisplayName() + " (" + experiment.getAnalysedAssays().size() + " assays)";
            experimentDisplayNames.put(accession, displayName);
            experimentAccessionsBySpecies.put(experiment.getNormalisedSpeciesName(), accession);
        }

        var baselineExperimentsData =
            ExperimentsUtil.getBaselineExperiments(experimentAccessionsBySpecies, experimentDisplayNames);

        model.addAttribute("baselineExperimentsData", baselineExperimentsData);
        model.addAttribute("speciesIconSelector", SpeciesIconSelector.getEnumMap());
        model.addAttribute("title", "Baseline experiments ");

        return "baseline-landing-page";
    }
}
