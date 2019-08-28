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

    // Dear Future Expression Atlas Developer,
    // You may feel the urge to optimise this method and intialise the collections in the constructor, then
    // have them be immutable throughout the lifetime of this controller. However, consider the following:
    // 1. Public experiments may change and they need to be reflected here
    // 2. Sorting the collections with the cached experiments takes about 30 ms
    @RequestMapping(value = "/baseline/experiments", produces = "text/html;charset=UTF-8")
    public String getBaselineExperimentsPage(Model model) {
        var experimentLinks = new HashMap<String, String>();
        var experimentDisplayNames = new HashMap<String, String>();

        Comparator<String> keyComparator = (o1, o2) -> {
            if (o1.equals("Homo sapiens") && !o2.equals("Homo sapiens")) {
                return -1;
            } else if (o2.equals("Homo sapiens") && !o1.equals("Homo sapiens")) {
                return 1;
            } else {
                return o1.compareTo(o2);
            }
        };
        // experiments should be sorted by their display name, not accession
        Comparator<String> valueComparator = (o1, o2) -> {
            // Services review: Alvis' edict for proteomics experiments to always come up at the bottom of
            // the list of experiments within each species
            if (o1.contains("-PROT-") && !o2.contains("-PROT-")) {
                return 1;
            } else if (o2.contains("-PROT-") && !o1.contains("-PROT-")) {
                return -1;
            } else {
                return experimentDisplayNames.get(o1).compareTo(experimentDisplayNames.get(o2));
            }
        };

        var experimentAccessionsBySpecies = TreeMultimap.create(keyComparator, valueComparator);

        var publicBaselineExperiments =
                experimentTrader.getPublicExperiments(
                        ExperimentType.RNASEQ_MRNA_BASELINE, ExperimentType.PROTEOMICS_BASELINE);

        for (var experiment : publicBaselineExperiments) {
            experimentDisplayNames.put(
                    experiment.getAccession(),
                    experiment.getDisplayName() + " (" + experiment.getAnalysedAssays().size() + " assays)");
            experimentAccessionsBySpecies.put(experiment.getSpecies().getName(), experiment.getAccession());
            experimentLinks.put(experiment.getAccession() + experiment.getSpecies().getName(), "");
        }

        model.addAttribute("experimentAccessionsBySpecies", experimentAccessionsBySpecies);
        model.addAttribute("experimentLinks", experimentLinks);
        model.addAttribute("experimentDisplayNames", experimentDisplayNames);

        model.addAttribute("mainTitle", "Baseline expression experiments ");

        return "baseline-landing-page";
    }
}
