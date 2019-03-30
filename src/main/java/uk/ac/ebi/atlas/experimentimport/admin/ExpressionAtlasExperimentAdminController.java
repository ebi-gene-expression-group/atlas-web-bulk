package uk.ac.ebi.atlas.experimentimport.admin;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrudFactory;
import uk.ac.ebi.atlas.experimentimport.ExpressionAtlasExperimentChecker;
import uk.ac.ebi.atlas.experimentimport.GxaExperimentDao;
import uk.ac.ebi.atlas.experimentimport.analyticsindex.AnalyticsIndexerManager;
import uk.ac.ebi.atlas.experimentimport.coexpression.BaselineCoexpressionProfileLoader;
import uk.ac.ebi.atlas.resource.DataFileHub;

@Controller
@Scope("request")
@RequestMapping("/admin/experiments")
public class ExpressionAtlasExperimentAdminController extends ExperimentAdminController {
    public ExpressionAtlasExperimentAdminController(DataFileHub dataFileHub,
                                                    ExperimentCrudFactory experimentCrudFactory,
                                                    GxaExperimentDao experimentDao,
                                                    ExpressionAtlasExperimentChecker experimentChecker,
                                                    BaselineCoexpressionProfileLoader baselineCoexpressionProfileLoader,
                                                    AnalyticsIndexerManager analyticsIndexerManager) {
        super(
                new ExperimentOps(
                        new ExperimentOpLogWriter(dataFileHub),
                        new ExpressionAtlasExperimentOpsExecutionService(
                                experimentCrudFactory.create(experimentDao, experimentChecker),
                                baselineCoexpressionProfileLoader,
                                analyticsIndexerManager)));
    }
}
