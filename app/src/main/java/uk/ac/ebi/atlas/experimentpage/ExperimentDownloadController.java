package uk.ac.ebi.atlas.experimentpage;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.web.DifferentialRequestPreferences;
import uk.ac.ebi.atlas.web.MicroarrayRequestPreferences;
import uk.ac.ebi.atlas.web.ProteomicsBaselineRequestPreferences;
import uk.ac.ebi.atlas.web.RnaSeqBaselineRequestPreferences;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static uk.ac.ebi.atlas.experimentpage.ExperimentDispatcherUtils.alreadyForwardedButNoOtherControllerHandledTheRequest;
import static uk.ac.ebi.atlas.experimentpage.ExperimentDispatcherUtils.buildForwardURL;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@Controller
public class ExperimentDownloadController {

    private final ExperimentTrader experimentTrader;
    //TODO: Proteomics class is only refering to proteomics-baseline, while proteomics-differential will go bulkDifferential class
    //TODO: Refactor the Proteomics name properly
    private final ExperimentDownloadSupplier.Proteomics proteomicsExperimentDownloadSupplier;
    private final ExperimentDownloadSupplier.RnaSeqBaseline rnaSeqBaselineExperimentDownloadSupplier;
    private final ExperimentDownloadSupplier.BulkDifferential bulkDifferentialExperimentDownloadSupplier;
    private final ExperimentDownloadSupplier.Microarray microarrayExperimentDownloadSupplier;
    private final ExperimentFileLocationService experimentFileLocationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentDownloadController.class);

    @Inject
    public ExperimentDownloadController(ExperimentTrader experimentTrader,
                                        ExperimentDownloadSupplier.Proteomics
                                                proteomicsExperimentDownloadSupplier,
                                        ExperimentDownloadSupplier.RnaSeqBaseline
                                                rnaSeqBaselineExperimentDownloadSupplier,
                                        ExperimentDownloadSupplier.BulkDifferential
                                                    bulkDifferentialExperimentDownloadSupplier,
                                        ExperimentDownloadSupplier.Microarray
                                                microarrayExperimentDownloadSupplier,
                                        ExperimentFileLocationService
                                                experimentFileLocationService) {
        this.experimentTrader = experimentTrader;
        this.proteomicsExperimentDownloadSupplier = proteomicsExperimentDownloadSupplier;
        this.rnaSeqBaselineExperimentDownloadSupplier = rnaSeqBaselineExperimentDownloadSupplier;
        this.bulkDifferentialExperimentDownloadSupplier = bulkDifferentialExperimentDownloadSupplier;
        this.microarrayExperimentDownloadSupplier = microarrayExperimentDownloadSupplier;
        this.experimentFileLocationService = experimentFileLocationService;
    }

    /*
    Wojtek : I think I wanted this to go somewhere - get rid of dispatching or so - rather than needed {experimentType}
    in the parameter I ended up needing the dispatcher anyway - so that different ExperimentPageRequestPreferences get
    wired in.
    We're not trying to keep these URLs stable and people shouldn't bookmark them. So, you could just remove the
    parameter, and also the dispatcher because the links will have the parameter `type` - see ExperimentPageService.
    Be nice and set up redirects here if you do, with a note telling the future us to remove it after a while.
     */
    public static final String DOWNLOAD_URL_TEMPLATE =
            "experiments-content/{experimentAccession}/download/{experimentType}";

    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE)
    public String dispatch(HttpServletRequest request,
                           @PathVariable String experimentAccession,
                           @RequestParam(defaultValue = "") String accessKey) {
        if (alreadyForwardedButNoOtherControllerHandledTheRequest(request)) {
            // prevent an infinite loop
            throw new NoExperimentSubResourceException();
        }

        return "forward:" + buildForwardURL(request, experimentTrader.getExperiment(experimentAccession, accessKey));
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    private class NoExperimentSubResourceException extends RuntimeException {
    }

    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE, params = "type=PROTEOMICS_BASELINE")
    public void
    proteomicsExperimentDownload(
            @PathVariable String experimentAccession,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @ModelAttribute("preferences") @Valid ProteomicsBaselineRequestPreferences preferences,
            HttpServletResponse response) throws IOException {
        BaselineExperiment experiment =
                (BaselineExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);

        proteomicsExperimentDownloadSupplier.write(response, preferences, experiment, "tsv");
    }

    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE, params = "type=PROTEOMICS_BASELINE_DIA")
    public void proteomicsDiaExperimentDownload(
            @PathVariable String experimentAccession,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @ModelAttribute("preferences") @Valid ProteomicsBaselineRequestPreferences preferences,
            HttpServletResponse response) throws IOException {
            var experiment = (BaselineExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);
        proteomicsExperimentDownloadSupplier.write(response, preferences, experiment, "tsv");
    }

    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE, params = "type=RNASEQ_MRNA_BASELINE")
    public void
    rnaSeqBaselineExperimentDownload(
            @PathVariable String experimentAccession,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @ModelAttribute("preferences") @Valid RnaSeqBaselineRequestPreferences preferences,
            HttpServletResponse response) throws IOException {
        BaselineExperiment experiment =
                (BaselineExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);

        rnaSeqBaselineExperimentDownloadSupplier.write(response, preferences, experiment, "tsv");
    }


    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE, params = "type=RNASEQ_MRNA_DIFFERENTIAL")
    public void
    rnaSeqDifferentialExperimentDownload(
            @PathVariable String experimentAccession,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @ModelAttribute("preferences") @Valid DifferentialRequestPreferences preferences,
            HttpServletResponse response) throws IOException {
        DifferentialExperiment experiment =
                (DifferentialExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);

        bulkDifferentialExperimentDownloadSupplier.write(response, preferences, experiment, "tsv");
    }

    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE, params = "type=PROTEOMICS_DIFFERENTIAL")
    public void
    proteomicsDifferentialExperimentDownload(
            @PathVariable String experimentAccession,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @ModelAttribute("preferences") @Valid DifferentialRequestPreferences preferences,
            HttpServletResponse response) throws IOException {
        DifferentialExperiment experiment =
                (DifferentialExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);

        bulkDifferentialExperimentDownloadSupplier.write(response, preferences, experiment, "tsv");
    }

    void microarrayExperimentDownload(String experimentAccession,
                                      String accessKey,
                                      MicroarrayRequestPreferences preferences,
                                      HttpServletResponse response) {
        MicroarrayExperiment experiment =
                (MicroarrayExperiment) experimentTrader.getExperiment(experimentAccession, accessKey);

        microarrayExperimentDownloadSupplier.write(response, preferences, experiment, "tsv");
    }

    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE, params = "type=MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL")
    public void
    microarray1ColourMRnaExperimentDownload(@PathVariable String experimentAccession,
                                            @RequestParam(value = "accessKey", required = false) String accessKey,
                                            @ModelAttribute("preferences")
                                            @Valid MicroarrayRequestPreferences preferences,
                                            HttpServletResponse response) {
        microarrayExperimentDownload(experimentAccession, accessKey, preferences, response);
    }

    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE, params = "type=MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL")
    public void
    microarray2ColourMRnaExperimentDownload(@PathVariable String experimentAccession,
                                            @RequestParam(value = "accessKey", required = false) String accessKey,
                                            @ModelAttribute("preferences")
                                            @Valid MicroarrayRequestPreferences preferences,
                                            HttpServletResponse response) {
        microarrayExperimentDownload(experimentAccession, accessKey, preferences, response);
    }

    @RequestMapping(value = DOWNLOAD_URL_TEMPLATE, params = "type=MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL")
    public void
    microarrayExperiment1ColourMicroRnaDownload(@PathVariable String experimentAccession,
                                                @RequestParam(value = "accessKey", required = false) String accessKey,
                                                @ModelAttribute("preferences")
                                                @Valid MicroarrayRequestPreferences preferences,
                                                HttpServletResponse response) {
        microarrayExperimentDownload(experimentAccession, accessKey, preferences, response);
    }

    @RequestMapping(value = "experiments/download/zip",
            method = RequestMethod.GET,
            produces = "application/zip")
    public void
    downloadMultipleExperimentsArchive(HttpServletResponse response,
                                       @RequestParam(value = "accession", defaultValue = "") List<String> accessions)
            throws IOException {

        List<Experiment> experiments = new ArrayList<>();

        accessions.stream().forEach(accession -> {
            try {
                experiments.add(experimentTrader.getPublicExperiment(accession));
            } catch (Exception e) {
                LOGGER.debug("Invalid experiment accession: {}", accession);
            }
        });

        if (!experiments.isEmpty()) {
            var archiveName = experiments.size() + "-experiment-files.zip";
            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + archiveName);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/zip");
            var zipOutputStream = new ZipOutputStream(response.getOutputStream());

            for (var experiment : experiments) {
                var experimentType = experiment.getType();
                var paths = ImmutableList.<Path>builder();
                switch (experimentType) {
                    case PROTEOMICS_BASELINE:
                        paths.add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.CONFIGURATION))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.BASELINE_FACTORS))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.IDF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.PROTEOMICS_B_MAIN))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.SUMMARY_PDF));
                        break;

                    case RNASEQ_MRNA_DIFFERENTIAL:
                        paths.add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONFIGURATION))
                                .add(experimentFileLocationService.getFilePath(

                                        experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.IDF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.RNASEQ_D_ANALYTICS));
                        break;

                    case RNASEQ_MRNA_BASELINE:
                        paths.add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONFIGURATION)).
                                add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.IDF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.BASELINE_FACTORS))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.RNASEQ_B_TPM));
                        break;

                    case PROTEOMICS_BASELINE_DIA:
                        paths.add(experimentFileLocationService.getFilePath(
                                experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.CONFIGURATION))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.BASELINE_FACTORS))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.IDF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.PROTEOMICS_B_MAIN));
                        break;
                    //MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL, MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL, MICROARRAY_2COLOUR_MICRORNA_DIFFERENTIAL
                    default:
                        paths.addAll(experimentFileLocationService.getFilePathsForArchive(
                                experiment, ExperimentFileType.MICROARRAY_D_ANALYTICS))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.CONFIGURATION))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.CONDENSE_SDRF))
                                .add(experimentFileLocationService.getFilePath(
                                        experiment.getAccession(), ExperimentFileType.IDF));
                        break;
                }

                for (var path : paths.build()) {
                    var file = path.toFile();
                    if (file.exists()) {
                        zipOutputStream.putNextEntry(new ZipEntry(experiment.getAccession() + "/" + file.getName()));
                        FileInputStream fileInputStream = new FileInputStream(file);

                        IOUtils.copy(fileInputStream, zipOutputStream);
                        fileInputStream.close();
                        zipOutputStream.closeEntry();
                    }
                }
            }
            zipOutputStream.close();
        }
    }

    @GetMapping(value = "json/experiments/download/zip/check",
            produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String
    checkMultipleExperimentsFileValid(@RequestParam(value = "accession", defaultValue = "") List<String> accessions) {
        List<Experiment> experiments = new ArrayList<>();

        accessions.stream().forEach(accession -> {
            try {
                experiments.add(experimentTrader.getPublicExperiment(accession));
            } catch (Exception e) {
                LOGGER.debug("Invalid experiment accession: {}", accession);
            }
        });

        Map<ExperimentType, ImmutableList<ExperimentFileType>> fileTypeCheckList =  new HashMap<>() {{
            put(ExperimentType.PROTEOMICS_BASELINE, ImmutableList.of(
                    ExperimentFileType.CONDENSE_SDRF,
                    ExperimentFileType.CONFIGURATION,
                    ExperimentFileType.BASELINE_FACTORS,
                    ExperimentFileType.IDF,
                    ExperimentFileType.SUMMARY_PDF,
                    ExperimentFileType.PROTEOMICS_B_MAIN));
            put(ExperimentType.RNASEQ_MRNA_DIFFERENTIAL, ImmutableList.of(
                    ExperimentFileType.CONDENSE_SDRF,
                    ExperimentFileType.CONFIGURATION,
                    ExperimentFileType.RNASEQ_D_ANALYTICS,
                    ExperimentFileType.IDF));
            put(ExperimentType.RNASEQ_MRNA_BASELINE, ImmutableList.of(
                    ExperimentFileType.CONDENSE_SDRF,
                    ExperimentFileType.CONFIGURATION,
                    ExperimentFileType.BASELINE_FACTORS,
                    ExperimentFileType.RNASEQ_B_TPM,
                    ExperimentFileType.IDF));
            put(ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL, ImmutableList.of(
                    ExperimentFileType.CONDENSE_SDRF,
                    ExperimentFileType.CONFIGURATION,
                    ExperimentFileType.MICROARRAY_D_ANALYTICS,
                    ExperimentFileType.IDF));
            put(ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL, ImmutableList.of(
                    ExperimentFileType.CONDENSE_SDRF,
                    ExperimentFileType.CONFIGURATION,
                    ExperimentFileType.MICROARRAY_D_ANALYTICS,
                    ExperimentFileType.IDF));
            put(ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL, ImmutableList.of(
                    ExperimentFileType.CONDENSE_SDRF,
                    ExperimentFileType.CONFIGURATION,
                    ExperimentFileType.MICROARRAY_D_ANALYTICS,
                    ExperimentFileType.IDF));
            put(ExperimentType.PROTEOMICS_BASELINE_DIA, ImmutableList.of(
                    ExperimentFileType.CONDENSE_SDRF,
                    ExperimentFileType.CONFIGURATION,
                    ExperimentFileType.BASELINE_FACTORS,
                    ExperimentFileType.IDF,
                    ExperimentFileType.PROTEOMICS_B_MAIN));
        }};

        var filePaths = experiments.stream().map(experiment ->
                        Map.entry(experiment.getAccession(),
                                fileTypeCheckList.get(experiment.getType()).stream()
                                        .map(fileType -> fileType != ExperimentFileType.MICROARRAY_D_ANALYTICS ?
                                                ImmutableList.of(experimentFileLocationService
                                                        .getFilePath(experiment.getAccession(), fileType)) :
                                                experimentFileLocationService
                                                        .getFilePathsForArchive(experiment, fileType))
                                        .collect(ImmutableList.toImmutableList())

                        ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var invalidFilesList = filePaths.keySet().stream()
                .map(experiment ->
                        Map.entry(experiment, filePaths.get(experiment).stream()
                                .map(fileType -> fileType.stream()
                                        .filter(path -> !path.toFile().exists())
                                        .map(path -> path.getFileName().toString())
                                        .collect(toImmutableList())
                                )
                                .flatMap(Collection::stream)
                                .collect(toImmutableList())
                        )
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!invalidFilesList.isEmpty()) {
            LOGGER.debug("Invalid experiment files: {}", invalidFilesList);
        }
        return GSON.toJson(Collections.singletonMap("invalidFiles", invalidFilesList));
    }
}
