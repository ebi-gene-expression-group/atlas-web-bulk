package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.experiments.ExperimentInfoListService;
import uk.ac.ebi.atlas.species.AtlasInformationDao;
import uk.ac.ebi.atlas.species.SpeciesProperties;
import uk.ac.ebi.atlas.species.SpeciesPropertiesTrader;
import uk.ac.ebi.atlas.trader.ExpressionAtlasExperimentTrader;
import uk.ac.ebi.atlas.utils.ExperimentInfo;

import java.util.Random;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.species.AtlasInformationDataType.EFO;
import static uk.ac.ebi.atlas.species.AtlasInformationDataType.EG;
import static uk.ac.ebi.atlas.species.AtlasInformationDataType.ENSEMBL;
import static uk.ac.ebi.atlas.species.AtlasInformationDataType.WBPS;

@Controller
public class HomeController {
    @Autowired
    protected Environment env;

    // From e.g. https://www.ebi.ac.uk/s4/identification?term=tpi1
    private static final ImmutableMap<String, String> S4_SPECIES =
            ImmutableMap.of(
                    "homo sapiens", "Homo sapiens",
                    "mus musculus", "Mus musculus",
                    "saccharomyces cerevisiae", "Saccharomyces cerevisiae",
                    "drosophila melanogaster", "Drosophila melanogaster",
                    "caenorhabditis elegans", "Caenorhabditis elegans");
    private static final String NORMAL_SEPARATOR = "━━━━━━━━━━━━━━━━";
    private static final String BEST_SEPARATOR = "(╯°□°）╯︵ ┻━┻";
    private static final double EASTER_EGG_PROBABILITY = 0.0001;
    private static final Random RANDOM = new Random();

    private final SpeciesPropertiesTrader speciesPropertiesTrader;
    private final AtlasInformationDao atlasInformationDao;
    private final ExperimentInfoListService experimentInfoListService;

    public HomeController(SpeciesPropertiesTrader speciesPropertiesTrader,
                          ExpressionAtlasExperimentTrader experimentTrader,
                          AtlasInformationDao atlasInformationDao) {
        this.speciesPropertiesTrader = speciesPropertiesTrader;
        this.atlasInformationDao = atlasInformationDao;

        this.experimentInfoListService =
                new ExperimentInfoListService(
                        experimentTrader,
                        ImmutableList.of(
                                RNASEQ_MRNA_BASELINE,
                                PROTEOMICS_BASELINE,
                                RNASEQ_MRNA_DIFFERENTIAL,
                                MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
                                MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
                                MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL));
    }

    @RequestMapping(value = "/home", produces = "text/html;charset=UTF-8")
    public String getHome(Model model) {
        model.addAttribute(
                "separator", RANDOM.nextDouble() < EASTER_EGG_PROBABILITY ? BEST_SEPARATOR : NORMAL_SEPARATOR);

        model.addAttribute("topSpecies", S4_SPECIES);

        var allSpecies = speciesPropertiesTrader.getAll().stream()
                .collect(toImmutableMap(
                        SpeciesProperties::referenceName,
                        speciesProperties -> StringUtils.capitalize(speciesProperties.referenceName())));
        model.addAttribute("species", allSpecies);
        model.addAttribute("speciesPath", ""); // Required by Spring form tag

        model.addAttribute("numberOfStudies", experimentInfoListService.listPublicExperiments().size());

        var numberOfSpecies =
                experimentInfoListService.listPublicExperiments().stream()
                        .map(ExperimentInfo::getSpecies)
                        .distinct()
                        .count();
        model.addAttribute("numberOfSpecies", numberOfSpecies);

        var numberOfAssays =
                experimentInfoListService.listPublicExperiments().stream()
                        .mapToInt(ExperimentInfo::getNumberOfAssays)
                        .sum();
        model.addAttribute("numberOfAssays", numberOfAssays);

        model.addAttribute("info", atlasInformationDao.atlasInformation.get());
        model.addAttribute("ensembl", ENSEMBL.getId());
        model.addAttribute("eg", EG.getId());
        model.addAttribute("wbps", WBPS.getId());
        model.addAttribute("efo", EFO.getId());

        return "home";
    }
}
