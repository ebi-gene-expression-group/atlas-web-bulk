package uk.ac.ebi.atlas.home;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.atlassian.util.concurrent.LazyReference;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.experiments.ExperimentJsonSerializer;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

public class LatestExperimentsService {
    private final LatestExperimentsDao latestExperimentsDao;
    private final ExperimentTrader experimentTrader;
    private final ImmutableSet<ExperimentType> experimentTypes;

    private final LazyReference<ImmutableMap<String, Object>> latestExperimentsAttributes =
            new LazyReference<>() {
                @Override
                    protected ImmutableMap<String, Object> create() {

                        var experimentCount = latestExperimentsDao.fetchPublicExperimentsCount(experimentTypes);
                        var latestExperimentInfo =
                                latestExperimentsDao.fetchLatestExperimentAccessions(experimentTypes).stream()
                                        .map(experimentTrader::getPublicExperiment)
                                        .map(ExperimentJsonSerializer::serialize)
                                        .collect(toImmutableSet());

                        return ImmutableMap.of(
                                "experimentCount", experimentCount,
                                "formattedExperimentCount",
                                NumberFormat.getNumberInstance(Locale.US).format(experimentCount),
                                "latestExperiments", latestExperimentInfo);
                    }
                };

    public LatestExperimentsService(LatestExperimentsDao latestExperimentsDao,
                                    ExperimentTrader experimentTrader,
                                    Set<ExperimentType> experimentTypes) {
        this.latestExperimentsDao = latestExperimentsDao;
        this.experimentTrader = experimentTrader;
        this.experimentTypes = ImmutableSet.copyOf(experimentTypes);
    }

    public ImmutableMap<String, Object> fetchLatestExperimentsAttributes() {
        return latestExperimentsAttributes.get();
    }
}
