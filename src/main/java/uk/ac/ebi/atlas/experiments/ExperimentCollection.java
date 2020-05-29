package uk.ac.ebi.atlas.experiments;

import java.util.List;

public class ExperimentCollection implements ExperimentCollectionsRepository {

    @Override
    public List<String> getExperimentCollections(String accession) {
        return List.of(); //ATM this class does nothing as collection table is not implemented in db as yet
    }
}
