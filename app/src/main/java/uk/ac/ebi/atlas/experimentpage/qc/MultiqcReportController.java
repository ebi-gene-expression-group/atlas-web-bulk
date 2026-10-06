package uk.ac.ebi.atlas.experimentpage.qc;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.resource.DataFileHub;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.text.MessageFormat;

@Controller
public class MultiqcReportController {
    private static final String MULTIQC_REPORT_URL = "experiments-content/{experimentAccession}/multiqc-report";

    public static String getMultiqcReportUrl(String experimentAccession, String accessKey) {
        return MULTIQC_REPORT_URL.replace("{experimentAccession}", experimentAccession) +
                (StringUtils.isNotEmpty(accessKey) ? "?accessKey=" + accessKey : "");
    }

    private final ExperimentTrader experimentTrader;
    private final DataFileHub dataFileHub;

    @Inject
    public MultiqcReportController(ExperimentTrader experimentTrader, DataFileHub dataFileHub) {
        this.experimentTrader = experimentTrader;
        this.dataFileHub = dataFileHub;
    }

    @ResponseBody
    @RequestMapping(value = MULTIQC_REPORT_URL, method = RequestMethod.GET)
    public void getMultiqcReport(HttpServletResponse response,
                                 @PathVariable String experimentAccession,
                                 @RequestParam(value = "accessKey", required = false) String accessKey) {
        Preconditions.checkNotNull(experimentTrader.getExperiment(experimentAccession, accessKey));

        var multiqcReport = dataFileHub.getExperimentFiles(experimentAccession).multiqcReport;
        if (!multiqcReport.exists()) {
            throw new ResourceNotFoundException(
                    MessageFormat.format("Not found: MultiQC report for experiment {0}", experimentAccession));
        }

        response.setContentType("text/html;charset=UTF-8");
        try {
            Files.copy(multiqcReport.getPath(), response.getOutputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
