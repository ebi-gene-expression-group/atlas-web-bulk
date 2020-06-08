package uk.ac.ebi.atlas.experimentpage.baseline.coexpression;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoexpressedGenesService {
    private final CoexpressedGenesDao coexpressedGenesDao;

    public CoexpressedGenesService(CoexpressedGenesDao coexpressedGenesDao) {
        this.coexpressedGenesDao = coexpressedGenesDao;
    }

    public List<String> fetchCoexpressions(String experimentAccession, String identifier, int requestedAmount) {
        var coexpressedGenes = coexpressedGenesDao.coexpressedGenesFor(experimentAccession, identifier);
        return coexpressedGenes.subList(0, Math.min(Math.max(0, requestedAmount), coexpressedGenes.size()));
    }
}
