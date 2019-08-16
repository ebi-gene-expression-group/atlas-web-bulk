package uk.ac.ebi.atlas.controllers.page;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Controller
public class BaselineExperimentsController extends HtmlExceptionHandlingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaselineExperimentsController.class);
    private final SortedSetMultimap<String, String> experimentAccessionsBySpecies;
    private final Map<String, String> experimentLinks;
    private final Map<String, String> experimentDisplayNames;

    public BaselineExperimentsController(ExperimentTrader experimentTrader) {
        experimentDisplayNames = new HashMap<>();

        for (var experiment :
                experimentTrader.getPublicExperiments(
                        ExperimentType.RNASEQ_MRNA_BASELINE, ExperimentType.PROTEOMICS_BASELINE)) {
            String experimentAccession = experiment.getAccession();
            String displayName;
            try {
                displayName = experimentTrader.getPublicExperiment(experimentAccession).getDisplayName();
            } catch (RuntimeException e) {
                // We don't want the entire application to crash just because one condensed SDRF file may is missing
                LOGGER.error(e.getMessage(), e);
                displayName = experimentAccession;
            }

            int numberOfAssays = experimentTrader.getPublicExperiment(experimentAccession).getAnalysedAssays().size();

            experimentDisplayNames.put(experimentAccession, displayName + " (" + numberOfAssays + " assays)");
        }

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
        experimentAccessionsBySpecies = TreeMultimap.create(keyComparator, valueComparator);

        experimentLinks = new HashMap<>();

        for (Experiment experiment :
                experimentTrader.getPublicExperiments(
                        ExperimentType.RNASEQ_MRNA_BASELINE, ExperimentType.PROTEOMICS_BASELINE)) {
            String experimentAccession = experiment.getAccession();

            try {
                experimentAccessionsBySpecies.put(experiment.getSpecies().getName(), experimentAccession);
                experimentLinks.put(experimentAccession + experiment.getSpecies().getName(), "");
            } catch (RuntimeException e) {
                // we don't want the entire application to crash just because one condensedSdrf file may be offline
                // because a curator is modifying it
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @RequestMapping(value = "/baseline/experiments", produces = "text/html;charset=UTF-8")
    public String getBaselineExperimentsPage(Model model) {
        model.addAttribute("experimentAccessionsBySpecies", experimentAccessionsBySpecies);
        model.addAttribute("experimentLinks", experimentLinks);
        model.addAttribute("experimentDisplayNames", experimentDisplayNames);

        model.addAttribute("mainTitle", "Baseline expression experiments ");

        return "baseline-landing-page";
    }
}
