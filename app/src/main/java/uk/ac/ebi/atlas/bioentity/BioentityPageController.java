package uk.ac.ebi.atlas.bioentity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.ui.Model;
import uk.ac.ebi.atlas.bioentity.properties.BioEntityCardModelFactory;
import uk.ac.ebi.atlas.controllers.HtmlExceptionHandlingController;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.search.SemanticQuery;
import uk.ac.ebi.atlas.solr.BioentityPropertyName;
import uk.ac.ebi.atlas.solr.analytics.AnalyticsSearchService;
import uk.ac.ebi.atlas.solr.analytics.baseline.BaselineAnalyticsSearchService;
import uk.ac.ebi.atlas.species.Species;
import uk.ac.ebi.atlas.species.SpeciesFactory;
import uk.ac.ebi.atlas.species.SpeciesInferrer;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BioentityPageController extends HtmlExceptionHandlingController {
    private final BaselineAnalyticsSearchService baselineAnalyticsSearchService;
    private final BioEntityCardModelFactory bioEntityCardModelFactory;
    private final SpeciesFactory speciesFactory;
    protected final SpeciesInferrer speciesInferrer;
    protected final AnalyticsSearchService analyticsSearchService;

    public BioentityPageController(BaselineAnalyticsSearchService baselineAnalyticsSearchService,
                                   BioEntityCardModelFactory bioEntityCardModelFactory,
                                   SpeciesFactory speciesFactory,
                                   SpeciesInferrer speciesInferrer,
                                   AnalyticsSearchService analyticsSearchService) {
        this.baselineAnalyticsSearchService = baselineAnalyticsSearchService;
        this.bioEntityCardModelFactory = bioEntityCardModelFactory;
        this.speciesFactory = speciesFactory;
        this.speciesInferrer = speciesInferrer;
        this.analyticsSearchService = analyticsSearchService;
    }

    // identifier (gene) = an Ensembl identifier (gene, transcript, or protein) or a mirna identifier or an MGI term.
    // identifier (gene set) = a Reactome id, Plant Ontology or Gene Ontology accession or an InterPro term
    public String showBioentityPage(String identifier,
                                    String speciesReferenceName,
                                    String entityName,
                                    Model model,
                                    Set<String> experimentTypes,
                                    List<BioentityPropertyName> desiredOrderOfPropertyNames,
                                    Map<BioentityPropertyName, Set<String>> propertyValuesByType) {

        boolean hasDifferentialResults = ExperimentType.containsDifferential(experimentTypes);
        boolean hasBaselineResults = ExperimentType.containsBaseline(experimentTypes);

        if (!hasDifferentialResults && !hasBaselineResults) {
            model.addAttribute("searchDescription", identifier);
            return "no-results";
        }

        model.addAttribute("hasBaselineResults", hasBaselineResults);
        model.addAttribute("hasDifferentialResults", hasDifferentialResults);

        Species species = speciesFactory.create(speciesReferenceName);
        model.addAttribute("species", species.getName());

        Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();
        if (hasBaselineResults) {
            model.addAttribute(
                    "jsonFacets",
                    gson.toJson(
                            baselineAnalyticsSearchService.findFacetsForTreeSearch(
                                    SemanticQuery.create(identifier), species)));
        }

        model.addAttribute("geneQuery", SemanticQuery.create(identifier).toUrlEncodedJson());
        model.addAllAttributes(
                bioEntityCardModelFactory.modelAttributes(
                        identifier, species, desiredOrderOfPropertyNames, entityName, propertyValuesByType));

        return "search-results";
    }
}
