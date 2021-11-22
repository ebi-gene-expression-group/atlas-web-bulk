package uk.ac.ebi.atlas.bioentity;

import com.google.common.collect.ImmutableSet;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.atlas.bioentity.geneset.GeneSetPropertyService;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityCardModelFactory;
import uk.ac.ebi.atlas.controllers.BioentityNotFoundException;
import uk.ac.ebi.atlas.search.SemanticQuery;
import uk.ac.ebi.atlas.solr.analytics.AnalyticsSearchService;
import uk.ac.ebi.atlas.solr.analytics.baseline.BaselineAnalyticsSearchService;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;
import uk.ac.ebi.atlas.species.SpeciesInferrer;

@Profile("!cli")
@Controller
public class GeneSetPageController extends BioentityPageController {
    private GeneSetPropertyService geneSetPropertyService;

    public GeneSetPageController(BaselineAnalyticsSearchService baselineAnalyticsSearchService,
                                 BioEntityCardModelFactory bioEntityCardModelFactory,
                                 SpeciesFactory speciesFactory,
                                 SpeciesInferrer speciesInferrer,
                                 AnalyticsSearchService analyticsSearchService,
                                 GeneSetPropertyService geneSetPropertyService) {
        super(
                baselineAnalyticsSearchService,
                bioEntityCardModelFactory,
                speciesFactory,
                speciesInferrer,
                analyticsSearchService);
        this.geneSetPropertyService = geneSetPropertyService;
    }

    @RequestMapping(value = "/genesets/{identifier:.*}", produces = "text/html;charset=UTF-8")
    public String showGeneSetPage(@PathVariable String identifier,
                                  @RequestParam(value = "species", required = false) String speciesReferenceName,
                                  Model model) {

        ImmutableSet<String> experimentTypes =
                analyticsSearchService.fetchExperimentTypes(SemanticQuery.create(identifier), speciesReferenceName);

        if (experimentTypes.isEmpty()) {
            throw new BioentityNotFoundException("Gene set <em>" + identifier + "</em> not found.");
        }

        Species species = speciesInferrer.inferSpeciesForGeneQuery(SemanticQuery.create(identifier));

        return super.showBioentityPage(
                identifier,
                species.getName(),
                identifier,
                model,
                experimentTypes,
                GeneSetPropertyService.ALL,
                geneSetPropertyService.propertyValuesByType(identifier, species.isPlant()));
    }
}
