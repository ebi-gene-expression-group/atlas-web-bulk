package uk.ac.ebi.atlas.ebeyedump;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.OntologyTerm;
import uk.ac.ebi.atlas.model.experiment.sdrf.FactorSet;
import uk.ac.ebi.atlas.model.experiment.sdrf.SampleCharacteristic;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExperiment;
import uk.ac.ebi.atlas.model.experiment.sdrf.Factor;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public class BaselineExperimentAssayGroupsLines implements Iterable<String[]> {
    private final LinkedHashSet<ImmutableList<String>> result = new LinkedHashSet<>();
    private final LinkedHashSet<ImmutableList<String>> assayGroupsDetails;

    public BaselineExperimentAssayGroupsLines(BaselineExperiment experiment) {
        assayGroupsDetails = buildAssayGroupsDetails(experiment);
    }

    private LinkedHashSet<ImmutableList<String>> buildAssayGroupsDetails(BaselineExperiment experiment) {
        for (AssayGroup assayGroupById : experiment.getDataColumnDescriptors()) {
            for (String assayAccession : assayGroupById.getAssayIds()) {
                this.populateSamples(experiment, assayAccession, assayGroupById);
                this.populateFactors(experiment, assayAccession, assayGroupById);
            }
        }

        return result;
    }

    private void populateSamples(BaselineExperiment experiment, String assayAccession, AssayGroup assayGroup) {
        for (SampleCharacteristic sample : experiment.getSampleCharacteristics(assayAccession)) {
            ImmutableList<String> line =
                    ImmutableList.of(
                            experiment.getAccession(),
                            assayGroup.getId(),
                            "characteristic",
                            sample.getHeader(),
                            sample.getValue(),
                            joinURIs(sample.getValueOntologyTerms()));
            result.add(line);
        }
    }

    private void populateFactors(BaselineExperiment experiment, String assayAccession, AssayGroup assayGroup) {
        FactorSet factorSet = experiment.getFactors(assayAccession);
        if (factorSet != null) {
            for (Factor factor : factorSet) {
                ImmutableList<String> line = ImmutableList.of(experiment.getAccession(), assayGroup.getId(), "factor",
                        factor.getHeader(), factor.getValue(), joinURIs(factor.getValueOntologyTerms()));
                result.add(line);
            }
        }
    }

    private static String joinURIs(Set<OntologyTerm> ontologyTerms) {
        return ontologyTerms.stream().map(OntologyTerm::uri).collect(joining(" "));
    }

    @Override
    @NotNull
    public Iterator<String[]> iterator() {
        return assayGroupsDetails.stream().map(list -> list.toArray(new String[0])).iterator();
    }
}
