package uk.ac.ebi.atlas.cli.coexpression;

import com.google.gson.JsonPrimitive;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.ac.ebi.atlas.experimentimport.coexpression.BaselineCoexpressionProfileLoader;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

@Component
@Command(
        name = "baseline-coexpression",
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
        
        for(String accession : experimentAccessions) {
            deleteCount = baselineCoexpressionProfileLoader.deleteCoexpressionsProfile(accession);
            loadCount = baselineCoexpressionProfileLoader.loadBaselineCoexpressionsProfile(accession);
            LOGGER.info(String.format(
                    " deleted %, d and loaded %, d coexpression profiles for accession %s", deleteCount, loadCount, accession));

        }
        return 0;
    }

}
