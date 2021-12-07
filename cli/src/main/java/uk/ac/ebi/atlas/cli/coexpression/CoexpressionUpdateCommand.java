package uk.ac.ebi.atlas.cli.coexpression;

import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.ac.ebi.atlas.cli.utils.FailedAccessionWriter;
import uk.ac.ebi.atlas.experimentimport.coexpression.BaselineCoexpressionProfileLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Component
@Command(
        name = "update-baseline-coexpression",
        description = "Update coexpression for baseline experiments")
public class CoexpressionUpdateCommand implements Callable<Integer> {

    private static final Logger LOGGER = Logger.getLogger(CoexpressionUpdateCommand.class.getName());

    @Option(names = {"-e", "--experiment"}, split = ",", description = "one or more experiment accessions, comma separated", required = true)
    private List<String> experimentAccessions;

    @Option(names = {"-f", "--failed-accessions-path"}, description = "File to write failed accessions to.", required = false)
    private String failedOutputPath;

    private final BaselineCoexpressionProfileLoader baselineCoexpressionProfileLoader;

    public CoexpressionUpdateCommand(BaselineCoexpressionProfileLoader baselineCoexpressionProfileLoader) {
        this.baselineCoexpressionProfileLoader = baselineCoexpressionProfileLoader;
    }


    @Override
    public Integer call() {
        LOGGER.info("Starting update coexpression for accessions.");
        int deleteCount;
        int loadCount;

        int done=0;
        List<String> failedAccessions = new ArrayList<>();
        for(String accession : experimentAccessions) {
            deleteCount = baselineCoexpressionProfileLoader.deleteCoexpressionsProfile(accession);
            try {
                loadCount = baselineCoexpressionProfileLoader.loadBaselineCoexpressionsProfile(accession, true);
                done++;
                LOGGER.info(String.format(
                        " deleted %,d and loaded %,d coexpression profiles for accession %s", deleteCount, loadCount, accession));
            } catch (Exception | Error e) {
                failedAccessions.add(accession);
                LOGGER.severe(String.format("%s FAILED", accession));
                LOGGER.severe(e.getMessage());
            }

        }
        int failed = failedAccessions.size();
        int status = 0;
        if (failed > 0) {
            status = 1;
            LOGGER.warning(String.format("%s experiments failed", failed));
            LOGGER.info(String.format("Re-run with the following arguments to re-try failed accessions: %s", String.join(",", failedAccessions)));
            if (failedOutputPath != null) {
                FailedAccessionWriter writer = new FailedAccessionWriter(failedOutputPath, failedAccessions);
                writer.write();
            }
        }
        LOGGER.info(String.format("%s experiments done", done));
        return status;
    }

}
