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
import uk.ac.ebi.atlas.testutils.JdbcUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfig.class)
class SitemapWriterIT {
    @Test
    void sitemapWriterIsAUtilityClassAndCannotBeInstantiated() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(SitemapWriter::new);
    }

    @Test
    void testWriteOneGene() throws Exception {
        var baos = new ByteArrayOutputStream();
        SitemapWriter.writeGenes(baos, ImmutableList.of(), ImmutableList.of("ASPM"));

        assertThat(baos.toString(Charset.defaultCharset()))
                .containsPattern("<url><loc>http://.+/genes/ASPM</loc><changefreq>monthly</changefreq></url>");
    }

    @Test
    void producesValidXmlForGenes() throws Exception {
        var baos = new ByteArrayOutputStream();
        SitemapWriter.writeGenes(baos, ImmutableList.of(), ImmutableList.of());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(baos.toInputStream());
        doc.getDocumentElement().normalize();

        assertThat(doc.getDocumentElement()).hasFieldOrPropertyWithValue("tagName", "urlset");
    }

    @Test
    void producesValidXmlForSpecies() throws Exception {
        var baos = new ByteArrayOutputStream();
        SitemapWriter.writeSitemapIndex(baos, ImmutableList.of());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(baos.toInputStream());
        doc.getDocumentElement().normalize();

        assertThat(doc.getDocumentElement()).hasFieldOrPropertyWithValue("tagName", "sitemapindex");
    }
}
