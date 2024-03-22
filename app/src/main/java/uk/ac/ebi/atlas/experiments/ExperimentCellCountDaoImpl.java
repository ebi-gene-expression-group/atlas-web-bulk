package uk.ac.ebi.atlas.experiments;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(transactionManager = "txManager", readOnly = true)
public class ExperimentCellCountDaoImpl implements ExperimentCellCountDao {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ExperimentCellCountDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Integer fetchNumberOfCellsByExperimentAccession(String experimentAccession) {
        return 0;
    }
}
