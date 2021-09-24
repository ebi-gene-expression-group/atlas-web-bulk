package uk.ac.ebi.atlas.bioentity;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityCardModelFactory;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityPropertyDao;
import uk.ac.ebi.atlas.search.SemanticQuery;
import uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName;
import uk.ac.ebi.atlas.solr.analytics.AnalyticsSearchService;
import uk.ac.ebi.atlas.solr.analytics.baseline.BaselineAnalyticsSearchService;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;
import uk.ac.ebi.atlas.species.SpeciesInferrer;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import static uk.ac.ebi.atlas.bioentity.properties.BioEntityCardProperties.BIOENTITY_PROPERTY_NAMES;

@Controller
public class GenePageController extends BioentityPageController {
    private final BioEntityPropertyDao bioentityPropertyDao;

    public GenePageController(BaselineAnalyticsSearchService baselineAnalyticsSearchService,
                              BioEntityCardModelFactory bioEntityCardModelFactory,
                              SpeciesFactory speciesFactory,
                              SpeciesInferrer speciesInferrer,
                              AnalyticsSearchService analyticsSearchService,
                              BioEntityPropertyDao bioentityPropertyDao) {
        super(
                baselineAnalyticsSearchService,
                bioEntityCardModelFactory,
                speciesFactory,
                speciesInferrer,
                analyticsSearchService);
        this.bioentityPropertyDao = bioentityPropertyDao;
    }

    @RequestMapping(value = "/genes/{identifier:.*}", produces = "text/html;charset=UTF-8")
    public String showGenePage(@PathVariable String identifier, Model model) {
        if (identifier.toUpperCase().startsWith("MGI")) {
            Set<String> correspondingEnsemblIdentifiers =
                    bioentityPropertyDao.fetchGeneIdsForPropertyValue(BioentityPropertyName.MGI_ID, identifier);
            if (correspondingEnsemblIdentifiers.size() > 0) {
                return MessageFormat.format("redirect:/genes/{0}", correspondingEnsemblIdentifiers.iterator().next());
            }
        }

        Species speciesReferenceName = speciesInferrer.inferSpeciesForGeneQuery(SemanticQuery.create(identifier));

        String geneName =
                String.join(
                        "/",
                        bioentityPropertyDao.fetchPropertyValuesForGeneId(identifier, BioentityPropertyName.SYMBOL));

        Map<BioentityPropertyName, Set<String>> propertyValuesByType =
                bioentityPropertyDao.fetchGenePageProperties(identifier);

        ImmutableSet<String> experimentTypes = analyticsSearchService.fetchExperimentTypes(identifier);

        return super.showBioentityPage(
                identifier,
                speciesReferenceName.getName(),
                geneName,
                model,
                experimentTypes,
                BIOENTITY_PROPERTY_NAMES,
                propertyValuesByType);
    }
}
