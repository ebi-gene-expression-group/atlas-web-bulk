package uk.ac.ebi.atlas.experimentimport.admin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrud;
import uk.ac.ebi.atlas.experimentimport.analyticsindex.AnalyticsIndexerManager;
import uk.ac.ebi.atlas.experimentimport.coexpression.BaselineCoexpressionProfileLoader;

import static org.mockito.Mockito.doNothing;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentOpsExecutionServiceTest {
    private static final String ACCESSION = "E-EXAMPLE-1";

    @Mock
    private ExperimentCrud experimentCrudMock;

    @Mock
    private BaselineCoexpressionProfileLoader baselineCoexpressionProfileLoader;

    @Mock
    private AnalyticsIndexerManager analyticsIndexerManager;

    private ExperimentOpsExecutionService subject;

    @Before
    public void setUp() {
        subject =
                new ExpressionAtlasExperimentOpsExecutionService(
                        experimentCrudMock,
                        baselineCoexpressionProfileLoader,
                        analyticsIndexerManager);
    }


    // No need to verify interactions: if any stub isn’t used Mockito will throw an exception

    @Test
    public void updateExperimentDesignShouldRemoveExperimentFromCache() throws Exception {
        doNothing().when(experimentCrudMock).updateExperimentDesign(ACCESSION);
        subject.attemptExecuteStatefulOp(ACCESSION, Op.UPDATE_DESIGN);
    }

    @Test
    public void updateExperimentToPrivateShouldRemoveExperimentFromAnalyticsIndex() throws Exception {
        doNothing().when(analyticsIndexerManager).deleteFromAnalyticsIndex(ACCESSION);
        doNothing().when(experimentCrudMock).updateExperimentPrivate(ACCESSION, true);
        subject.attemptExecuteStatefulOp(ACCESSION, Op.UPDATE_PRIVATE);
    }

    @Test
    public void deleteExperimentShouldRemoveExperimentFromAnalyticsIndex() throws Exception {
        doNothing().when(analyticsIndexerManager).deleteFromAnalyticsIndex(ACCESSION);
        doNothing().when(experimentCrudMock).deleteExperiment(ACCESSION);
        subject.attemptExecuteStatefulOp(ACCESSION, Op.DELETE);
    }
}
