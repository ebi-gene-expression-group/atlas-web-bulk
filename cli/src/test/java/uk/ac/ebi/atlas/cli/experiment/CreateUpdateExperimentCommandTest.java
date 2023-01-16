package uk.ac.ebi.atlas.cli.experiment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import picocli.CommandLine;
import uk.ac.ebi.atlas.experimentimport.ExperimentChecker;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrudDao;
import uk.ac.ebi.atlas.experimentimport.GxaExperimentCrud;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParser;
import uk.ac.ebi.atlas.experimentimport.experimentdesign.ExperimentDesignFileWriterService;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.trader.ConfigurationTrader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CreateUpdateExperimentCommandTest {

    private CreateUpdateExperimentCommand createUpdateExperimentCommand;

    private GxaExperimentCrud gxaExperimentCrudSpy;

    @Mock
    private ExperimentCrudDao experimentCrudDao;
    @Mock
    private ExperimentDesignFileWriterService experimentDesignFileWriterService;
    @Mock
    private ConfigurationTrader configurationTrader;
    @Mock
    private ExperimentChecker experimentChecker;
    @Mock
    private CondensedSdrfParser condensedSdrfParser;
    @Mock
    private IdfParser idfParser;

    private List<UUID> randomUUIDs;

    @Before
    public void setUp() {
        gxaExperimentCrudSpy = new GxaExperimentCrud(experimentCrudDao, experimentDesignFileWriterService,
                configurationTrader, experimentChecker, condensedSdrfParser, idfParser);

        randomUUIDs = generateRandomUUIDs(10);
    }

    @Test
    public void whenNoAccessionPassed_ThenReturnCorrectExitCode() {
        createUpdateExperimentCommand = new CreateUpdateExperimentCommand(gxaExperimentCrudSpy);
        var exitCode = createUpdateExperimentCommand.call();

        final int exitCodeForNoExperimentAccessionSupplied = 1;
        assertThat(exitCode).isEqualTo(exitCodeForNoExperimentAccessionSupplied);
    }

    @Test
    public void createExperimentHasBeenCalledByNumberOfProvidedAccessions() {
        GxaExperimentCrud spiedGxaExperimentCrud = spy(gxaExperimentCrudSpy);
        createUpdateExperimentCommand = new CreateUpdateExperimentCommand(spiedGxaExperimentCrud);
        final UUID uuid1 = randomUUIDs.get(0);
        final UUID uuid2 = randomUUIDs.get(1);
        final UUID uuid3 = randomUUIDs.get(2);

        doReturn(uuid1).doReturn(uuid2).doReturn(uuid3).
                when(spiedGxaExperimentCrud).createExperiment(anyString(),eq(false));

        CommandLine cmd = new CommandLine(createUpdateExperimentCommand);

        int exitCode = cmd.execute("-e=acc1,acc2,acc3");

        assertThat(exitCode).isEqualTo(0);
        verify(spiedGxaExperimentCrud, times(3))
                .createExperiment(anyString(), eq(false));
    }

    @Test
    public void ifExperimentCreationFails_ThenRecordedInFailedAccessions() throws Exception{
        GxaExperimentCrud spiedGxaExperimentCrud = spy(gxaExperimentCrudSpy);
        createUpdateExperimentCommand = new CreateUpdateExperimentCommand(spiedGxaExperimentCrud);
        final UUID uuid1 = randomUUIDs.get(0);

        var acc1 = "acc1";
        var acc2 = "acc2";
        var acc3 = "acc3";

        var tempOutputFile = File.createTempFile("tmp", "");
        tempOutputFile.deleteOnExit();
        var expectedOutputFile = new File("src/test/resources/expectedErrorOutput.txt");

        doReturn(uuid1).
                when(spiedGxaExperimentCrud).createExperiment(acc1,false);
        doThrow(NullPointerException.class).
                when(spiedGxaExperimentCrud).createExperiment(acc2,false);
        doThrow(NullPointerException.class).
                when(spiedGxaExperimentCrud).createExperiment(acc3,false);

        CommandLine cmd = new CommandLine(createUpdateExperimentCommand);

        final String failedAccessionPath = String.format("-f=%s", tempOutputFile.getAbsolutePath());
        final String experiments = String.format("-e=%s,%s,%s", acc1, acc2, acc3);
        int exitCode = cmd.execute(failedAccessionPath, experiments);

        assertThat(exitCode).isEqualTo(1);
        assertThat(tempOutputFile).hasSameContentAs(expectedOutputFile);
    }

    private List<UUID> generateRandomUUIDs(int numberOfUUIDs) {
        List<UUID> uuids = new ArrayList<>(numberOfUUIDs);
        for (int i = 0; i < numberOfUUIDs; i++) {
            uuids.add(UUID.randomUUID());
        }

        return uuids;
    }
}