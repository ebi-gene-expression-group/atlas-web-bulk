package uk.ac.ebi.atlas.controllers.page;

import com.google.common.collect.TreeMultimap;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExperimentsUtil {

    public static LinkedHashMap<String, LinkedHashMap<String, String>> getBaselineExperiments(
        TreeMultimap<String, String> experimentAccessionsBySpecies,
        Map<String, String> experimentDisplayNames
    ) {
        var baselineExperimentsData = new LinkedHashMap<String, LinkedHashMap<String, String>>();

        experimentAccessionsBySpecies.asMap().forEach((species, accessions) -> {
            var nameByAccession = new LinkedHashMap<String, String>();
            accessions.forEach(accession ->
                nameByAccession.put(accession, experimentDisplayNames.get(accession)));
            baselineExperimentsData.put(species, nameByAccession);
        });

        return baselineExperimentsData;
    }
}
