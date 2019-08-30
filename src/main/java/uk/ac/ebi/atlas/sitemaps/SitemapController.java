package uk.ac.ebi.atlas.sitemaps;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;

import static uk.ac.ebi.atlas.sitemaps.SitemapWriter.writeExperimentsSitemap;
import static uk.ac.ebi.atlas.sitemaps.SitemapWriter.writeBioentityIdentifiersSitemap;
import static uk.ac.ebi.atlas.sitemaps.SitemapWriter.writeSitemapIndex;

@Controller
public class SitemapController {
    private final SitemapDao sitemapDao;

    public SitemapController(SitemapDao sitemapDao) {
        this.sitemapDao = sitemapDao;
    }

    @GetMapping(value = "/sitemap.xml")
    public void mainSitemap(HttpServletResponse response) throws IOException, XMLStreamException {
        response.setContentType(MediaType.TEXT_XML_VALUE);
        writeSitemapIndex(response.getOutputStream(), sitemapDao.getSpeciesInPublicExperiments());
    }

    @GetMapping(value = "/experiments/sitemap.xml")
    public void experimentsSitemap(HttpServletResponse response) throws IOException, XMLStreamException {
        response.setContentType(MediaType.TEXT_XML_VALUE);
        writeExperimentsSitemap(response.getOutputStream());
    }

    @GetMapping(value = "/species/{species}/sitemap.xml")
    public void speciesGenesSitemap(@PathVariable String species,
                                    @RequestParam(defaultValue = "false", required = false) boolean allEntries,
                                    HttpServletResponse response) throws IOException, XMLStreamException {
        response.setContentType(MediaType.TEXT_XML_VALUE);
        writeBioentityIdentifiersSitemap(
                response.getOutputStream(),
                sitemapDao.getBioentityIdentifiersInPublicExperiments(species),
                allEntries);
    }
}
