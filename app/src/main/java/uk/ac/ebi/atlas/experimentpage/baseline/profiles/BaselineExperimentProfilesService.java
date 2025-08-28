package uk.ac.ebi.atlas.experimentpage.baseline.profiles;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.experimentpage.baseline.topgenes.BaselineExperimentTopGenesService;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineProfile;
import uk.ac.ebi.atlas.web.BaselineRequestPreferences;

import java.util.List;

/**
 * Service for retrieving baseline expression profiles for genes in baseline experiments.
 * This service acts as a facade that delegates to either PostgreSQL-based (MarkerGeneDao) or 
 * Solr-based (BaselineExperimentProfilesDao) implementations based on the request preferences.
 * 
 * When preferences.isSpecific() is true, the service uses the PostgreSQL-based implementation
 * to fetch marker genes or specific genes. Otherwise, it uses the Solr-based implementation.
 */
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

    /**
     * Retrieves profiles for the top expressed genes in a baseline experiment.
     * 
     * If preferences.isSpecific() is true, use the PostgreSQL-based implementation (MarkerGeneDao)
     * to fetch marker genes. Otherwise, use the Solr-based implementation to fetch top genes.
     * 
     * @param experimentAccession The experiment accession
     * @param assayGroups The list of assay groups in the experiment
     * @param preferences The request preferences containing filtering criteria
     * @return A list of baseline profiles for the top expressed genes
     */
    public GeneProfilesList<BaselineProfile> getTopGeneProfiles(String experimentAccession,
                                                                List<AssayGroup> assayGroups,
                                                                BaselineRequestPreferences<?> preferences,
                                                                JsonArray columnHeaders) {
        var isGeneSearch = !preferences.getGeneQuery().terms().isEmpty();
        if (isGeneSearch) {
            List<String> topGeneIds =  baselineExperimentTopGenesService.searchSpecificGenesInBaselineExperiment(
                experimentAccession, preferences);
            return baselineExperimentProfilesDao.fetchProfiles(
                topGeneIds, assayGroups, preferences, experimentAccession);
        } else if (preferences.isSpecific()) {
            var geneProfilesList = markerGeneDao.fetchMarkerGeneProfiles(
                experimentAccession, assayGroups, preferences, columnHeaders, "factorValue");
            geneProfilesList.setTotalResultCount(fetchCount(experimentAccession, preferences));

            return geneProfilesList;
        }

        List<String> topGeneIds = 
                baselineExperimentTopGenesService.searchMostExpressedGenesInBaselineExperiment(
                        experimentAccession, preferences);
        return baselineExperimentProfilesDao.fetchProfiles(
                topGeneIds, assayGroups, preferences, experimentAccession);
    }

    /**
     * Retrieves profiles for specific genes in a baseline experiment.
     * 
     * If preferences.isSpecific() is true, use the PostgreSQL-based implementation (MarkerGeneDao)
     * to fetch specific genes. Otherwise, use the Solr-based implementation to fetch the genes.
     * 
     * @param experimentAccession The experiment accession
     * @param assayGroups The list of assay groups in the experiment
     * @param preferences The request preferences containing filtering criteria
     * @param geneIds The gene IDs to retrieve profiles for
     * @return A list of baseline profiles for the specified genes
     */
    public GeneProfilesList<BaselineProfile> getGeneProfiles(String experimentAccession,
                                                             List<AssayGroup> assayGroups,
                                                             BaselineRequestPreferences<?> preferences,
                                                             JsonArray columnHeaders,
                                                             String... geneIds) {
        ImmutableList<String> geneIdsList = ImmutableList.copyOf(geneIds);

        if (geneIdsList.isEmpty() && preferences.isSpecific()) {
            return new GeneProfilesList<>();
        }

        if (preferences.isSpecific()) {
            return markerGeneDao.fetchMarkerGeneProfiles(
                experimentAccession, assayGroups, preferences, columnHeaders, "name");
        }

        return baselineExperimentProfilesDao.fetchProfiles(
                geneIdsList, assayGroups, preferences, experimentAccession);
    }

    /**
     * Retrieves the count of genes matching the criteria in a baseline experiment.
     * 
     * If preferences.isSpecific() is true, use the PostgreSQL-based implementation (MarkerGeneDao)
     * to fetch the count. Otherwise, use the Solr-based implementation.
     * 
     * @param experimentAccession The experiment accession
     * @param preferences The request preferences containing filtering criteria
     * @return The count of genes matching the criteria
     */
    public long fetchCount(String experimentAccession, BaselineRequestPreferences<?> preferences) {
        if (preferences.isSpecific()) {
            return markerGeneDao.fetchCount(experimentAccession, preferences);
        }

        return baselineExperimentProfilesDao.fetchCount(experimentAccession, preferences);
    }
}