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

    @GetMapping(value = "/json/species-summary",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getSpeciesSummaryGroupedByKingdom() {
        String json = speciesSummarySerializer.serialize(
                speciesSummaryService.getReferenceSpeciesSummariesGroupedByKingdom());
        // #region agent log
        try {
            int urlKey = json.indexOf("\"url\":\"");
            String sample = urlKey < 0 ? null : json.substring(urlKey, Math.min(json.length(), urlKey + 180));
            uk.ac.ebi.atlas.utils.UrlHelpers.agentLogFromController(
                    "E",
                    "JsonSpeciesSummaryController.java:getSpeciesSummaryGroupedByKingdom",
                    "species-summary json sample",
                    "{\"sample\":" + (sample == null ? "null" : "\"" + sample.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                            + ",\"jsonLen\":" + json.length() + "}");
        } catch (Exception ignored) { }
        // #endregion
        return json;
    }
}
