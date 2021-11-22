package uk.ac.ebi.atlas.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import uk.ac.ebi.atlas.cli.analytics.BioentityPropertiesMapCommand;
import uk.ac.ebi.atlas.cli.analytics.BulkAnalyticsJsonCommand;
import uk.ac.ebi.atlas.cli.bioentities.BioentitiesJsonCommand;

@Command(subcommands = {
        BioentitiesJsonCommand.class,
        BioentityPropertiesMapCommand.class,
        BulkAnalyticsJsonCommand.class
})
@Component
public class ExpressionAtlasCliCommand {
}