package uk.ac.ebi.atlas.download;

import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DownloadController extends HtmlExceptionHandlingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadController.class);
    private static final String FILEPATH = "/pub/databases/microarray/data/atlas/experiments/atlas-latest-data.tar.gz";

    @RequestMapping(value = "/download", produces = "text/html;charset=UTF-8")
    public String getExperimentsListParameters(@RequestParam(defaultValue = "ftp.ebi.ac.uk", required = false) String ftpHost,
                                               Model model) {
        Map<String, String> fileInfo = getFtpFileInfo(ftpHost);

        model.addAttribute("fileSize", fileInfo.get("fileSize"));
        model.addAttribute("fileName", fileInfo.get("fileName"));
        model.addAttribute("fileTimestamp", fileInfo.get("fileTimestamp"));

        model.addAttribute("mainTitle", "Download ");

        return "download";
    }

    private Map<String, String> getFtpFileInfo(String ftpHost) {
        Map<String, String> fileInfo = new HashMap<>();
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpHost);
            ftpClient.login("ftp", "anonymous");
            FTPFile[] file = ftpClient.listFiles(FILEPATH);
            fileInfo.put("fileName", file[0].getName());
            fileInfo.put("fileSize", format(file[0].getSize(), 1));
            fileInfo.put("fileTimestamp", LocalDate.ofInstant(
                    file[0].getTimestamp().getTime().toInstant(),
                    ZoneId.systemDefault()).toString());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return fileInfo;
    }

    private String format(final double bytes, final int digits) {
        String[] dictionary = {"bytes", "KiB", "MiB", "GiB", "TiB", "PiB"};
        final var base = 1024;
        Double index = Math.floor(Math.log(bytes) / Math.log(base));
        var size = bytes / Math.pow(base, index);
        return String.format("%." + digits + "f", size) + " " + dictionary[index.intValue()];
    }
}
