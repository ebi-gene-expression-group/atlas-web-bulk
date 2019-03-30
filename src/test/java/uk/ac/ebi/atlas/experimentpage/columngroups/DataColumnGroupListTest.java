//package uk.ac.ebi.atlas.experimentpage.columngroups;
//
//import com.google.common.collect.ImmutableList;
//import com.google.gson.JsonArray;
//import org.junit.Test;
//import uk.ac.ebi.atlas.model.experiment.ExperimentDesign;
//import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.equalToIgnoringCase;
//import static org.hamcrest.Matchers.greaterThan;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.lessThan;
//import static org.junit.jupiter.api.Assertions.*;
//import static uk.ac.ebi.atlas.testutils.MockExperiment.createDifferentialExperiment;
//import static uk.ac.ebi.atlas.utils.GsonProvider.GSON;
//
//class DataColumnGroupListTest {
//    @Test
//    public void groupingsForHeatmapIncludeComparisonName() {
//        DifferentialExperiment experiment =
//                createDifferentialExperiment(
//                        "accession",
//                        ImmutableList.of(c1),
//                        ExperimentDesignTest.mockExperimentDesign(ImmutableList.of(referenceAssay1, testAssay1)));
//        JsonArray result = experiment.groupingsForHeatmap();
//
//        assertThat(result.size(), greaterThan(0));
//        assertThat(result.get(0).getAsJsonObject().get("name").getAsString(), equalToIgnoringCase("comparison_name"));
//    }
//
//    @Test
//    public void sampleCharacteristicsThatArePerWholeContrastShowUpAsTestThenReference() {
//        ExperimentDesign experimentDesign = new ExperimentDesign();
//
//        experimentDesign.putFactor(referenceAssay1.getFirstAssayAccession(), "infect", "totally_normal");
//        experimentDesign.putFactor(testAssay1.getFirstAssayAccession(), "infect", "very_disease");
//
//        DifferentialExperiment experiment =
//                createDifferentialExperiment("accession", ImmutableList.of(c1), experimentDesign);
//        JsonArray result = experiment.groupingsForHeatmap();
//
//        assertThat(result.size(), is(2));
//
//        String stringDump = GSON.toJson(result);
//
//        assertThat(stringDump.indexOf("very_disease"), lessThan(stringDump.indexOf("totally_normal")));
//    }
//
//}