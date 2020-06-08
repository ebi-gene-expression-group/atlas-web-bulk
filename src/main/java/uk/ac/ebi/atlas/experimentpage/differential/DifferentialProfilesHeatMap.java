package uk.ac.ebi.atlas.experimentpage.differential;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.atlas.experimentpage.context.DifferentialRequestContext;
import uk.ac.ebi.atlas.model.experiment.sample.Contrast;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExpression;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialProfile;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialProfileComparator;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialProfilesList;
import uk.ac.ebi.atlas.profiles.MinMaxProfileRanking;
import uk.ac.ebi.atlas.profiles.ProfileStreamFilter;
import uk.ac.ebi.atlas.profiles.differential.DifferentialProfilesListBuilder;
import uk.ac.ebi.atlas.profiles.stream.ProfileStreamFactory;
import uk.ac.ebi.atlas.search.bioentities.BioentitiesSearchDao;
import uk.ac.ebi.atlas.web.DifferentialRequestPreferences;

import java.util.concurrent.TimeUnit;

import static uk.ac.ebi.atlas.solr.cloud.collections.BioentitiesCollectionProxy.BIOENTITY_IDENTIFIER_DV;

public class
DifferentialProfilesHeatMap<
        X extends DifferentialExpression,
        E extends DifferentialExperiment,
        P extends DifferentialProfile<X, P>,
        R extends DifferentialRequestContext<E, ? extends DifferentialRequestPreferences>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialProfilesHeatMap.class);
    private final BioentitiesSearchDao bioentitiesSearchDao;
    private final ProfileStreamFactory<Contrast, X, E, R, P> profileStreamFactory;

    public DifferentialProfilesHeatMap(ProfileStreamFactory<Contrast, X, E, R, P> profileStreamFactory,
                                       BioentitiesSearchDao bioentitiesSearchDao) {
        this.profileStreamFactory = profileStreamFactory;
        this.bioentitiesSearchDao = bioentitiesSearchDao;
    }

    public DifferentialProfilesList<P> fetch(R requestContext) {
        var stopwatch = Stopwatch.createStarted();

        var geneIds = bioentitiesSearchDao.parseStringFieldFromMatchingDocs(
                requestContext.getGeneQuery(), requestContext.getSpecies(), BIOENTITY_IDENTIFIER_DV);
        var profiles =
                profileStreamFactory.select(
                        requestContext.getExperiment(),
                        requestContext,
                        geneIds,
                        ProfileStreamFilter.create(requestContext),
                        new MinMaxProfileRanking<>(
                                DifferentialProfileComparator.create(requestContext),
                                new DifferentialProfilesListBuilder<>()));

        stopwatch.stop();

        LOGGER.debug(
                "<fetch> for [{}] gene IDs took {} secs",
                geneIds.size(),
                stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000D);

        return profiles;
    }
}
