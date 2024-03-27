package uk.ac.ebi.atlas.trader;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.atlas.controllers.ResourceNotFoundException;
import uk.ac.ebi.atlas.experimentimport.ExperimentCrudDao;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.trader.factory.BaselineExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.MicroarrayExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.ProteomicsDifferentialExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.RnaSeqDifferentialExperimentFactory;

@Repository
public class GxaExperimentRepository implements ExperimentRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(GxaExperimentRepository.class);

    private final ExperimentCrudDao experimentCrudDao;
    private final ExperimentDesignParser experimentDesignParser;
    private final IdfParser idfParser;
    private final BaselineExperimentFactory baselineExperimentFactory;
    private final RnaSeqDifferentialExperimentFactory rnaSeqDifferentialExperimentFactory;
    private final MicroarrayExperimentFactory microarrayExperimentFactory;
    private final ProteomicsDifferentialExperimentFactory proteomicsDifferentialExperimentFactory;

    public GxaExperimentRepository(ExperimentCrudDao experimentCrudDao,
                                   ExperimentDesignParser experimentDesignParser,
                                   IdfParser idfParser,
                                   BaselineExperimentFactory baselineExperimentFactory,
                                   RnaSeqDifferentialExperimentFactory rnaSeqDifferentialExperimentFactory,
                                   MicroarrayExperimentFactory microarrayExperimentFactory,
                                   ProteomicsDifferentialExperimentFactory proteomicsDifferentialExperimentFactory) {
        this.experimentCrudDao = experimentCrudDao;
        this.experimentDesignParser = experimentDesignParser;
        this.idfParser = idfParser;
        this.baselineExperimentFactory = baselineExperimentFactory;
        this.rnaSeqDifferentialExperimentFactory = rnaSeqDifferentialExperimentFactory;
        this.microarrayExperimentFactory = microarrayExperimentFactory;
        this.proteomicsDifferentialExperimentFactory = proteomicsDifferentialExperimentFactory;
    }

    @Override
    @Cacheable(cacheNames = "experiment", sync = true)
    public Experiment getExperiment(String experimentAccession) {
        var experimentDto = experimentCrudDao.readExperiment(experimentAccession);

        if (experimentDto == null) {
            throw new ResourceNotFoundException(
                    "Experiment with accession " + experimentAccession + " could not be found");
        }

        LOGGER.info("Building experiment {}...", experimentAccession);

        var experimentDesign = experimentDesignParser.parse(experimentDto.getExperimentAccession());
        var idfParserOutput = idfParser.parse(experimentDto.getExperimentAccession());
        switch (experimentDto.getExperimentType()) {
            case PROTEOMICS_BASELINE:
            case PROTEOMICS_BASELINE_DIA:
                return baselineExperimentFactory.create(
                        experimentDto,
                        experimentDesign,
                        idfParserOutput,
                        ImmutableSet.of("Proteomics"));
            case RNASEQ_MRNA_BASELINE:
                return baselineExperimentFactory.create(
                        experimentDto,
                        experimentDesign,
                        idfParserOutput,
                        ImmutableSet.of("RNA-Seq mRNA"));
            case RNASEQ_MRNA_DIFFERENTIAL:
                return rnaSeqDifferentialExperimentFactory.create(
                        experimentDto,
                        experimentDesign,
                        idfParserOutput,
                        ImmutableSet.of("RNA-Seq mRNA"));
            case MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL:
                return microarrayExperimentFactory.create(
                        experimentDto,
                        experimentDesign,
                        idfParserOutput,
                        ImmutableSet.of("Microarray 1-colour miRNA"));
            case MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL:
                return microarrayExperimentFactory.create(
                        experimentDto,
                        experimentDesign,
                        idfParserOutput,
                        ImmutableSet.of("Microarray 1-colour mRNA"));
            case MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL:
                return microarrayExperimentFactory.create(
                        experimentDto,
                        experimentDesign,
                        idfParserOutput,
                        ImmutableSet.of("Microarray 2-colour mRNA"));
            case PROTEOMICS_DIFFERENTIAL:
                return proteomicsDifferentialExperimentFactory.create(
                        experimentDto,
                        experimentDesign,
                        idfParserOutput,
                        ImmutableSet.of("Proteomics differential"));
            default:
                throw new IllegalArgumentException(
                        "Unable to build experiment " + experimentDto.getExperimentAccession()
                        + ": experiment type " + experimentDto.getExperimentType() + " is not supported");
        }
    }

    @Override
    public String getExperimentType(String experimentAccession) {
        return experimentCrudDao.getExperimentType(experimentAccession);
    }
}
