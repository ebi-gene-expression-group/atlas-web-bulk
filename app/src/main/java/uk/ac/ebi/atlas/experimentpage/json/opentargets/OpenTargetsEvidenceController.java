package uk.ac.ebi.atlas.experimentpage.json.opentargets;

import com.google.common.collect.ImmutableSet;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.experimentpage.context.BulkDifferentialRequestContext;
import uk.ac.ebi.atlas.experimentpage.context.MicroarrayRequestContext;
import uk.ac.ebi.atlas.experimentpage.differential.DifferentialRequestPreferencesValidator;
import uk.ac.ebi.atlas.experimentpage.json.JsonExperimentController;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExpression;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExpression;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayProfile;
import uk.ac.ebi.atlas.model.experiment.differential.rnaseq.BulkDifferentialProfile;
import uk.ac.ebi.atlas.profiles.stream.BulkDifferentialProfileStreamFactory;
import uk.ac.ebi.atlas.profiles.stream.MicroarrayProfileStreamFactory;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.web.DifferentialRequestPreferences;
import uk.ac.ebi.atlas.web.MicroarrayRequestPreferences;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

// View of experiment as set of evidence between genes and diseases, in the Open Targets format.
// Returns an empty response for experiments that are not about diseases or that we have no evidence for.
@Controller
@Scope("request")
public class OpenTargetsEvidenceController extends JsonExperimentController {
    private final
    EvidenceService<DifferentialExpression, DifferentialExperiment, BulkDifferentialRequestContext, BulkDifferentialProfile>
            diffRnaSeqEvidenceService;
    private final
    EvidenceService<MicroarrayExpression, MicroarrayExperiment, MicroarrayRequestContext, MicroarrayProfile>
            diffMicroarrayEvidenceService;

    @Inject
    public OpenTargetsEvidenceController(ExperimentTrader experimentTrader,
                                         BulkDifferentialProfileStreamFactory bulkDifferentialProfileStreamFactory,
                                         MicroarrayProfileStreamFactory microarrayProfileStreamFactory,
                                         DataFileHub dataFileHub) {
        super(experimentTrader);
        String resourcesVersion = "prod.30";

        diffRnaSeqEvidenceService =
                new EvidenceService<>(bulkDifferentialProfileStreamFactory, dataFileHub, resourcesVersion);
        diffMicroarrayEvidenceService =
                new EvidenceService<>(microarrayProfileStreamFactory, dataFileHub, resourcesVersion);
    }

    @InitBinder("preferences")
    void initBinder(WebDataBinder binder) {
        binder.addValidators(new DifferentialRequestPreferencesValidator());
    }

    // As the response is written directly the content-type is set in the HttpServletResponse, within the method
    private void differentialMicroarrayExperimentEvidence(double logFoldChangeCutoff,
                                                          double pValueCutoff,
                                                          int maxGenesPerContrast,
                                                          String experimentAccession,
                                                          String accessKey,
                                                          HttpServletResponse response) throws IOException {
        MicroarrayExperiment experiment =
                (MicroarrayExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);
        // Setting the header is enough, there’s no need to do response.setCharacterEncoding("UTF-8")
        response.setHeader("content-type", "application/json-seq; charset=UTF-8");
        PrintWriter responseWriter = response.getWriter();
        diffMicroarrayEvidenceService.evidenceForExperiment(
                experiment,
                contrast -> {
                    MicroarrayRequestPreferences requestPreferences = new MicroarrayRequestPreferences();
                    requestPreferences.setFoldChangeCutoff(logFoldChangeCutoff);
                    requestPreferences.setCutoff(pValueCutoff);
                    requestPreferences.setHeatmapMatrixSize(maxGenesPerContrast);
                    requestPreferences.setSelectedColumnIds(ImmutableSet.of(contrast.getId()));
                    return new MicroarrayRequestContext(requestPreferences, experiment);
                },
                jsonObject -> responseWriter.println(GSON.toJson(jsonObject)));
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}/evidence",
            produces = "application/json-seq;charset=UTF-8",
            params = "type=MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL")
    public void differentialMicroarray1ColourMRnaExperimentEvidence(
            @RequestParam(defaultValue = "0") double logFoldChangeCutoff,
            @RequestParam(defaultValue = "1") double pValueCutoff,
            @RequestParam(defaultValue = "-1") int maxGenesPerContrast,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey, HttpServletResponse response) throws IOException {
        differentialMicroarrayExperimentEvidence(
                logFoldChangeCutoff,
                pValueCutoff,
                maxGenesPerContrast,
                experimentAccession,
                accessKey,
                response);
    }

    // As the response is written directly the content-type is set in the HttpServletResponse, within the method
    @RequestMapping(value = "/json/experiments/{experimentAccession}/evidence",
            produces = "application/json-seq;charset=UTF-8",
            params = "type=MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL")
    public void differentialMicroarray2ColourMRnaExperimentEvidence(
            @RequestParam(defaultValue = "0") double logFoldChangeCutoff,
            @RequestParam(defaultValue = "1") double pValueCutoff,
            @RequestParam(defaultValue = "-1") int maxGenesPerContrast,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey, HttpServletResponse response) throws IOException {
        differentialMicroarrayExperimentEvidence(
                logFoldChangeCutoff,
                pValueCutoff,
                maxGenesPerContrast,
                experimentAccession,
                accessKey,
                response);
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}/evidence",
            produces = "application/json-seq;charset=UTF-8",
            params = "type=MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL")
    public void differentialMicroarray1ColourMicroRnaExperimentEvidence(
            @RequestParam(defaultValue = "0") double logFoldChangeCutoff,
            @RequestParam(defaultValue = "1") double pValueCutoff,
            @RequestParam(defaultValue = "-1") int maxGenesPerContrast,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey, HttpServletResponse response) throws IOException {
        differentialMicroarrayExperimentEvidence(
                logFoldChangeCutoff,
                pValueCutoff,
                maxGenesPerContrast,
                experimentAccession,
                accessKey,
                response);
    }

    @RequestMapping(value = "/json/experiments/{experimentAccession}/evidence",
            produces = "application/json-seq;charset=UTF-8",
            params = "type=RNASEQ_MRNA_DIFFERENTIAL")
    public void differentialRnaSeqExperimentEvidence(
            @RequestParam(defaultValue = "0") double logFoldChangeCutoff,
            @RequestParam(defaultValue = "1") double pValueCutoff,
            @RequestParam(defaultValue = "-1") int maxGenesPerContrast,
            @PathVariable String experimentAccession,
            @RequestParam(defaultValue = "") String accessKey, HttpServletResponse response) throws IOException {
        DifferentialExperiment experiment =
                (DifferentialExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);
        response.setHeader("content-type", "application/json-seq; charset=UTF-8");
        PrintWriter printWriter = response.getWriter();
        diffRnaSeqEvidenceService.evidenceForExperiment(
                experiment,
                contrast -> {
                    DifferentialRequestPreferences requestPreferences = new DifferentialRequestPreferences();
                    requestPreferences.setFoldChangeCutoff(logFoldChangeCutoff);
                    requestPreferences.setCutoff(pValueCutoff);
                    requestPreferences.setHeatmapMatrixSize(maxGenesPerContrast);
                    requestPreferences.setSelectedColumnIds(ImmutableSet.of(contrast.getId()));
                    return new BulkDifferentialRequestContext(requestPreferences, experiment);
                },
                jsonObject -> printWriter.println(GSON.toJson(jsonObject)));
    }
}
