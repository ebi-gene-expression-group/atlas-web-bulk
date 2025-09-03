package uk.ac.ebi.atlas.experiments;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.trader.ExperimentTrader;

import java.util.Comparator;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_BASELINE_DIA;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.RNASEQ_MRNA_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_CELL_RNASEQ_MRNA_BASELINE;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.PROTEOMICS_DIFFERENTIAL;
import static uk.ac.ebi.atlas.model.experiment.ExperimentType.SINGLE_NUCLEUS_RNASEQ_MRNA_BASELINE;

@Component
public class ExperimentJsonService {
    private final static ImmutableList<ExperimentType> EXPERIMENT_TYPE_PRECEDENCE_LIST = ImmutableList.of(
            SINGLE_CELL_RNASEQ_MRNA_BASELINE,
            RNASEQ_MRNA_BASELINE,
            PROTEOMICS_BASELINE,
            PROTEOMICS_BASELINE_DIA,
            RNASEQ_MRNA_DIFFERENTIAL,
            MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL,
            MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL,
            MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL,
            PROTEOMICS_DIFFERENTIAL,
            SINGLE_NUCLEUS_RNASEQ_MRNA_BASELINE);

    private final ExperimentTrader experimentTrader;
    private final ExperimentJsonSerializer experimentJsonSerializer;

    public ExperimentJsonService(ExperimentTrader experimentTrader,
                                 ExperimentJsonSerializer experimentJsonSerializer) {
        this.experimentTrader = experimentTrader;
        this.experimentJsonSerializer = experimentJsonSerializer;
    }

    public JsonObject getExperimentJson(String experimentAccession, String accessKey) {
        return experimentJsonSerializer.serialize(experimentTrader.getExperiment(experimentAccession, accessKey));
    }

    public ImmutableSet<JsonObject> getPublicExperimentsJson() {
        //Sort by experiment type according to the above precedence list and then by loadDate
        return experimentTrader.getPublicExperiments().stream()
                .sorted(Comparator
                        .<Experiment>comparingInt(e ->
                                EXPERIMENT_TYPE_PRECEDENCE_LIST.indexOf(e.getType()))
                        .thenComparing(Experiment::getLoadDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(experimentJsonSerializer::serialize)
                .collect(toImmutableSet());
    }
}
