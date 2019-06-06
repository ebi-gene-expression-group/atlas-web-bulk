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
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Controller
public class PlantExperimentsController extends HtmlExceptionHandlingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlantExperimentsController.class);

    private final ExperimentTrader experimentTrader;

    private Integer numberOfPlantExperiments;

    private SortedSetMultimap<String, String> baselineExperimentAccessionsBySpecies;
    private SortedMap<String, Integer> numDifferentialExperimentsBySpecies;

    private Map<String, String> experimentLinks = new HashMap<>();
    private Map<String, String> experimentDisplayNames = new HashMap<>();

    public PlantExperimentsController(ExperimentTrader experimentTrader) {
        this.experimentTrader = experimentTrader;

        // Get number of all public plant experiments in Atlas
        numberOfPlantExperiments = 0;

        Comparator<String> keyComparator = String::compareTo;
        // experiments should be sorted by their display name, not accession
        Comparator<String> valueComparator = Comparator.comparing(o -> experimentDisplayNames.get(o));
        baselineExperimentAccessionsBySpecies = TreeMultimap.create(keyComparator, valueComparator);

        for (Experiment experiment : experimentTrader.getPublicExperiments(ExperimentType.RNASEQ_MRNA_BASELINE, ExperimentType.PROTEOMICS_BASELINE)) {
            String experimentAccession = experiment.getAccession();

            try {
                int numberOfAssays = experiment.getAnalysedAssays().size();

                experimentDisplayNames.put(
                        experimentAccession, experiment.getDisplayName() + " (" + numberOfAssays + " assays)");

                Species species = experiment.getSpecies();
                if (species.isPlant()) {
                    baselineExperimentAccessionsBySpecies.put(species.getName(), experimentAccession);
                    experimentLinks.put(experimentAccession + species.getName(), "");
                    numberOfPlantExperiments++;
                }

            } catch (RuntimeException e) {
                // we don't want the entire application to crash just because one condensedSdrf file may be offline
                // because a curator is modifying it
                LOGGER.error(e.getMessage(), e);
            }
        }

        numDifferentialExperimentsBySpecies = new TreeMap<>();
        long start = System.currentTimeMillis();
        populateExperimentAccessionToSpecies(ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL);
        populateExperimentAccessionToSpecies(ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL);
        populateExperimentAccessionToSpecies(ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL);
        populateExperimentAccessionToSpecies(ExperimentType.RNASEQ_MRNA_DIFFERENTIAL);
        LOGGER.info("Differential experiments took: {} ms", System.currentTimeMillis() - start);
    }

    @RequestMapping(value = "/plant/experiments", produces = "text/html;charset=UTF-8")
    public String getPlantExperimentsPage(Model model) {
        model.addAttribute("baselineExperimentAccessionsBySpecies", baselineExperimentAccessionsBySpecies);
        model.addAttribute("numDifferentialExperimentsBySpecies", numDifferentialExperimentsBySpecies);
        model.addAttribute("experimentLinks", experimentLinks);
        model.addAttribute("experimentDisplayNames", experimentDisplayNames);
        model.addAttribute("numberOfPlantExperiments", numberOfPlantExperiments);

        model.addAttribute("mainTitle", "Plant experiments ");

        return "plants-landing-page";
    }

    /**
     * Populates numDifferentialExperimentsBySpecies and numberOfPlantExperiments for a given experimentType
     * This is a part of a work-around until https://www.pivotaltracker.com/story/show/88885788 gets implemented.
     */
    private void populateExperimentAccessionToSpecies(ExperimentType experimentType) {
        for (Experiment experiment : experimentTrader.getPublicExperiments(experimentType)) {
            try {
                Species species =
                        experimentTrader.getExperiment(experiment.getAccession(), "").getSpecies();

                if (species.isPlant()) {
                    Integer numSoFar = numDifferentialExperimentsBySpecies.get(species.getReferenceName());
                    numDifferentialExperimentsBySpecies.put(species.getReferenceName(), numSoFar == null ? 1 : ++numSoFar);
                    numberOfPlantExperiments++;
                }

            } catch (RuntimeException e) {
                // We don't want the entire application to crash just because one condensedSdrf file is missing because
                // a curator is modifying it
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
