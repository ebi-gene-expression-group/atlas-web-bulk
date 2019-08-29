package uk.ac.ebi.atlas.experimentimport;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParser;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParserOutput;
import uk.ac.ebi.atlas.experimentimport.experimentdesign.ExperimentDesignFileWriterService;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.model.experiment.ExperimentConfiguration;
import uk.ac.ebi.atlas.trader.ConfigurationTrader;

import java.util.UUID;

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

    // Unfortunately, it’d be very convoluted to create a static method to act as a key generator for
    // bioentityIdentifiers (we’d need a lot of ad-hoc replicated code in a static context that can get a species
    // Ensembl name from the experiment accession). However, bioentityIdentifiers will perform well in public/fallback
    // as they are read-only environements.

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentContent", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "bioentityIdentifiers", allEntries = true) })
    public UUID createExperiment(String experimentAccession, boolean isPrivate) {
        var files = loadAndValidateFiles(experimentAccession);
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

    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentContent", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "bioentityIdentifiers", allEntries = true) })
    public void updateExperimentPrivate(String experimentAccession, boolean isPrivate) {
        super.updateExperimentPrivate(experimentAccession, isPrivate);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentContent", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "bioentityIdentifiers", allEntries = true) })
    public void deleteExperiment(String experimentAccession) {
        super.deleteExperiment(experimentAccession);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experiment", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentAttributes", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentContent", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "bioentityIdentifiers", allEntries = true) })
    public void updateExperimentDesign(String experimentAccession) {
        var experimentDto =
                readExperiment(experimentAccession)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Experiment " + experimentAccession + " could not be found"));

        updateExperimentDesign(
                loadAndValidateFiles(experimentAccession).getRight().getExperimentDesign(),
                experimentDto);
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
