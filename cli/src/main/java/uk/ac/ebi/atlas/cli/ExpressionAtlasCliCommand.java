package uk.ac.ebi.atlas.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import uk.ac.ebi.atlas.cli.analytics.BioentityPropertiesMapCommand;
import uk.ac.ebi.atlas.cli.analytics.BulkAnalyticsJsonCommand;
import uk.ac.ebi.atlas.cli.bioentities.BioentitiesJsonCommand;
import uk.ac.ebi.atlas.cli.experimentDesign.ExperimentDesignCommand;

@Command(subcommands = {
        BioentitiesJsonCommand.class,
        BioentityPropertiesMapCommand.class,
        BulkAnalyticsJsonCommand.class,
        ExperimentDesignCommand.class
})
@Component
public class ExpressionAtlasCliCommand {
}