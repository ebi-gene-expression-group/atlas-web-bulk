package uk.ac.ebi.atlas.cli.experimentDesign;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import uk.ac.ebi.atlas.cli.utils.FailedAccessionWriter;
import uk.ac.ebi.atlas.experimentimport.GxaExperimentCrud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Component
@Command(
        name = "update-experiment-design",
        description = "Update experiment design for a set of accessions")
public class ExperimentDesignCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ExperimentDesignCommand.class.getName());

    @Option(names = {"-e", "--experiment"}, split = ",", description = "one or more experiment accessions", required = true)
    private List<String> experimentAccessions;
    @Option(names = {"-f", "--failed-accessions-path"}, description = "File to write failed accessions to.", required = false)
    private String failedOutputPath;

    private final GxaExperimentCrud experimentCrud;

    public ExperimentDesignCommand(GxaExperimentCrud gxaExperimentCrud) {
        this.experimentCrud = gxaExperimentCrud;
    }

    @Override
    public Integer call() {

        LOGGER.info("Starting update experiment designs for accessions.");
        List<String> failedAccessions = new ArrayList<>();
        for(String accession : experimentAccessions) {
            try {
                experimentCrud.updateExperimentDesign(accession);
            } catch (RuntimeException e) {
                failedAccessions.add(accession);
                LOGGER.severe(String.format("%s failed: %s",accession, e.getMessage() ));
            } catch (Exception | Error e) {
                failedAccessions.add(accession);
                LOGGER.severe(String.format("%s failed: %s",accession, e.getMessage() ));
            }
        }

        int failed = failedAccessions.size();
        int status = 0;
        if (failed > 0) {
            LOGGER.warning(String.format("%s experiments failed", failed));
            LOGGER.info(String.format("Re-run with the following arguments to re-try failed accessions: %s", String.join(",", failedAccessions)));
            if (failedOutputPath != null) {
                FailedAccessionWriter writer = new FailedAccessionWriter(failedOutputPath, failedAccessions);
                writer.write();
            }
            status = 1;
        }

        return status;
    }
}
