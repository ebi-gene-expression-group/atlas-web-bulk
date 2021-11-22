package uk.ac.ebi.atlas.cli.bioentities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.cli.utils.SolrInputDocumentMapper;
import uk.ac.ebi.atlas.model.resource.BioentityPropertyFile;
import uk.ac.ebi.atlas.solr.bioentities.BioentityPropertiesSource;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.io.MoreFiles.getNameWithoutExtension;

/**
 * Class that serialises bioentity property files to JSONL files.
 */
@Service
public class BioentitiesJsonWriter {
    private static final Logger LOGGER = Logger.getLogger(BioentitiesJsonWriter.class.getName());

    private static final ObjectWriter OBJECT_WRITER =
            new ObjectMapper().setSerializationInclusion(NON_NULL).writer().withRootValueSeparator("\n");
    // Instead of having an implementation for the class BioentityProperty we’d rather get instances of
    // SolrInputDocument to take advantage of the @Field annotations in BioentityProperty and in that way we can have
    // some “code symmetry” with bulk-analytics file serialisation. We’reusing DocumentObjectBinder in the way it’s
    // in SolrClient::addBeans. If we change Solr/SolrJ version we might need to change this.
    private static final DocumentObjectBinder DOCUMENT_OBJECT_BINDER = new DocumentObjectBinder();

    private final BioentityPropertiesSource bioentityPropertiesSource;

    public BioentitiesJsonWriter(BioentityPropertiesSource bioentityPropertiesSource) {
        this.bioentityPropertiesSource = bioentityPropertiesSource;
    }

    /**
     * Write JSONL files of all Ensembl annotation files.
     *
     * @param outputDir path of the output directory where the files will be written to
     */
    public void writeAnnotationFiles(String outputDir) {
        bioentityPropertiesSource.getAnnotationFiles()
                .forEach(annotationFile -> writePropertyFile(outputDir, annotationFile));
    }

    /**
     * Write JSONL files of all array design mapping files.
     *
     * @param outputDir path of the output directory where the files will be written to
     */
    public void writeArrayDesignMappingFiles(String outputDir) {
        bioentityPropertiesSource.getArrayDesignMappingFiles()
                .forEach(annotationFile -> writePropertyFile(outputDir, annotationFile));
    }

    /**
     * Write JSONL files of all Reactome stable IDs files.
     *
     * @param outputDir path of the output directory where the files will be written to
     */
    public void writeReactomePropertyFiles(String outputDir) {
        bioentityPropertiesSource.getReactomePropertyFiles()
                .forEach(annotationFile -> writePropertyFile(outputDir, annotationFile));
    }

    /**
     * Helper method that writes a single bioentity property file (namely, an Ensembl/WBPS, an array design mapping
     * file or a Reactome property file) to a specified output directory. The name of the resulting file is the
     * basename of the original file with the “jsonl” extension.
     *
     * @param outputDir             path of the output directory where the file will be written to
     * @param bioentityPropertyFile bioentity propert file, as an Expression Atlas <code>BioentityPropertyFile</code>
     *                              instance
     */
    private void writePropertyFile(String outputDir, BioentityPropertyFile bioentityPropertyFile) {
        var outputFilename = getNameWithoutExtension(bioentityPropertyFile.getPath()) + ".jsonl";
        var targetFilePath = Paths.get(outputDir).resolve(outputFilename);

        LOGGER.info(bioentityPropertyFile.getPath().getFileName() + " -> " + targetFilePath.getFileName());
        try (var seq = OBJECT_WRITER.writeValues(targetFilePath.toFile())) {
            seq.writeAll(
                    bioentityPropertyFile.get()
                            // There are missing IDs in homo_sapiens.A-GEOD-13158.tsv which may be a data bug though
                            .filter(bioentityProperty -> !isNullOrEmpty(bioentityProperty.getBioentityIdentifier()))
                            .map(DOCUMENT_OBJECT_BINDER::toSolrInputDocument)
                            .map(SolrInputDocumentMapper::transformToMap)
                            .collect(toImmutableList()));
        } catch (IOException e) {
            LOGGER.severe(e.toString());
        }
    }
}
