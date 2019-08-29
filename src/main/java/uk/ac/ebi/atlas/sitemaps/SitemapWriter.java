package uk.ac.ebi.atlas.sitemaps;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import uk.ac.ebi.atlas.species.SpeciesProperties;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

class SitemapWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapWriter.class);

    public SitemapWriter() {
        throw new UnsupportedOperationException();
    }

    static void writeSitemapIndex(OutputStream outputStream,
                                  Collection<SpeciesProperties> speciesProperties) throws XMLStreamException {
        writeDocument(
                outputStream,
                speciesProperties.stream()
                        .map(_speciesProperties ->
                            ServletUriComponentsBuilder.fromCurrentContextPath()
                                    .path("/species/{speciesEnsemblName}/sitemap.xml")
                                    .buildAndExpand(_speciesProperties.ensemblName()))
                        .map(UriComponents::encode)
                        .map(UriComponents::toUriString)
                        .collect(toImmutableList()),
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
                        .map(UriComponents::toUriString)
                        .collect(toImmutableList());

        writeDocument(outputStream, urls, "urlset", "url", ImmutableMap.of("changefreq", "monthly"));
    }

    private static void writeDocument(OutputStream outputStream,
                                      Collection<String> urls,
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

            for (String url : urls) {
                writeChild(writer, eventFactory, url, childName, parametersForChildren);
            }

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
                                   Map<String, String> parameters) throws XMLStreamException {
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
    }
}
