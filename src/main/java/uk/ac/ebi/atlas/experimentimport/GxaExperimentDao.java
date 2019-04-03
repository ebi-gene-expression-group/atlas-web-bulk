package uk.ac.ebi.atlas.experimentimport;

import com.google.common.collect.Sets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GxaExperimentDao extends ExperimentDao {
    // Create
    private static final String INSERT_NEW_EXPERIMENT =
            "INSERT INTO experiment " +
            "(accession, type, species, private, access_key, pubmed_ids) VALUES (?, ?, ?, ?, ?, ?)";
    // Read
    private static final String SELECT_EXPERIMENT_AS_ADMIN_BY_ACCESSION =
            "SELECT * FROM experiment " +
            "WHERE accession=?";
    private static final String SELECT_EXPERIMENT_BY_ACCESSION_AND_ACCESS_KEY =
            "SELECT * FROM experiment " +
            "WHERE accession=? AND (private=FALSE OR access_key=?)";
    private static final String SELECT_PUBLIC_EXPERIMENTS_BY_EXPERIMENT_TYPE =
            "SELECT accession FROM public_experiment WHERE type IN(:experimentTypes)";
    private static final String SELECT_ALL_EXPERIMENTS_AS_ADMIN =
            "SELECT * FROM experiment";
    private static final String COUNT_EXPERIMENTS = "SELECT COUNT(*) FROM experiment";
    // Update
    private static final String UPDATE_EXPERIMENT = "UPDATE experiment SET private=? where accession=?";
    // Delete
    private static final String DELETE_EXPERIMENT = "DELETE FROM experiment WHERE accession=?";


    public GxaExperimentDao(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(jdbcTemplate, namedParameterJdbcTemplate);
    }

    // Will fail when trying to add experiment already present in database
    @Override
    public void addExperiment(ExperimentDto experimentDto, UUID accessKeyUuid) {
        // Add experiment row
        jdbcTemplate.update(
                INSERT_NEW_EXPERIMENT,
                experimentDto.getExperimentAccession(),
                experimentDto.getExperimentType().name(),
                experimentDto.getSpecies(),
                experimentDto.isPrivate(),
                accessKeyUuid.toString(),
                experimentDto.getPubmedIds().stream().collect(Collectors.joining(", ")));
    }

    @Override
    public HashSet<String> findPublicExperimentAccessions(ExperimentType... experimentTypes) {
        Set<String> experimentTypeNames =
                Arrays.stream(experimentTypes)
                        .map(ExperimentType::name)
                        .collect(Collectors.toSet());

        return Sets.newHashSet(
                namedParameterJdbcTemplate.queryForList(
                        SELECT_PUBLIC_EXPERIMENTS_BY_EXPERIMENT_TYPE,
                        new MapSqlParameterSource("experimentTypes", experimentTypeNames),
                        String.class));
    }

    @Override
    public ExperimentDto findExperiment(String experimentAccession, String accessKey) {
        return getSingleExperiment(
                jdbcTemplate.query(
                        SELECT_EXPERIMENT_BY_ACCESSION_AND_ACCESS_KEY,
                        new GxaExperimentDtoResultSetExtractor(),
                        experimentAccession,
                        accessKey),
                experimentAccession);
    }

    @Override
    public ExperimentDto getExperimentAsAdmin(String experimentAccession) {
        return getSingleExperiment(
                jdbcTemplate.query(
                        SELECT_EXPERIMENT_AS_ADMIN_BY_ACCESSION,
                        new GxaExperimentDtoResultSetExtractor(),
                        experimentAccession),
                experimentAccession);
    }

    @Override
    public List<ExperimentDto> getAllExperimentsAsAdmin() {
        return jdbcTemplate.query(SELECT_ALL_EXPERIMENTS_AS_ADMIN, new GxaExperimentDtoResultSetExtractor());
    }

    @Override
    public int countExperiments() {
        return jdbcTemplate.queryForObject(COUNT_EXPERIMENTS, Integer.class);
    }

    @Override
    public void setExperimentPrivacyStatus(String experimentAccession, boolean isPrivate) {
        int recordsCount = jdbcTemplate.update(UPDATE_EXPERIMENT, isPrivate, experimentAccession);
        checkExperimentFound(recordsCount == 1, experimentAccession);
    }

    @Override
    public void deleteExperiment(String experimentAccession) {
        int deletedRecordsCount = jdbcTemplate.update(DELETE_EXPERIMENT, experimentAccession);
        checkExperimentFound(deletedRecordsCount == 1, experimentAccession);
    }

    private ExperimentDto getSingleExperiment(List<ExperimentDto> experimentDtos, String accession) {
        checkExperimentFound(experimentDtos.size() == 1, accession);
        return experimentDtos.get(0);
    }
}
