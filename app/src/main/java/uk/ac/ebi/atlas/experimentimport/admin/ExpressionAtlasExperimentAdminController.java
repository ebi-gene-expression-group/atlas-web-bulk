package uk.ac.ebi.atlas.experimentimport.admin;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.experimentimport.GxaExperimentCrud;
import uk.ac.ebi.atlas.experimentimport.analyticsindex.AnalyticsIndexerManager;
import uk.ac.ebi.atlas.experimentimport.coexpression.BaselineCoexpressionProfileLoader;
import uk.ac.ebi.atlas.resource.DataFileHub;

@Controller
@Scope("request")
@RequestMapping("/admin/experiments")
public class ExpressionAtlasExperimentAdminController extends ExperimentAdminController {
    public ExpressionAtlasExperimentAdminController(DataFileHub dataFileHub,
                                                    GxaExperimentCrud experimentCrud,
                                                    BaselineCoexpressionProfileLoader baselineCoexpressionProfileLoader,
                                                    AnalyticsIndexerManager analyticsIndexerManager) {
        super(
                new ExperimentOps(
                        new ExperimentOpLogWriter(dataFileHub),
                        new ExpressionAtlasExperimentOpsExecutionService(
                                experimentCrud,
                                baselineCoexpressionProfileLoader,
                                analyticsIndexerManager)));
    }
}
