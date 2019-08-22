package uk.ac.ebi.atlas.experimentimport;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParser;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParserOutput;
import uk.ac.ebi.atlas.experimentimport.experimentdesign.ExperimentDesignFileWriterService;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.model.experiment.ExperimentConfiguration;
import uk.ac.ebi.atlas.trader.ConfigurationTrader;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GxaExperimentCrud extends ExperimentCrud {
    private final ConfigurationTrader configurationTrader;
    private final ExperimentChecker experimentChecker;
    private final CondensedSdrfParser condensedSdrfParser;

    private final IdfParser idfParser;

    public GxaExperimentCrud(ExperimentCrudDao experimentCrudDao,
                             ExperimentDesignFileWriterService experimentDesignFileWriterService,
                             ConfigurationTrader configurationTrader,
                             ExperimentChecker experimentChecker,
                             CondensedSdrfParser condensedSdrfParser,
                             IdfParser idfParser) {
        super(experimentCrudDao, experimentDesignFileWriterService);
        this.experimentChecker = experimentChecker;
        this.condensedSdrfParser = condensedSdrfParser;
        this.configurationTrader = configurationTrader;
        this.idfParser = idfParser;
    }

    @Override
    public UUID createExperiment(String experimentAccession, boolean isPrivate) {
        var files = loadAndValidateFiles(experimentAccession);
        var experimentConfiguration = files.getLeft();
        var condensedSdrfParserOutput = files.getRight();
        var idfParserOutput = idfParser.parse(experimentAccession);
        var accessKey = readExperiment(experimentAccession).map(ExperimentDto::getAccessKey);

        var experimentDto = new ExperimentDto(
                condensedSdrfParserOutput.getExperimentAccession(),
                condensedSdrfParserOutput.getExperimentType(),
                condensedSdrfParserOutput.getSpecies(),
                idfParserOutput.getPubmedIds(),
                idfParserOutput.getDois(),
                isPrivate,
                accessKey.orElseGet(() -> UUID.randomUUID().toString()));

        if (accessKey.isPresent()) {
            experimentCrudDao.updateExperiment(experimentDto);
        } else {
            experimentCrudDao.createExperiment(experimentDto);
            updateExperimentDesign(condensedSdrfParserOutput.getExperimentDesign(), experimentDto);
        }
        return UUID.fromString(experimentDto.getAccessKey());
    }

    @Override
    public void updateExperimentDesign(String experimentAccession) {
        readExperiment(experimentAccession)
                .ifPresent(experimentDto ->
                        updateExperimentDesign(
                                loadAndValidateFiles(experimentAccession).getRight().getExperimentDesign(),
                                experimentDto));
    }

    private Pair<ExperimentConfiguration, CondensedSdrfParserOutput> loadAndValidateFiles(String experimentAccession) {
        var experimentConfiguration = configurationTrader.getExperimentConfiguration(experimentAccession);
        experimentChecker.checkAllFiles(experimentAccession, experimentConfiguration.getExperimentType());

        var condensedSdrfParserOutput =
                condensedSdrfParser.parse(experimentAccession, experimentConfiguration.getExperimentType());

        new ExperimentFilesCrossValidator(experimentConfiguration, condensedSdrfParserOutput.getExperimentDesign())
                .validate();

        return Pair.of(experimentConfiguration, condensedSdrfParserOutput);
    }
}
