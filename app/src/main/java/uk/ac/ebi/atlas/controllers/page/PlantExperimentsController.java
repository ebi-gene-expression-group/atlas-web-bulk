package uk.ac.ebi.atlas.controllers.page;

import com.google.common.collect.TreeMultimap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

@Controller
public class PlantExperimentsController extends HtmlExceptionHandlingController {
    private final ExperimentTrader experimentTrader;

    public PlantExperimentsController(ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;
    }

    // Dear Future Expression Atlas Developer,
    // Have a look at the comment in BaselineExperimentsController.
    @GetMapping(value = "/plant/experiments", produces = "text/html;charset=UTF-8")
    public String getPlantExperimentsPage(Model model) {
        var experimentLinks = new HashMap<String, String>();
        var experimentDisplayNames = new HashMap<String, String>();

        var publicPlantExperiments =
                experimentTrader.getPublicExperiments().stream()
                        .filter(experiment -> experiment.getSpecies().isPlant())
                        .collect(toImmutableSet());

        // Sort experiments by their display name
        Comparator<String> valueComparator = Comparator.comparing(experimentDisplayNames::get);
        var baselineExperimentAccessionsBySpecies = TreeMultimap.create(String::compareTo, valueComparator);
        var numDifferentialExperimentsBySpecies = new TreeMap<String, Integer>();

        for (var experiment : publicPlantExperiments) {
            experimentLinks.put(experiment.getAccession() + experiment.getSpecies().getName(), "");
            experimentDisplayNames.put(
                    experiment.getAccession(),
                    experiment.getDisplayName() + " (" + experiment.getAnalysedAssays().size() + " assays)");

            if (experiment.getType().isBaseline()) {
                baselineExperimentAccessionsBySpecies.put(experiment.getSpecies().getName(), experiment.getAccession());
            }
            else if (experiment.getType().isDifferential()) {
                var speciesReferenceName = experiment.getSpecies().getReferenceName();
                numDifferentialExperimentsBySpecies.put(
                        speciesReferenceName,
                        numDifferentialExperimentsBySpecies.getOrDefault(speciesReferenceName, 0) + 1);
            }
        }

        model.addAttribute("baselineExperimentAccessionsBySpecies", baselineExperimentAccessionsBySpecies);
        model.addAttribute("numDifferentialExperimentsBySpecies", numDifferentialExperimentsBySpecies);
        model.addAttribute("experimentLinks", experimentLinks);
        model.addAttribute("experimentDisplayNames", experimentDisplayNames);
        model.addAttribute("numberOfPlantExperiments", publicPlantExperiments.size());

        model.addAttribute("mainTitle", "Plant experiments ");

        return "plants-landing-page";
    }
}
