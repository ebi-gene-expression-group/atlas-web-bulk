package uk.ac.ebi.atlas.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.commons.streams.ObjectInputStream;
import uk.ac.ebi.atlas.experimentimport.analytics.baseline.BaselineAnalytics;
import uk.ac.ebi.atlas.experimentimport.analytics.baseline.BaselineAnalyticsInputStreamFactory;
import uk.ac.ebi.atlas.experimentimport.analytics.differential.DifferentialAnalytics;
import uk.ac.ebi.atlas.experimentimport.analytics.differential.microarray.MicroarrayDifferentialAnalyticsInputStream;
import uk.ac.ebi.atlas.experimentimport.analytics.differential.microarray.MicroarrayDifferentialAnalyticsInputStreamFactory;
import uk.ac.ebi.atlas.experimentimport.analytics.differential.rnaseq.RnaSeqDifferentialAnalyticsInputStream;
import uk.ac.ebi.atlas.experimentimport.analytics.differential.rnaseq.RnaSeqDifferentialAnalyticsInputStreamFactory;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.io.IOException;
import java.util.HashSet;

import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;

// TODO this code could be much shorter. Make the DifferentialAnalytics and BaselineAnalytics inherit from
// TODO Analytics, with has a method getGeneId(), make the analytics input streams inherit from a common parent, and
// TODO write one method that gets all gene ids for an experiment accession.
@Service
public class ExpressionAtlasBioentityIdentifiersReader extends BioentityIdentifiersReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionAtlasBioentityIdentifiersReader.class);

    private final ExperimentTrader experimentTrader;
    private final BaselineAnalyticsInputStreamFactory baselineAnalyticsInputStreamFactory;
    private final MicroarrayDifferentialAnalyticsInputStreamFactory microarrayDifferentialAnalyticsInputStreamFactory;
    private final RnaSeqDifferentialAnalyticsInputStreamFactory rnaSeqDifferentialAnalyticsInputStreamFactory;

    public ExpressionAtlasBioentityIdentifiersReader(ExperimentTrader experimentTrader,
                                                     BaselineAnalyticsInputStreamFactory
                                                             baselineAnalyticsInputStreamFactory,
                                                     MicroarrayDifferentialAnalyticsInputStreamFactory
                                                             microarrayDifferentialAnalyticsInputStreamFactory,
                                                     RnaSeqDifferentialAnalyticsInputStreamFactory
                                                             rnaSeqDifferentialAnalyticsInputStreamFactory) {
        this.experimentTrader = experimentTrader;
        this.baselineAnalyticsInputStreamFactory = baselineAnalyticsInputStreamFactory;
        this.microarrayDifferentialAnalyticsInputStreamFactory = microarrayDifferentialAnalyticsInputStreamFactory;
        this.rnaSeqDifferentialAnalyticsInputStreamFactory = rnaSeqDifferentialAnalyticsInputStreamFactory;
    }

    @Override
    protected int addBioentityIdentifiers(HashSet<String> bioentityIdentifiers, ExperimentType experimentType) {
        if (experimentType.isBaseline()) {
            return addBioentityIdentifiersFromBaselineExperiments(bioentityIdentifiers, experimentType);
        } else {  //if (experimentType.isDifferential()) {

            if (experimentType.isMicroarray()) {
                return addBioentityIdentifiersFromMicroarrayExperiments(bioentityIdentifiers);
            } else {
                return addBioentityIdentifiersFromRnaSeqDifferentialExperiments(bioentityIdentifiers);
            }
        }
    }

    private int addBioentityIdentifiersFromBaselineExperiments(HashSet<String> bioentityIdentifiers,
                                                               ExperimentType experimentType) {
        int bioentityIdentifiersSizeWithoutNewElements = bioentityIdentifiers.size();

        for (Experiment experiment : experimentTrader.getPublicExperiments(experimentType)) {
            LOGGER.debug("Reading bioentity identifiers in {}", experiment.getAccession());

            try (ObjectInputStream<BaselineAnalytics> inputStream =
                         baselineAnalyticsInputStreamFactory.create(experiment.getAccession(), experimentType)) {
                BaselineAnalytics analytics = inputStream.readNext();
                while (analytics != null) {
                    bioentityIdentifiers.add(analytics.geneId());
                    analytics = inputStream.readNext();
                }
            } catch (Exception exception) {
                LOGGER.error(exception.getMessage());
            }
        }

        return bioentityIdentifiers.size() - bioentityIdentifiersSizeWithoutNewElements;
    }

    private int addBioentityIdentifiersFromMicroarrayExperiments(HashSet<String> bioentityIdentifiers) {
        int bioentityIdentifiersSizeWithoutNewElements = bioentityIdentifiers.size();

        for (Experiment experiment :
                experimentTrader.getPublicExperiments(
                        MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
                        MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
                        MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL)) {
            String experimentAccession = experiment.getAccession();
            LOGGER.debug("Reading bioentity identifiers in {}", experimentAccession);

            for (String arrayDesign : ((MicroarrayExperiment)experiment).getArrayDesignAccessions()) {
                try (MicroarrayDifferentialAnalyticsInputStream inputStream =
                             microarrayDifferentialAnalyticsInputStreamFactory.create(
                                     experimentAccession, arrayDesign)) {
                    DifferentialAnalytics analytics = inputStream.readNext();
                    while (analytics != null) {
                        bioentityIdentifiers.add(analytics.getGeneId());
                        analytics = inputStream.readNext();
                    }
                } catch (IOException exception) {
                    LOGGER.error(exception.getMessage());
                }
            }
        }

        return bioentityIdentifiers.size() - bioentityIdentifiersSizeWithoutNewElements;
    }

    private int addBioentityIdentifiersFromRnaSeqDifferentialExperiments(HashSet<String> bioentityIdentifiers) {
        int bioentityIdentifiersSizeWithoutNewElements = bioentityIdentifiers.size();

        for (Experiment experiment : experimentTrader.getPublicExperiments(RNASEQ_MRNA_DIFFERENTIAL)) {
            String experimentAccession = experiment.getAccession();
            LOGGER.debug("Reading bioentity identifiers in {}", experimentAccession);

            try (RnaSeqDifferentialAnalyticsInputStream inputStream =
                         rnaSeqDifferentialAnalyticsInputStreamFactory.create(experimentAccession)) {
                DifferentialAnalytics analytics = inputStream.readNext();
                while (analytics != null) {
                    bioentityIdentifiers.add(analytics.getGeneId());
                    analytics = inputStream.readNext();
                }
            } catch (IOException exception) {
                LOGGER.error(exception.getMessage());
            }
        }

        return bioentityIdentifiers.size() - bioentityIdentifiersSizeWithoutNewElements;
    }

    @Override
    public HashSet<String> getBioentityIdsFromExperiment(String experimentAccession) {
        LOGGER.info("Reading gene IDs of {}", experimentAccession);
        Experiment experiment = experimentTrader.getExperimentForAnalyticsIndex(experimentAccession);

        HashSet<String> bioentityIdentifiers = new HashSet<>();

        if (experiment.getType().isBaseline()) {

            try (ObjectInputStream<BaselineAnalytics> inputStream =
                         baselineAnalyticsInputStreamFactory.create(experimentAccession, experiment.getType())) {
                BaselineAnalytics analytics = inputStream.readNext();
                while (analytics != null) {
                    bioentityIdentifiers.add(analytics.geneId());
                    analytics = inputStream.readNext();
                }
            } catch (Exception exception) {
                LOGGER.error(exception.getMessage());
            }

        } else {  //if (experimentType.isDifferential()) {

            if (experiment.getType().isMicroarray()) {
                for (String arrayDesign : ((MicroarrayExperiment) experiment).getArrayDesignAccessions()) {

                    try (MicroarrayDifferentialAnalyticsInputStream inputStream =
                                 microarrayDifferentialAnalyticsInputStreamFactory.create(
                                         experimentAccession, arrayDesign)) {
                        DifferentialAnalytics analytics = inputStream.readNext();
                        while (analytics != null) {
                            bioentityIdentifiers.add(analytics.getGeneId());
                            analytics = inputStream.readNext();
                        }
                    } catch (IOException exception) {
                        LOGGER.error(exception.getMessage());
                    }
                }

            } else {

                try (RnaSeqDifferentialAnalyticsInputStream inputStream =
                             rnaSeqDifferentialAnalyticsInputStreamFactory.create(experimentAccession)) {
                    DifferentialAnalytics analytics = inputStream.readNext();
                    while (analytics != null) {
                        bioentityIdentifiers.add(analytics.getGeneId());
                        analytics = inputStream.readNext();
                    }
                } catch (IOException exception) {
                    LOGGER.error(exception.getMessage());
                }
            }

        }

        return bioentityIdentifiers;
    }
}
