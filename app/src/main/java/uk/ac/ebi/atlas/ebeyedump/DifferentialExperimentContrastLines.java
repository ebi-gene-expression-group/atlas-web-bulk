package uk.ac.ebi.atlas.ebeyedump;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.atlas.model.OntologyTerm;
import uk.ac.ebi.atlas.model.experiment.sdrf.FactorSet;
import uk.ac.ebi.atlas.model.experiment.sdrf.SampleCharacteristic;
import uk.ac.ebi.atlas.model.experiment.sdrf.Factor;
import uk.ac.ebi.atlas.model.experiment.sample.Contrast;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public class DifferentialExperimentContrastLines implements Iterable<String[]> {
    private final LinkedHashSet<ImmutableList<String>> contrastDetails;
    private final LinkedHashSet<ImmutableList<String>> result = new LinkedHashSet<>();

    public DifferentialExperimentContrastLines(DifferentialExperiment experiment) {
        this.contrastDetails = buildContrastDetails(experiment);
    }

    private LinkedHashSet<ImmutableList<String>> buildContrastDetails(DifferentialExperiment experiment) {
        for (Contrast contrast : experiment.getDataColumnDescriptors()) {
            for (String assayAccession : contrast.getReferenceAssayGroup().getAssayIds()) {
                this.populateSamples(experiment, assayAccession, contrast, "reference");
                this.populateFactors(experiment, assayAccession, contrast, "reference");
            }

            for (String assayAccession : contrast.getTestAssayGroup().getAssayIds()) {
                this.populateSamples(experiment, assayAccession, contrast, "test");
                this.populateFactors(experiment, assayAccession, contrast, "test");
            }
        }

        return result;
    }

    private void populateSamples(DifferentialExperiment experiment,
                                 String assayAccession,
                                 Contrast contrast,
                                 String value) {
        for (SampleCharacteristic sample : experiment.getExperimentDesign().getSampleCharacteristics(assayAccession)) {
            ImmutableList<String> line =
                    ImmutableList.of(
                            experiment.getAccession(),
                            contrast.getId(),
                            value,
                            "characteristic",
                            sample.getHeader(),
                            sample.getValue(),
                            joinURIs(sample.getValueOntologyTerms()));
            result.add(line);
        }
    }

    private void populateFactors(DifferentialExperiment experiment,
                                 String assayAccession,
                                 Contrast contrast,
                                 String value) {
        FactorSet factorSet = experiment.getExperimentDesign().getFactors(assayAccession);
        if (factorSet != null) {
            for (Factor factor : factorSet) {
                ImmutableList<String> line =
                        ImmutableList.of(
                                experiment.getAccession(),
                                contrast.getId(),
                                value,
                                "factor",
                                factor.getHeader(),
                                factor.getValue(),
                                joinURIs(factor.getValueOntologyTerms()));
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
        return contrastDetails.stream().map(list -> list.toArray(new String[0])).iterator();
    }
}
