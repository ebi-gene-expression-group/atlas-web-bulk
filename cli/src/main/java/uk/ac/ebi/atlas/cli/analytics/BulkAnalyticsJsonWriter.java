package uk.ac.ebi.atlas.cli.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.cli.utils.SolrInputDocumentMapper;
import uk.ac.ebi.atlas.experimentimport.analyticsindex.BioentityPropertiesDao;
import uk.ac.ebi.atlas.experimentimport.analyticsindex.ExperimentDataPointStreamFactory;
import uk.ac.ebi.atlas.experimentimport.analyticsindex.stream.SolrInputDocumentInputStream;
import uk.ac.ebi.atlas.solr.bioentities.BioentityPropertyName;
import uk.ac.ebi.atlas.trader.ExperimentTrader;
import uk.ac.ebi.atlas.utils.BioentityIdentifiersReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 *
 */
@Service
public class BulkAnalyticsJsonWriter {
    private static final Logger LOGGER = Logger.getLogger(BulkAnalyticsJsonWriter.class.getName());
    private static final ObjectWriter OBJECT_WRITER =
            new ObjectMapper().setSerializationInclusion(NON_NULL).writer().withRootValueSeparator("\n");

    private final BioentityPropertiesDao bioentityPropertiesDao;
    private final BioentityIdentifiersReader bioentityIdentifiersReader;
    private final ExperimentTrader experimentTrader;
    private final ExperimentDataPointStreamFactory experimentDataPointStreamFactory;

    public BulkAnalyticsJsonWriter(BioentityPropertiesDao bioentityPropertiesDao,
                                   BioentityIdentifiersReader bioentityIdentifiersReader,
                                   ExperimentTrader experimentTrader,
                                   ExperimentDataPointStreamFactory experimentDataPointStreamFactory) {
        this.bioentityPropertiesDao = bioentityPropertiesDao;
        this.bioentityIdentifiersReader = bioentityIdentifiersReader;
        this.experimentTrader = experimentTrader;
        this.experimentDataPointStreamFactory = experimentDataPointStreamFactory;
    }

    /**
     * Serialises one or more experiments to bulk-analytics collection JSONL files using the bioentities collection to
     * retrieve the gene annotations. The bioentity identifiers of all experiments specified will be first collected
     * and a map of bioentity ID to bioentity properties will be built before the serialisation starts.
     *
     * @param experimentAccessions  One or more experiment accessions to serialise to bulk-analytics JSONL files
     * @param outputDir             Path of output directory where the file will be written to
     */
    public void writeJsonLFiles(ImmutableCollection<String> experimentAccessions, String outputDir) {
        var bioentityIdentifiers =
                experimentAccessions.stream()
                        .flatMap(
                                experimentAccession ->
                                        bioentityIdentifiersReader
                                                .getBioentityIdsFromExperiment(experimentAccession).stream())
                        .collect(toImmutableSet());
        var bioentityIdToProperties = bioentityPropertiesDao.getMap(bioentityIdentifiers);

        experimentAccessions.forEach(
                experimentAccession ->
                        writeBulkAnalyticsFile(experimentAccession, outputDir, bioentityIdToProperties));
    }

    /**
     * Serialises one or more experiments to bulk-analytics collection JSONL files using a binary, serialised map of
     * bioentity to bioentity properties. Note that if a bioentity identifier isn’t found in the map no exception will
     * be thrown and the identifier_search and keywords won’t be included in that document.
     *
     * @param experimentAccessions  One or more experiment accessions to serialise to bulk-analytics JSONL files
     * @param outputDir             Path of output directory where the file will be written to
     * @param inputFile             Path to file that contains the serialised map of bioentity to bioentity properties
     *                              file to use (@see BioentityPropertiesMapCommand)
     */
    public void writeJsonLFiles(ImmutableCollection<String> experimentAccessions, String outputDir, String inputFile) {
        LOGGER.info("Reading binary map file " + inputFile + "...");
        try (var objectInputStream = new ObjectInputStream(new FileInputStream(inputFile))) {
            var bioentityIdToProperties =
                    (ImmutableMap<String, Map<BioentityPropertyName, Set<String>>>) objectInputStream.readObject();
            LOGGER.info("Map read: " + bioentityIdToProperties.size() + " entries found");

            experimentAccessions.forEach(
                    experimentAccession ->
                            writeBulkAnalyticsFile(experimentAccession, outputDir, bioentityIdToProperties));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method that writes a single bulk-analytics JSONL file.
     *
     * @param experimentAccession       Accession of the experiment whose analytics file will be serialised
     * @param outputDir                 Path of output directory where the file will be written to
     * @param bioentityIdToProperties   Map of bioentity ID to bioentity properties map for the identifier_search field
     */
    private void writeBulkAnalyticsFile(String experimentAccession,
                                        String outputDir,
                                        Map<String, Map<BioentityPropertyName, Set<String>>> bioentityIdToProperties) {
        var outputFilename = experimentAccession + ".jsonl";
        var targetFilePath = Paths.get(outputDir).resolve(outputFilename);

        LOGGER.info("Building Solr document stream for " + experimentAccession);
        try (
                var solrInputDocumentInputStream =
                        new SolrInputDocumentInputStream(
                                experimentDataPointStreamFactory.stream(
                                        experimentTrader.getExperimentForAnalyticsIndex(experimentAccession)),
                                bioentityIdToProperties);
                var seq = OBJECT_WRITER.writeValues(targetFilePath.toFile())) {
            var nextDoc = solrInputDocumentInputStream.readNext();

            LOGGER.info("Writing records of " + experimentAccession + " to JSON file...");
            while (nextDoc != null) {
                seq.write(SolrInputDocumentMapper.transformToMap(nextDoc));
                nextDoc = solrInputDocumentInputStream.readNext();
            }
            LOGGER.info(experimentAccession + " finished");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
