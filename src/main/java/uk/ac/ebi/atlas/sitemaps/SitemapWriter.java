package uk.ac.ebi.atlas.sitemaps;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

class SitemapWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapWriter.class);
    // Google imposes a limit of 50k entries in a sitemap file:
    // https://support.google.com/webmasters/answer/183668?hl=en
    static final int SITEMAP_URL_MAX_COUNT = 50000;

    SitemapWriter() {
        throw new UnsupportedOperationException();
    }

    static void writeSitemapIndex(OutputStream outputStream,
                                  Collection<String> ensemblSpecies) throws XMLStreamException {
        writeDocument(
                outputStream,
                Stream.concat(
                        ensemblSpecies.stream()
                                .map(_ensemblSpecies ->
                                        ServletUriComponentsBuilder.fromCurrentContextPath()
                                                .path("/species/{speciesEnsemblName}/sitemap.xml")
                                                .buildAndExpand(_ensemblSpecies)),
                        Stream.of(ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/experiments/sitemap.xml")
                                .build()))
                        .map(UriComponents::encode)
                        .map(UriComponents::toUriString),
                "sitemapindex",
                "sitemap",
                ImmutableMap.of());
    }

    static void writeGenes(OutputStream outputStream, Collection<String> endpoints, Collection<String> genes)
            throws XMLStreamException {
        var urls =
                Stream.concat(
                        endpoints.stream()
                                .map(endpoint ->
                                        ServletUriComponentsBuilder.fromCurrentContextPath()
                                                .path("{endpoint}")
                                                .buildAndExpand(endpoint)),
                        genes.stream()
                                .map(gene -> ServletUriComponentsBuilder.fromCurrentContextPath()
                                        .path("/genes/{gene}")
                                        .buildAndExpand(gene)))
                        .map(UriComponents::encode)
                        .map(UriComponents::toUriString);

        writeDocument(
                outputStream,
                allEntries ? urls : urls.limit(SITEMAP_URL_MAX_COUNT),
                "urlset",
                "url",
                ImmutableMap.of("changefreq", "monthly"));
    }

    private static void writeDocument(OutputStream outputStream,
                                      Stream<String> urls,
                                      String rootName,
                                      String childName,
                                      Map<String, String> parametersForChildren) throws XMLStreamException {
        var outputFactory = XMLOutputFactory.newInstance();
        var eventFactory = XMLEventFactory.newInstance();
        var writer = outputFactory.createXMLEventWriter(outputStream);

        try {
            writer.add(eventFactory.createStartDocument());
            writer.add(eventFactory.createStartElement("", "", rootName));
            writer.add(eventFactory.createAttribute("xmlns", "http://www.sitemaps.org/schemas/sitemap/0.9"));

            urls.forEachOrdered(url -> writeChild(writer, eventFactory, url, childName, parametersForChildren));

            writer.add(eventFactory.createEndElement("", "", rootName));
            writer.add(eventFactory.createEndDocument());
        } catch (XMLStreamException e) {
            LOGGER.error(e.getMessage());
        } finally {
            writer.close();
        }
    }

    private static void writeChild(XMLEventWriter writer,
                                   XMLEventFactory eventFactory,
                                   String url,
                                   String childName,
                                   Map<String, String> parameters) {
        try {
            writer.add(eventFactory.createStartElement("", "", childName));
            writer.add(eventFactory.createStartElement("", "", "loc"));
            writer.add(eventFactory.createCharacters(url));
            writer.add(eventFactory.createEndElement("", "", "loc"));

            for (var e : parameters.entrySet()) {
                writer.add(eventFactory.createStartElement("", "", e.getKey()));
                writer.add(eventFactory.createCharacters(e.getValue()));
                writer.add(eventFactory.createEndElement("", "", e.getKey()));
            }

            writer.add(eventFactory.createEndElement("", "", childName));
        } catch (XMLStreamException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }
}
