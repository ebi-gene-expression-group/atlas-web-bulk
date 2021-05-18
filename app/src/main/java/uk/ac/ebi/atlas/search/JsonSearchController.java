package uk.ac.ebi.atlas.search;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;
import uk.ac.ebi.atlas.solr.analytics.baseline.BaselineAnalyticsSearchService;
import uk.ac.ebi.atlas.solr.analytics.differential.DifferentialAnalyticsSearchService;
import uk.ac.ebi.atlas.species.SpeciesFactory;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;

@RestController
public class JsonSearchController extends JsonExceptionHandlingController {
    private final BaselineAnalyticsSearchService baselineAnalyticsSearchService;
    private final DifferentialAnalyticsSearchService differentialAnalyticsSearchService;
    private final SpeciesFactory speciesFactory;

    public JsonSearchController(BaselineAnalyticsSearchService baselineAnalyticsSearchService,
                                DifferentialAnalyticsSearchService differentialAnalyticsSearchService,
                                SpeciesFactory speciesFactory) {
        this.baselineAnalyticsSearchService = baselineAnalyticsSearchService;
        this.differentialAnalyticsSearchService = differentialAnalyticsSearchService;
        this.speciesFactory = speciesFactory;
    }

    @GetMapping(value = "/json/search/baseline_facets",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getJsonBaselineFacets(
            @RequestParam(value = "geneQuery", required = false, defaultValue = "") SemanticQuery geneQuery,
            @RequestParam(value = "conditionQuery", required = false, defaultValue = "") SemanticQuery conditionQuery,
            @RequestParam(value = "organism", required = false, defaultValue = "") String species) {
        if (conditionQuery.isEmpty()) {
            return GSON.toJson(
                    baselineAnalyticsSearchService.findFacetsForTreeSearch(geneQuery, speciesFactory.create(species)));
        }

        return GSON.toJson(
              baselineAnalyticsSearchService.findFacetsForTreeSearch(
                      geneQuery, conditionQuery, speciesFactory.create(species)));
    }

    @GetMapping(value = "/json/search/differential_facets",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getDifferentialJsonFacets(
            @RequestParam(value = "geneQuery", required = false, defaultValue = "") SemanticQuery geneQuery,
            @RequestParam(value = "conditionQuery", required = false, defaultValue = "") SemanticQuery conditionQuery,
            @RequestParam(value = "species", required = false, defaultValue = "") String species) {
        if (isBlank(species)) {
            return GSON.toJson(differentialAnalyticsSearchService.fetchFacets(geneQuery, conditionQuery));
        }

        return GSON.toJson(
                differentialAnalyticsSearchService.fetchFacets(
                        geneQuery, conditionQuery, speciesFactory.create(species)));
    }

    @GetMapping(value = "/json/search/differential_results",
                produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getDifferentialJsonResults(
            @RequestParam(value = "geneQuery", required = false, defaultValue = "") SemanticQuery geneQuery,
            @RequestParam(value = "conditionQuery", required = false, defaultValue = "") SemanticQuery conditionQuery,
            @RequestParam(value = "species", required = false, defaultValue = "") String species) {

        if (isBlank(species)) {
            return GSON.toJson(differentialAnalyticsSearchService.fetchResults(geneQuery, conditionQuery));
        }

        return GSON.toJson(
                differentialAnalyticsSearchService.fetchResults(
                        geneQuery, conditionQuery, speciesFactory.create(species)));
    }
}
