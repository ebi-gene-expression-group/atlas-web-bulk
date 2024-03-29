package uk.ac.ebi.atlas.cli.experimentDesign;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import uk.ac.ebi.atlas.cli.AbstractPerAccessionCommand;
import uk.ac.ebi.atlas.cli.utils.AccessionsWriter;
import uk.ac.ebi.atlas.experimentimport.GxaExperimentCrud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Component
@Command(
        name = "update-experiment-design",
        description = "Update experiment design for a set of accessions")
public class ExperimentDesignCommand extends AbstractPerAccessionCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ExperimentDesignCommand.class.getName());

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
            }
        }

        return handleFailedAccessions(failedAccessions);
    }
}
