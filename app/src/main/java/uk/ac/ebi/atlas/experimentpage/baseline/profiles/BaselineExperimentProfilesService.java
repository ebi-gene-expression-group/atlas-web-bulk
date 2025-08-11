package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.baseline.topgenes.BaselineExperimentTopGenesService;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

import java.util.List;

// Get lists of gene IDs and/or preferences from the experiment page sidebar and get baseline profiles (i.e. heatmap
// rows).

@Component
public class BaselineExperimentProfilesService {
    private final BaselineExperimentTopGenesService baselineExperimentTopGenesService;
    private final BaselineExperimentProfilesDao baselineExperimentProfilesDao;
    private final MarkerGeneDao markerGeneDao;


    public BaselineExperimentProfilesService(BaselineExperimentTopGenesService baselineExperimentTopGenesService,
                                             BaselineExperimentProfilesDao baselineExperimentProfilesDao,
                                             MarkerGeneDao markerGeneDao) {
        this.baselineExperimentTopGenesService = baselineExperimentTopGenesService;
        this.baselineExperimentProfilesDao = baselineExperimentProfilesDao;
        this.markerGeneDao = markerGeneDao;
    }

    public GeneProfilesList<BaselineProfile> getTopGeneProfiles(String experimentAccession,
                                                                List<AssayGroup> assayGroups,
                                                                BaselineRequestPreferences<?> preferences) {

        if (preferences.isSpecific()) {
            // Fetch marker genes from PostgreSQL
            return markerGeneDao.fetchMarkerGeneProfiles(experimentAccession, assayGroups, preferences);
        } else {
            // Use the existing Solr-based implementation
            List<String> topGeneIds = 
                    baselineExperimentTopGenesService.searchMostExpressedGenesInBaselineExperiment(
                            experimentAccession, preferences);

            return baselineExperimentProfilesDao.fetchProfiles(topGeneIds, assayGroups, preferences, experimentAccession);
        }
    }

    public GeneProfilesList<BaselineProfile> getGeneProfiles(String experimentAccession,
                                                            List<AssayGroup> assayGroups,
                                                            BaselineRequestPreferences<?> preferences,
                                                            String... geneIds) {
        if (preferences.isSpecific()) {
            // Fetch specific genes from PostgreSQL
            return markerGeneDao.fetchSpecificGeneProfiles(
                    ImmutableList.copyOf(geneIds), experimentAccession, assayGroups, preferences);
        } else {
            // Use the existing Solr-based implementation
            return baselineExperimentProfilesDao.fetchProfiles(
                    ImmutableList.copyOf(geneIds), assayGroups, preferences, experimentAccession);
        }
    }


    public long fetchCount(String experimentAccession, BaselineRequestPreferences<?> preferences) {
        if (preferences.isSpecific()) {
            // Get count from PostgreSQL
            return markerGeneDao.fetchCount(experimentAccession, preferences);
        } else {
            // Use the existing Solr-based implementation
            return baselineExperimentProfilesDao.fetchCount(experimentAccession, preferences);
        }
    }
}
