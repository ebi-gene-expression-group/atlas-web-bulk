package uk.ac.ebi.atlas.cli.bioentities;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Component
@Command(
        name = "bioentities-json",
        description = "Write JSONL files for the bioentities collection")
public class BioentitiesJsonCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(BioentitiesJsonCommand.class.getName());

    @Option(names = {"-o", "--output"}, description = "directory to write output files", required = true)
    private String outputDir;

    private final BioentitiesJsonWriter bioentitiesJsonWriter;

    public BioentitiesJsonCommand(BioentitiesJsonWriter bioentitiesJsonWriter) {
        this.bioentitiesJsonWriter = bioentitiesJsonWriter;
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

        bioentitiesJsonWriter.writeAnnotationFiles(outputDir);
        bioentitiesJsonWriter.writeReactomePropertyFiles(outputDir);
        bioentitiesJsonWriter.writeArrayDesignMappingFiles(outputDir);

        return 0;
    }
}
