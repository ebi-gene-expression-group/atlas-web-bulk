package uk.ac.ebi.atlas.cli;

import picocli.CommandLine;
import uk.ac.ebi.atlas.cli.experimentDesign.ExperimentDesignCommand;
import uk.ac.ebi.atlas.cli.utils.AccessionsWriter;

import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractPerAccessionCommand {

    protected static Logger LOGGER;

    @CommandLine.Option(names = {"-f", "--failed-accessions-path"}, description = "File to write failed accessions to.", required = false)
    private String failedOutputPath;

    @CommandLine.Option(names = {"-e", "--experiment"}, split = ",", description = "one or more experiment accessions", required = true)
    protected List<String> experimentAccessions;

    protected int handleFailedAccessions(List<String> failedAccessions) {
        int status = 0;
        if (failedOutputPath != null && !failedAccessions.isEmpty()) {
            LOGGER.warning(String.format("%s experiments failed", failedAccessions.size()));
            LOGGER.info(String.format("Re-run with the following arguments to re-try failed accessions: %s", String.join(",", failedAccessions)));
            AccessionsWriter writer = new AccessionsWriter(failedOutputPath, failedAccessions);
            writer.write();
            status = 1;
        }
        return status;
    }
}
