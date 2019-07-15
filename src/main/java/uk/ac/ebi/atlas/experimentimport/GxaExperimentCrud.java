package uk.ac.ebi.atlas.experimentimport;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParser;
import uk.ac.ebi.atlas.experimentimport.condensedSdrf.CondensedSdrfParserOutput;
import uk.ac.ebi.atlas.experimentimport.experimentdesign.ExperimentDesignFileWriterService;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.model.experiment.ExperimentConfiguration;
import uk.ac.ebi.atlas.trader.ConfigurationTrader;

import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class GxaExperimentCrud extends ExperimentCrud {
    private final ConfigurationTrader configurationTrader;

    public GxaExperimentCrud(GxaExperimentDao gxaExperimentDao,
                              ExperimentChecker experimentChecker,
                              CondensedSdrfParser condensedSdrfParser,
                              IdfParser idfParser,
                              ExperimentDesignFileWriterService experimentDesignFileWriterService,
                              ConfigurationTrader configurationTrader) {
        super(gxaExperimentDao, experimentChecker, condensedSdrfParser, idfParser, experimentDesignFileWriterService);
        this.configurationTrader = configurationTrader;
    }


    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public UUID importExperiment(String experimentAccession, boolean isPrivate) {
        checkNotNull(experimentAccession);

        var files = loadAndValidateFiles(experimentAccession);
        var experimentConfiguration = files.getLeft();
        var condensedSdrfParserOutput = files.getRight();
        var idfParserOutput = idfParser.parse(experimentAccession);
        var accessKey = fetchExperimentAccessKey(experimentAccession);

        var experimentDTO = ExperimentDto.create(
                condensedSdrfParserOutput,
                idfParserOutput,
                condensedSdrfParserOutput
                        .getExperimentDesign()
                        .getSpeciesForAssays(
                                experimentConfiguration.getAssayGroups().stream()
                                        .flatMap(assayGroup -> assayGroup.getAssayIds().stream())
                                        .collect(Collectors.toSet())),
                isPrivate);

        if (accessKey.isPresent()) {
            deleteExperiment(experimentAccession);
        }

        var accessKeyUuid = accessKey.map(UUID::fromString).orElseGet(UUID::randomUUID);
        experimentDao.addExperiment(experimentDTO, accessKeyUuid);

        updateWithNewExperimentDesign(condensedSdrfParserOutput.getExperimentDesign(), experimentDTO);

        return accessKeyUuid;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public void updateExperimentDesign(String experimentAccession) {
        updateWithNewExperimentDesign(
                loadAndValidateFiles(experimentAccession).getRight().getExperimentDesign(),
                experimentDao.getExperimentAsAdmin(experimentAccession));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", allEntries = true),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public void makeExperimentPrivate(String experimentAccession) {
        super.makeExperimentPrivate(experimentAccession);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "experimentByAccession", key = "#experimentAccession"),
            @CacheEvict(cacheNames = "experimentsByType", allEntries = true) })
    public void makeExperimentPublic(String experimentAccession) {
        super.makeExperimentPublic(experimentAccession);
    }

    private Pair<ExperimentConfiguration, CondensedSdrfParserOutput> loadAndValidateFiles(String experimentAccession) {
        var experimentConfiguration = configurationTrader.getExperimentConfiguration(experimentAccession);
        experimentChecker.checkAllFiles(experimentAccession, experimentConfiguration.getExperimentType());

        var condensedSdrfParserOutput = condensedSdrfParser.parse(experimentAccession, experimentConfiguration.getExperimentType());

        new ExperimentFilesCrossValidator(experimentConfiguration, condensedSdrfParserOutput.getExperimentDesign())
                .validate();

        return Pair.of(experimentConfiguration, condensedSdrfParserOutput);
    }
}
