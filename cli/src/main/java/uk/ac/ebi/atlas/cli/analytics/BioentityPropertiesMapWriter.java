package uk.ac.ebi.atlas.cli.analytics;

import com.google.common.collect.ImmutableCollection;
import org.springframework.stereotype.Service;
import uk.ac.ebi.atlas.cli.experimentDesign.ExperimentDesignCommand;
import uk.ac.ebi.atlas.cli.utils.SpeciesBioentityFinder;
import uk.ac.ebi.atlas.experimentimport.analyticsindex.BioentityPropertiesDao;
import uk.ac.ebi.atlas.utils.BioentityIdentifiersReader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Serialise a bioentity-to-bioentity properties map to a specified file.
 *
 * The bioentity (i.e. gene) IDs can be sourced from a set of experiments via their accessions or from a single
 * species. In the former case the experiment expression matrices will be parsed to collect all available gene IDs and
 * in the latter case the <code>bioentities</code> collection in Solr will be queried to retrieve all identifiers that
 * match the given species.
 *
 * Once the identifiers are retrieved from either source, properties will be read from the <code>bioentities</code>
 * collection. Special care should be taken not to modify or operate Solr during the write operation to avoid data
 * inconsistencies or network I/O errors.
 */
@Service
public class BioentityPropertiesMapWriter {
    private final SpeciesBioentityFinder speciesBioentityFinder;
    private final BioentityPropertiesDao bioentityPropertiesDao;
    private final BioentityIdentifiersReader bioentityIdentifiersReader;
    private List<String> failedAccessions;
    private static final Logger LOGGER = Logger.getLogger(BioentityPropertiesMapWriter.class.getName());

    public BioentityPropertiesMapWriter(SpeciesBioentityFinder speciesBioentityFinder,
                                        BioentityPropertiesDao bioentityPropertiesDao,
                                        BioentityIdentifiersReader bioentityIdentifiersReader) {
        this.speciesBioentityFinder = speciesBioentityFinder;
        this.bioentityPropertiesDao = bioentityPropertiesDao;
        this.bioentityIdentifiersReader = bioentityIdentifiersReader;
        this.failedAccessions = new ArrayList<>();
    }

    /**
     * Write a map of bioentity-to-bioentity properties map sourced from expression matrices.
     *
     * @param experimentAccessions  set of experiment accessions that will be used to parse the gene IDs from their
     *                              expression matrices files
     * @param outputFile            destination file where the map will be written to
     */
    public void writeMap(ImmutableCollection<String> experimentAccessions, String outputFile) {
        try (var objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            var bioentityIdentifiers = new HashSet<String>();
            for(String accession : experimentAccessions) {
                try {
                    bioentityIdentifiers.addAll(bioentityIdentifiersReader.getBioentityIdsFromExperiment(accession, true));
                } catch (RuntimeException e) {
                    failedAccessions.add(accession);
                    LOGGER.severe("Failed to add bioentity mappings for "+accession+": "+e.getMessage());
                }
            }
            objectOutputStream.writeObject(bioentityPropertiesDao.getMap(bioentityIdentifiers));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Write a map of bioentity-to-bioentity properties map source from a single species.
     * <p>
     * The bioentities are sourced from the bioentities collection in Solr. If you have want to know the available
     * species query the collection with <code>/select?facet.field=species&facet.limit=-1&facet=on&q=*:*&rows=0</code>.
     * @param species       species that will be used as reference; the value is case-insensitive but spaces should
     *                      be replaced by underscores (e.g. “Homo sapiens” should be passed as “Homo_sapiens”)
     * @param outputFile    destination file where the map will be written to
     */
    public void writeMap(String species, String outputFile) {
        try (var objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            objectOutputStream.writeObject(
                    bioentityPropertiesDao.getMap(speciesBioentityFinder.findBioentityIdentifiers(species)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<String> getFailedAccessions() {
        return failedAccessions;
    }
}
