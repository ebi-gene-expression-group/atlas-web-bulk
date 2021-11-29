package uk.ac.ebi.atlas.cli.analytics;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
        name = "bioentities-map",
        description = "Write a bioentity-to-bioentity properties map to file; the source of bioentity (i.e. gene) " +
                      "IDs can be either expression experiment matrices specified by their accessions or a single " +
                      "species from the bioentities collection")
public class BioentityPropertiesMapCommand implements Callable<Integer> {
    @Option(names = {"-o", "--output"}, description = "path of output file", required = true)
    private String outputFilePath;

    @ArgGroup(exclusive = true, multiplicity = "1")
    private ExclusiveOptions exclusiveOptions;
    static class ExclusiveOptions {
        @Option(names = {"-e", "--experiment"}, split = ",", description = "one or more experiment accessions")
        private List<String> experimentAccessions = new ArrayList<>();
        @Option(names = {"-s", "--species"}, description = "species")
        private String species;
    }

    private final BioentityPropertiesMapWriter bioentityPropertiesMapWriter;

    public BioentityPropertiesMapCommand(BioentityPropertiesMapWriter bioentityPropertiesMapWriter) {
        this.bioentityPropertiesMapWriter = bioentityPropertiesMapWriter;
    }

    @Override
    public Integer call() {
        if (exclusiveOptions.experimentAccessions.isEmpty()) {
            bioentityPropertiesMapWriter.writeMap(exclusiveOptions.species, outputFilePath);
        } else {
            bioentityPropertiesMapWriter.writeMap(ImmutableSet.copyOf(exclusiveOptions.experimentAccessions), outputFilePath);
        }

        return 0;
    }
}