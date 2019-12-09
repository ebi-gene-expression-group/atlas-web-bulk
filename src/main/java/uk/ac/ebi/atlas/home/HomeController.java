package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.experiments.ExperimentInfoListService;
import uk.ac.ebi.atlas.home.species.SpeciesSummaryService;
import uk.ac.ebi.atlas.utils.ExperimentInfo;

import java.util.Random;
import java.util.function.Function;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EFO;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EG;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.ENSEMBL;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.WBPS;

@Controller
public class HomeController extends HtmlExceptionHandlingController {
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

    private final SpeciesSummaryService speciesSummaryService;
    private final AtlasInformationDao atlasInformationDao;
    private final ExperimentInfoListService experimentInfoListService;

    public HomeController(SpeciesSummaryService speciesSummaryService,
                          AtlasInformationDao atlasInformationDao,
                          ExperimentInfoListService experimentInfoListService) {
        this.speciesSummaryService = speciesSummaryService;
        this.atlasInformationDao = atlasInformationDao;
        this.experimentInfoListService = experimentInfoListService;
    }

    @RequestMapping(value = "/home", produces = "text/html;charset=UTF-8")
    public String getHome(Model model) {
        var species = speciesSummaryService.getReferenceSpecies()
                .stream()
                .collect(toImmutableMap(Function.identity(), StringUtils::capitalize));

        model.addAttribute("numberOfSpecies", species.size());
        model.addAttribute("numberOfStudies", experimentInfoListService.listPublicExperiments().size());
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

        model.addAttribute("topSpecies", S4_SPECIES);
        model.addAttribute(
                "separator", RANDOM.nextDouble() < EASTER_EGG_PROBABILITY ? BEST_SEPARATOR : NORMAL_SEPARATOR);
        model.addAttribute("species", species);
        model.addAttribute("speciesPath", ""); // Required by Spring form tag

        return "home";
    }
}
