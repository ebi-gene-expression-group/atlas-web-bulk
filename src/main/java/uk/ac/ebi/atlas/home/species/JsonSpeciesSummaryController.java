package uk.ac.ebi.atlas.home.species;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.atlas.controllers.JsonExceptionHandlingController;

@RestController
public class JsonSpeciesSummaryController extends JsonExceptionHandlingController {
    private final SpeciesSummaryService speciesSummaryService;
    private final SpeciesSummarySerializer speciesSummarySerializer;

    public JsonSpeciesSummaryController(SpeciesSummaryService speciesSummaryService,
                                        SpeciesSummarySerializer speciesSummarySerializer) {
        this.speciesSummaryService = speciesSummaryService;
        this.speciesSummarySerializer = speciesSummarySerializer;
    }

    @Cacheable("speciesSummary")
    @GetMapping(value = "/json/species-summary",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getSpeciesSummaryGroupedByKingdom() {
        return speciesSummarySerializer.serialize(
                speciesSummaryService.getSpeciesSummariesGroupedByKingdom());
    }
}
