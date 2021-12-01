package uk.ac.ebi.atlas.cli.coexpression;

import com.google.gson.JsonPrimitive;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
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
        List<String> failed_accessions = new ArrayList<>();
        for(String accession : experimentAccessions) {
            deleteCount = baselineCoexpressionProfileLoader.deleteCoexpressionsProfile(accession);
            try {
                loadCount = baselineCoexpressionProfileLoader.loadBaselineCoexpressionsProfile(accession, true);
                done++;
                LOGGER.info(String.format(
                        " deleted %, d and loaded %, d coexpression profiles for accession %s", deleteCount, loadCount, accession));
            } catch (IOException e) {
                failed_accessions.add(accession);
                LOGGER.severe(String.format("%s FAILED", accession));
                LOGGER.severe(e.getMessage());
            }

        }
        int failed = failed_accessions.size();
        LOGGER.warning(String.format("% experiments failed", failed));
        if (failed > 0) {
            LOGGER.info(String.format("Re-run with the following arguments to re-try failed accessions: %s", String.join(",", failed_accessions)));
        }
        LOGGER.info(String.format("% experiments done", done));
        return failed_accessions.size();
    }

}
