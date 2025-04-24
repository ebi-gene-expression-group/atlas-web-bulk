package uk.ac.ebi.atlas.experimentpage.qc;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static java.nio.file.Files.newInputStream;

public class QcReportUtil {

    public static String getContent(HttpServletRequest request) {
        Path filePath = (Path) request.getAttribute("contentPath");
        StringBuilder content = new StringBuilder();
        try (InputStream f = newInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(f, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");  // Append the file content
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return content.toString();  // Return the file content as a string
    }
}
