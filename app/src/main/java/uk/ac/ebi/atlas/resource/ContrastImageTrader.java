package uk.ac.ebi.atlas.resource;

import com.google.gson.JsonArray;
import uk.ac.ebi.atlas.model.experiment.sample.Contrast;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayExperiment;
import uk.ac.ebi.atlas.model.resource.ContrastImage;
import uk.ac.ebi.atlas.model.resource.ExternalImage;
import uk.ac.ebi.atlas.model.resource.ResourceType;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Named
public class ContrastImageTrader {
    private ContrastImageFactory contrastImageFactory;

    @Inject
    public ContrastImageTrader(ContrastImageFactory contrastImageFactory) {
        this.contrastImageFactory = contrastImageFactory;
    }

    public Map<String, JsonArray> contrastImages(DifferentialExperiment differentialExperiment) {
        Map<String, JsonArray>  result = new HashMap<>();
        for (Contrast contrast : differentialExperiment.getDataColumnDescriptors()) {
            Optional<String> arrayDesign =
                    differentialExperiment instanceof MicroarrayExperiment ?
                            Optional.of(contrast.getArrayDesignAccession()) :
                            Optional.empty();

            JsonArray resultsForThisContrast = new JsonArray();
            for (ResourceType resourceType : ContrastImage.RESOURCE_TYPES) {
                ExternalImage externalImage =
                        contrastImageFactory.getContrastImage(
                                resourceType,
                                differentialExperiment.getAccession(),
                                arrayDesign,
                                contrast.getId());
                if (externalImage.exists()) {
                    resultsForThisContrast.add(externalImage.toJson());
                }
            }

            result.put(contrast.getId(), resultsForThisContrast);
        }
        return result;
    }
}
