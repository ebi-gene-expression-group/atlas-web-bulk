package uk.ac.ebi.atlas.cli.analytics;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.ac.ebi.atlas.cli.utils.AccessionsWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Component
@Command(
        name = "bulk-analytics-json",
        description = "Write JSONL files for the bulk-analytics collection for the ")
public class BulkAnalyticsJsonCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(BulkAnalyticsJsonCommand.class.getName());

    @Option(names = {"-o", "--output"}, description = "output directory used to write JSONL files", required = true)
    private String outputDir;

    @Option(names = {"-e", "--experiment"}, split = ",", description = "one or more experiment accessions", required = true)
    private List<String> experimentAccessions;

    @Option(names = {"-i", "--input"}, description = "optional path of bioentitiy-to-bioentity properties map file", required = false)
    private Optional<String> inputFile;

    @Option(names = {"-f", "--failed-accessions-path"}, description = "File to write failed accessions to.", required = false)
    private String failedOutputPath;

    private final BulkAnalyticsJsonWriter bulkAnalyticsJsonWriter;

    public BulkAnalyticsJsonCommand(BulkAnalyticsJsonWriter bulkAnalyticsJsonWriter) {
        this.bulkAnalyticsJsonWriter = bulkAnalyticsJsonWriter;
    }

    @Override
    public Integer call() {
        // Create output path if it doesn’t exist
        try {
            var outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (IOException e) {
            LOGGER.severe(outputDir + " is a file or you don’t have permissions to create it");
            return 1;
        }

        inputFile.ifPresentOrElse(
                inputFile -> bulkAnalyticsJsonWriter.writeJsonLFiles(ImmutableList.copyOf(experimentAccessions), outputDir, inputFile),
                () -> bulkAnalyticsJsonWriter.writeJsonLFiles(ImmutableList.copyOf(experimentAccessions), outputDir));

        List<String> failedAccessions = bulkAnalyticsJsonWriter.getFailedAccessions();
        int status = 0;
        if (failedOutputPath != null && !failedAccessions.isEmpty()) {
            FailedAccessionWriter writer = new FailedAccessionWriter(failedOutputPath, failedAccessions);
            writer.write();
            status = 1;
        }

        return status;
    }
}
