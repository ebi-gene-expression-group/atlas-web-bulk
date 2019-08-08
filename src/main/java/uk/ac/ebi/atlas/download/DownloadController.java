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
import java.util.Optional;

@Controller
public class DownloadController extends HtmlExceptionHandlingController {

    private static final String filePath = "/pub/databases/microarray/data/atlas/experiments/atlas-latest-data.tar.gz";

    @RequestMapping(value = "/download", produces = "text/html;charset=UTF-8")
    public String getExperimentsListParameters(Model model) {

        model.addAttribute("fileSize", getFTPFileSize());
        model.addAttribute("fileName", getFTPFileName());
        model.addAttribute("fileTimestamp", getFTPFileTimestamp());

        model.addAttribute("mainTitle", "Download ");

        return "download";
    }

    private String getFTPFileSize() {
        Optional<FTPClient> ftpClient = getFTPClient();
        String size = "";
        if (ftpClient.isPresent()) {
            Optional<FTPFile> file = getFTPFile(ftpClient.get());
            if (file.isPresent()) {
                size = format(file.get().getSize(), 1);
            }
        }
        return size;
    }

    private String getFTPFileName() {
        Optional<FTPClient> ftpClient = getFTPClient();
        String name = "";
        if (ftpClient.isPresent()) {
            Optional<FTPFile> file = getFTPFile(ftpClient.get());
            if(file.isPresent()) {
                name = file.get().getName();
            }
        }
        return name;
    }

    private String getFTPFileTimestamp() {
        Optional<FTPClient> ftpClient = getFTPClient();
        String timestamp = "";
        if (ftpClient.isPresent()) {
            Optional<FTPFile> file = getFTPFile(ftpClient.get());
            if (file.isPresent()) {
                timestamp = LocalDate.ofInstant(
                        file.get().getTimestamp().getTime().toInstant(),
                        ZoneId.systemDefault()).toString();
            }
        }
        return timestamp;
    }

    private Optional<FTPClient> getFTPClient(){
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect("ftp.ebi.ac.uk");
            ftpClient.login("ftp","anonymous");
            return Optional.of(ftpClient);
        } catch (IOException ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<FTPFile> getFTPFile(FTPClient ftpClient) {
        try {
            FTPFile[] file = ftpClient.listFiles(filePath);
            return Optional.ofNullable(file[0]);
        } catch (IOException ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    private String format(double bytes, int digits) {
        String[] dictionary = { "bytes", "KB", "MB", "GB", "TB", "PB"};
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
