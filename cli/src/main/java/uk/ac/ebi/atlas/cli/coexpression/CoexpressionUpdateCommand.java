package uk.ac.ebi.atlas.cli.coexpression;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.ac.ebi.atlas.cli.AbstractPerAccessionCommand;
import uk.ac.ebi.atlas.cli.utils.AccessionsWriter;
import uk.ac.ebi.atlas.experimentimport.coexpression.BaselineCoexpressionProfileLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Component
@Command(
        name = "update-baseline-coexpression",
        description = "Update coexpression for baseline experiments")
public class CoexpressionUpdateCommand extends AbstractPerAccessionCommand implements Callable<Integer> {

    private static final Logger LOGGER = Logger.getLogger(CoexpressionUpdateCommand.class.getName());

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
        LOGGER.info(String.format("%s experiments done", done));

        return handleFailedAccessions(failedAccessions);
    }

}
