package uk.ac.ebi.atlas.trader;

import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentimport.ExperimentDto;
import uk.ac.ebi.atlas.experimentimport.GxaExperimentDao;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParser;
import uk.ac.ebi.atlas.experimentimport.idf.IdfParserOutput;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesign;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.trader.factory.BaselineExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.RnaSeqDifferentialExperimentFactory;
import uk.ac.ebi.atlas.trader.factory.MicroarrayExperimentFactory;

@Component
public class ExpressionAtlasExperimentTrader extends ExperimentTrader {
    private final BaselineExperimentFactory baselineExperimentFactory;
    private final RnaSeqDifferentialExperimentFactory differentialExperimentFactory;
    private final MicroarrayExperimentFactory microarrayExperimentFactory;

    public ExpressionAtlasExperimentTrader(GxaExperimentDao experimentDao,
                                           ExperimentDesignParser experimentDesignParser,
                                           IdfParser idfParser,
                                           BaselineExperimentFactory baselineExperimentFactory,
                                           RnaSeqDifferentialExperimentFactory differentialExperimentFactory,
                                           MicroarrayExperimentFactory microarrayExperimentFactory) {
        super(experimentDao, experimentDesignParser, idfParser);
        this.baselineExperimentFactory = baselineExperimentFactory;
        this.differentialExperimentFactory = differentialExperimentFactory;
        this.microarrayExperimentFactory = microarrayExperimentFactory;
    }

    @Override
    public Experiment getExperiment(String experimentAccession, String accessKey) {
        ExperimentDesign experimentDesign = experimentDesignParser.parse(experimentAccession);
        IdfParserOutput idfParserOutput = idfParser.parse(experimentAccession);
        ExperimentDto experimentDto = experimentDao.findExperiment(experimentAccession, accessKey);

        ExperimentType experimentType = experimentDto.getExperimentType();

        switch (experimentType) {
            case RNASEQ_MRNA_BASELINE:
            case PROTEOMICS_BASELINE:
                return baselineExperimentFactory.create(experimentDto, experimentDesign, idfParserOutput);
            case RNASEQ_MRNA_DIFFERENTIAL:
                return differentialExperimentFactory.create(experimentDto, experimentDesign, idfParserOutput);
            case MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL:
            case MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL:
            case MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL:
                return microarrayExperimentFactory.create(experimentDto, experimentDesign, idfParserOutput);
            default:
                throw new RuntimeException("Unsupported experiment type: " + experimentType);
        }
    }
}
