package uk.ac.ebi.atlas.search;

import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.search.suggester.AnalyticsSuggesterService;

import java.util.Map;
import java.util.stream.Stream;

@Service
public class AnalyticsSuggesterServiceImpl implements AnalyticsSuggesterService {

    //To get rid of the errors from bean wiring issues of AnalyticsSuggesterService
    @Override
    public Stream<Map<String, String>> fetchMetadataSuggestions(String query, String... species) {
        return null;
    }
}
