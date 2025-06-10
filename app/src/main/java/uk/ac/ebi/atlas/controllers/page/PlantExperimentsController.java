package uk.ac.ebi.atlas.controllers.page;

import com.google.common.collect.TreeMultimap;
import org.apache.commons.lang3.StringUtils;
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

    @GetMapping(value = "/plant/experiments", produces = "text/html;charset=UTF-8")
    public String getPlantExperimentsPage(Model model) {
        var experimentDisplayNames = new HashMap<String, String>();

        var publicPlantExperiments =
                experimentTrader.getPublicExperiments().stream()
                        .filter(experiment -> experiment.getSpecies().isPlant())
                        .collect(toImmutableSet());

        Comparator<String> displayNameComparator = Comparator.comparing(experimentDisplayNames::get);
        var experimentAccessionsBySpecies = TreeMultimap.create(String::compareTo, displayNameComparator);
        var numDifferentialExperimentsBySpecies = new TreeMap<String, Integer>();

        for (var experiment : publicPlantExperiments) {
            var accession = experiment.getAccession();
            experimentDisplayNames.put(
                accession,
                experiment.getDisplayName() + " (" + experiment.getAnalysedAssays().size() + " assays)"
            );

            if (experiment.getType().isBaseline()) {
                experimentAccessionsBySpecies.put(
                    getSpeciesName(experiment.getSpecies().getName()), accession);
            }
            else if (experiment.getType().isDifferential()) {
                var speciesReferenceName =
                    getSpeciesName(experiment.getSpecies().getReferenceName());
                numDifferentialExperimentsBySpecies.put(
                    speciesReferenceName,
                    numDifferentialExperimentsBySpecies.getOrDefault(speciesReferenceName, 0) + 1
                );
            }
        }

        var baselineExperimentsData =
            ExperimentsUtil.getBaselineExperiments(experimentAccessionsBySpecies, experimentDisplayNames);

        model.addAttribute("baselineExperimentsData", baselineExperimentsData);
        model.addAttribute("numDifferentialExperimentsBySpecies", numDifferentialExperimentsBySpecies);
        model.addAttribute("numberOfPlantExperiments", publicPlantExperiments.size());
        model.addAttribute("speciesIconSelector", SpeciesIconSelector.getEnumMap());
        model.addAttribute("title", "Plant experiments ");

        return "plants-landing-page";
    }

    private static String getSpeciesName(String speciesName) {
        return StringUtils.capitalize(speciesName.toLowerCase().replace(".", ""));
    }
}
