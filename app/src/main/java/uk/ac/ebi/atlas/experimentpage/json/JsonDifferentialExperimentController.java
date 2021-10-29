package uk.ac.ebi.atlas.experimentpage.json;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.atlas.experimentpage.context.DifferentialRequestContextFactory;
import uk.ac.ebi.atlas.experimentpage.context.MicroarrayRequestContext;
import uk.ac.ebi.atlas.experimentpage.context.RnaSeqRequestContext;
import uk.ac.ebi.atlas.experimentpage.differential.DifferentialExperimentPageService;
import uk.ac.ebi.atlas.experimentpage.differential.DifferentialProfilesHeatMap;
import uk.ac.ebi.atlas.experimentpage.differential.DifferentialRequestPreferencesValidator;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExpression;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExpression;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayProfile;
import uk.ac.ebi.atlas.model.experiment.differential.rnaseq.RnaSeqProfile;
import uk.ac.ebi.atlas.profiles.stream.MicroarrayProfileStreamFactory;
import uk.ac.ebi.atlas.profiles.stream.RnaSeqAndProteomicsProfileStreamFactory;
import uk.ac.ebi.atlas.resource.ContrastImageTrader;
import uk.ac.ebi.atlas.solr.bioentities.query.SolrQueryService;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.web.DifferentialRequestPreferences;
import uk.ac.ebi.atlas.web.MicroarrayRequestPreferences;

import javax.inject.Inject;
import javax.validation.Valid;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Controller
@Scope("request")
public class JsonDifferentialExperimentController extends JsonExperimentController {

    @InitBinder("preferences")
    void initBinder(WebDataBinder binder) {
        binder.addValidators(new DifferentialRequestPreferencesValidator());
    }

    private final
        DifferentialExperimentPageService<DifferentialExpression, DifferentialExperiment,
                DifferentialRequestPreferences, RnaSeqProfile, RnaSeqRequestContext>
            differentialExperimentPageService;

    private final
        DifferentialExperimentPageService<MicroarrayExpression, MicroarrayExperiment, MicroarrayRequestPreferences,
                MicroarrayProfile, MicroarrayRequestContext>
            diffMicroarrayExperimentPageService;

    @Inject
    public JsonDifferentialExperimentController(ExperimentTrader experimentTrader,
                                                RnaSeqAndProteomicsProfileStreamFactory rnaSeqAndProteomicsProfileStreamFactory,
                                                MicroarrayProfileStreamFactory microarrayProfileStreamFactory,
                                                SolrQueryService solrQueryService,
                                                ContrastImageTrader atlasResourceHub) {
        super(experimentTrader);

        differentialExperimentPageService =
                new DifferentialExperimentPageService<>(new DifferentialRequestContextFactory.RnaSeq(),
                        new DifferentialProfilesHeatMap<>(rnaSeqAndProteomicsProfileStreamFactory, solrQueryService),
                        atlasResourceHub);

        diffMicroarrayExperimentPageService =
                new DifferentialExperimentPageService<>(new DifferentialRequestContextFactory.Microarray(),
                        new DifferentialProfilesHeatMap<>(microarrayProfileStreamFactory, solrQueryService),
                        atlasResourceHub);
    }

    private String differentialMicroarrayExperimentData(MicroarrayRequestPreferences preferences,
                                                        String experimentAccession,
                                                        String accessKey) {
        return GSON.toJson(diffMicroarrayExperimentPageService.getResultsForExperiment(
                (MicroarrayExperiment) experimentTrader.getExperiment(experimentAccession, accessKey),
                accessKey,
                preferences));
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}",
                    produces = "application/json;charset=UTF-8",
                    params = "type=MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL")
    @ResponseBody
    public String differentialMicroarray1ColourMRnaExperimentData(
            @ModelAttribute("preferences") @Valid MicroarrayRequestPreferences preferences,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey) {
        return differentialMicroarrayExperimentData(preferences, experimentAccession, accessKey);
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}",
            produces = "application/json;charset=UTF-8",
            params = "type=MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL")
    @ResponseBody
    public String differentialMicroarray2ColourMRnaExperimentData(
            @ModelAttribute("preferences") @Valid MicroarrayRequestPreferences preferences,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey) {
        return differentialMicroarrayExperimentData(preferences, experimentAccession, accessKey);
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}",
            produces = "application/json;charset=UTF-8",
            params = "type=MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL")
    @ResponseBody
    public String differentialMicroarray1ColourMicroRnaExperimentData(
            @ModelAttribute("preferences") @Valid MicroarrayRequestPreferences preferences,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey) {
        return differentialMicroarrayExperimentData(preferences, experimentAccession, accessKey);
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}",
                    produces = "application/json;charset=UTF-8",
                    params = "type=RNASEQ_MRNA_DIFFERENTIAL")
    @ResponseBody
    public String differentialRnaSeqExperimentData(
            @ModelAttribute("preferences") @Valid DifferentialRequestPreferences preferences,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey) {
        return GSON.toJson(differentialExperimentPageService.getResultsForExperiment(
                (DifferentialExperiment) experimentTrader.getExperiment(experimentAccession, accessKey),
                accessKey, preferences));
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}",
            produces = "application/json;charset=UTF-8",
            params = "type=PROTEOMICS_DIFFERENTIAL")
    @ResponseBody
    public String differentialProteomicsExperimentData(
            @ModelAttribute("preferences") @Valid DifferentialRequestPreferences preferences,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey) {
        return GSON.toJson(differentialExperimentPageService.getResultsForExperiment(
                (DifferentialExperiment) experimentTrader.getExperiment(experimentAccession, accessKey),
                accessKey, preferences));
    }
}
