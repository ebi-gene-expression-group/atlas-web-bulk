package uk.ac.ebi.atlas.cli.utils;

import uk.ac.ebi.atlas.cli.experimentDesign.ExperimentDesignCommand;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class FailedAccessionWriter {

    private String path;
    private List<String> accessions;
    private static final Logger LOGGER = Logger.getLogger(FailedAccessionWriter.class.getName());

    public FailedAccessionWriter(String path, List<String> accessions) {
        this.path = path;
        this.accessions = accessions;
    }

    public void write() {
        try {
            FileWriter writer = new FileWriter(this.path);
            for(String accession : accessions) {
                writer.write(accession + System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
            System.exit(2);
        }
    }
}
