package uk.ac.ebi.atlas.sitemaps;

import com.google.common.collect.ImmutableList;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.ac.ebi.atlas.solr.analytics.AnalyticsSearchService;
import uk.ac.ebi.atlas.species.SpeciesFactory;
import uk.ac.ebi.atlas.species.SpeciesPropertiesTrader;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;

import static uk.ac.ebi.atlas.sitemaps.SitemapWriter.writeGenes;
import static uk.ac.ebi.atlas.sitemaps.SitemapWriter.writeSitemapIndex;

@Controller
public class SitemapController {
    private final AnalyticsSearchService solr;
    private final SpeciesFactory speciesFactory;
    private final SpeciesPropertiesTrader speciesPropertiesTrader;

    public SitemapController(AnalyticsSearchService solr,
                             SpeciesFactory speciesFactory,
                             SpeciesPropertiesTrader speciesPropertiesTrader) {
        this.solr = solr;
        this.speciesFactory = speciesFactory;
        this.speciesPropertiesTrader = speciesPropertiesTrader;
    }

    @GetMapping(value = "/sitemap.xml")
    public void mainSitemap(HttpServletResponse response) throws IOException, XMLStreamException {
        response.setContentType(MediaType.TEXT_XML_VALUE);
        writeSitemapIndex(response.getOutputStream(), speciesPropertiesTrader.getAll());
    }

    @GetMapping(value = "/species/{species}/sitemap.xml")
    public void sitemapForSpecies(@PathVariable String species,
                                  HttpServletResponse response) throws IOException, XMLStreamException {
        response.setContentType(MediaType.TEXT_XML_VALUE);
        writeGenes(
                response.getOutputStream(),
                ImmutableList.of("/experiments", "/plant/experiments"),
                solr.getBioentityIdentifiersForSpecies(speciesFactory.create(species).getReferenceName()));
    }

}
