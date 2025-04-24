package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.home.species.SpeciesSummaryService;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Collection;
import java.util.Objects;

import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EFO;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.EG;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.ENSEMBL;
import static uk.ac.ebi.atlas.home.AtlasInformationDataType.WBPS;

@Controller
public class HomeController extends HtmlExceptionHandlingController {
    private static final ImmutableSet<String> S4_SPECIES =
        ImmutableSet.of(
            "Homo sapiens",
            "Mus musculus",
            "Saccharomyces cerevisiae",
            "Drosophila melanogaster",
            "Caenorhabditis elegans");

    private final SpeciesSummaryService speciesSummaryService;
    private final AtlasInformationDao atlasInformationDao;
    private final ExperimentTrader experimentTrader;

    public HomeController(SpeciesSummaryService speciesSummaryService,
                          AtlasInformationDao atlasInformationDao,
                          ExperimentTrader experimentTrader) {
        this.speciesSummaryService = speciesSummaryService;
        this.atlasInformationDao = atlasInformationDao;
        this.experimentTrader = experimentTrader;
    }

    @RequestMapping(value = {"/", "/index"})
    public String redirectHome() {
        return "redirect:/home";
    }

    @RequestMapping(value = "/home", produces = "text/html;charset=UTF-8")
    public String getHome(Model model) {
        model.addAttribute("title", "Home");

        var species = ImmutableSortedSet.copyOf(speciesSummaryService.getSpecies());

        model.addAttribute("numberOfSpecies", species.size());
        model.addAttribute("numberOfStudies", experimentTrader.getPublicExperiments().size());
        var numberOfAssays =
                experimentTrader.getPublicExperiments().stream()
                        .map(Experiment::getAnalysedAssays)
                        .mapToInt(Collection::size)
                        .sum();
        model.addAttribute("numberOfAssays", numberOfAssays);

        var info = Objects.requireNonNull(atlasInformationDao.atlasInformation.get());
        model.addAttribute("ensembl", info.get(ENSEMBL.getId()));
        model.addAttribute("eg", info.get(EG.getId()));
        model.addAttribute("wbps", info.get(WBPS.getId()));
        model.addAttribute("efo", info.get(EFO.getId()));

        model.addAttribute("topSpecies", S4_SPECIES);
        model.addAttribute("species", species);
        model.addAttribute("speciesPath", ""); // Required by Spring form tag

        return "home";
    }
}
