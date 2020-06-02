package uk.ac.ebi.atlas.experiments;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ExperimentCollection implements ExperimentCollectionsRepository {

    @Override
    public List<String> getExperimentCollections(String accession) {
        return List.of(); //ATM this class does nothing as collection table is not implemented in db as yet
    }
}
