package uk.ac.ebi.atlas.sitemaps;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.w3c.dom.Document;
import uk.ac.ebi.atlas.configuration.TestConfig;
import uk.ac.ebi.atlas.species.SpeciesProperties;
import uk.ac.ebi.atlas.species.SpeciesPropertiesTrader;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static uk.ac.ebi.atlas.sitemaps.SitemapWriter.SITEMAP_URL_MAX_COUNT;
import static uk.ac.ebi.atlas.testutils.RandomDataTestUtils.generateRandomEnsemblGeneId;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
class SitemapWriterIT {
    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();

    @Inject
    SpeciesPropertiesTrader speciesPropertiesTrader;

    @Test
    void sitemapWriterIsAUtilityClassAndCannotBeInstantiated() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(SitemapWriter::new);
    }

//    @Test
//    void testWriteOneGene() throws Exception {
//        var baos = new ByteArrayOutputStream();
//        SitemapWriter.writeBioentityIdentifiersSitemap(baos, ImmutableList.of("ASPM"), true);
//
//        assertThat(baos.toString(Charset.defaultCharset()))
//                .containsPattern("<url><loc>http://.+/genes/ASPM</loc><changefreq>monthly</changefreq></url>");
//    }

    @Test
    void producesValidXmlForMainSitemapOfSitemaps() throws Exception {
        var randomSubsetOfSpecies =
                speciesPropertiesTrader.getAll().stream()
                        .filter(__ -> RNG.nextBoolean())
                        .map(SpeciesProperties::referenceName)
                        .collect(toImmutableSet());

        var baos = new ByteArrayOutputStream();
        SitemapWriter.writeSitemapIndex(baos, randomSubsetOfSpecies);

        assertThat(parseXml(baos).getDocumentElement()).hasFieldOrPropertyWithValue("tagName", "sitemapindex");
    }

    @Test
    void producesValidXmlForExperiments() throws Exception {
        var baos = new ByteArrayOutputStream();
        SitemapWriter.writeExperimentsSitemap(baos);

        assertThat(parseXml(baos).getDocumentElement()).hasFieldOrPropertyWithValue("tagName", "urlset");
    }

    @Test
    void sitemapIsLimitedTo50KEntriesUnlessToldOtherwise() throws Exception {
        var bioentityIdentifiers = IntStream.range(0, SITEMAP_URL_MAX_COUNT + RNG.nextInt(1, 100000)).boxed()
                .map(__ -> generateRandomEnsemblGeneId())
                .collect(toImmutableList());

        var baos = new ByteArrayOutputStream();
        var baosForAllEntries = new ByteArrayOutputStream();
        SitemapWriter.writeBioentityIdentifiersSitemap(baos, bioentityIdentifiers, false);
        SitemapWriter.writeBioentityIdentifiersSitemap(baosForAllEntries, bioentityIdentifiers, true);

        assertThat(parseXml(baos).getElementsByTagName("url").getLength())
                .isEqualTo(SITEMAP_URL_MAX_COUNT)
                .isLessThan(parseXml(baosForAllEntries).getElementsByTagName("url").getLength());
    }

    @Test
    void producesValidXmlForBioentityIdentifiers() throws Exception {
        var baos = new ByteArrayOutputStream();
        SitemapWriter.writeBioentityIdentifiersSitemap(baos, ImmutableList.of(), true);

        assertThat(parseXml(baos).getDocumentElement()).hasFieldOrPropertyWithValue("tagName", "urlset");
    }

    private Document parseXml(final ByteArrayOutputStream outputStream) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(outputStream.toInputStream());
        doc.getDocumentElement().normalize();

        return doc;
    }
}
