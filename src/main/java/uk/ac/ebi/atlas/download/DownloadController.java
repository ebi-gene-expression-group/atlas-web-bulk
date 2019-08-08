package uk.ac.ebi.atlas.download;

import org.apache.commons.net.ftp.FTPFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DownloadController extends HtmlExceptionHandlingController {

    private static final String filePath = "/pub/databases/microarray/data/atlas/experiments/atlas-latest-data.tar.gz";

    @RequestMapping(value = "/download", produces = "text/html;charset=UTF-8")
    public String getExperimentsListParameters(Model model) {

        Map<String, String> fileInfo = getFTPFileInfo();

        model.addAttribute("fileSize", fileInfo.get("fileSize"));
        model.addAttribute("fileName", fileInfo.get("fileName"));
        model.addAttribute("fileTimestamp", fileInfo.get("fileTimestamp"));

        model.addAttribute("mainTitle", "Download ");

        return "download";
    }

    private Map<String, String> getFTPFileInfo() {
        Map<String, String> fileInfo = new HashMap<>();
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect("ftp.ebi.ac.uk");
            ftpClient.login("ftp", "anonymous");
            FTPFile[] file = ftpClient.listFiles(filePath);
            fileInfo.put("fileName", file[0].getName());
            fileInfo.put("fileSize", format(file[0].getSize(), 1));
            fileInfo.put("fileTimestamp", LocalDate.ofInstant(
                    file[0].getTimestamp().getTime().toInstant(),
                    ZoneId.systemDefault()).toString());

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return fileInfo;
    }

    private String format(double bytes, int digits) {
        String[] dictionary = {"bytes", "KB", "MB", "GB", "TB", "PB"};
        int index;
        for (index = 0; index < dictionary.length; index++) {
            if (bytes < 1024) {
                break;
            }
            bytes = bytes / 1024;
        }
        return String.format("%." + digits + "f", bytes) + " " + dictionary[index];
    }
}
