package uk.ac.ebi.atlas.experimentpage.content;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.atlas.model.experiment.Experiment;
import uk.ac.ebi.atlas.model.experiment.baseline.BaselineExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.sample.Contrast;
import uk.ac.ebi.atlas.model.experiment.sample.ReportsGeneExpression;
import uk.ac.ebi.atlas.model.experiment.sdrf.Factor;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSetMultimap.flatteningToImmutableSetMultimap;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;

// I don’t like this class:
// 1. It uses reflection (and a cast)
// 2. It massages text in some places, non-consistently
//
// The ideal case to solve #1 would be to have a more consistent (aka better) ReportsGeneExpression class hierarchy,
// but the fact that contrasts have a method getDisplayName() we need to use, which depends on the subtype of
// Experiment, makes things a bit tricky. I can’t see a way of getting around this without redoing (and breaking) the
// current inheritance tree of ReportsGeneExpression, AssayGroup and Contrast. From a modelling point of view those are
// fine, it’s just that it’s hard to reconcile that view with what we’re doing here.
//
// As for #2, be aware that ExperimentDisplayDefaults::getDefaultQueryFactorType and
// ExperimentDisplayDefaults::getFactorTypes return a normalised (i.e. upper-cased with underscores) factor
// type/header (also, what’s with the use of type and header interchangeably?), whereas
// ExperimentDesign::getFactorHeaders and ExperimentDesign::getSampleCharacteristicHeaders return “denormalised” (i.e.
// lower case with blanks) headers. HeatmapFilterGroup normalises headers no matter what to be on the safe side, but
// there’s a bit of text wrangling throughout. Beware!
public class HeatmapGroupingsService {
    private final static String ALL_VALUES_KEYWORD = "all";

    public HeatmapGroupingsService() {
        throw new UnsupportedOperationException();
    }

    // TODO Cache results of serialized JSON
    public static ImmutableList<HeatmapFilterGroup> getExperimentVariablesAsHeatmapFilterGroups(Experiment<?> experiment) {
        if (!(experiment instanceof BaselineExperiment) && !(experiment instanceof DifferentialExperiment)) {
            throw new IllegalArgumentException(
                    "Experiment " + experiment.getAccession() + " of type " + experiment.getType() + " is not " +
                    "supported for heatmap display");
        }

        ImmutableList<String> assayIds =
                experiment.getDataColumnDescriptors().stream()
                        .flatMap(sample -> sample.getAssayIds().stream())
                        .collect(toImmutableList());

        // Multimap since one assay can be part of more than one contrast if it’s part of the reference assay group
        ImmutableSetMultimap<String, String> assayId2AssayGroup =
                assayIds.stream().parallel()
                        .collect(flatteningToImmutableSetMultimap(
                                Function.identity(),
                                assayId ->
                                        experiment.getDataColumnDescriptors().stream()
                                                .filter(assayGroup -> assayGroup.getAssayIds().contains(assayId))
                                                .map(ReportsGeneExpression::getId)));

        Optional<HeatmapFilterGroup> mainFilterGroup = experiment instanceof DifferentialExperiment ?
                // We add a “fake” factor that has the same role as the defaultQueryFactorType in baseline experiments
                Optional.of(
                        HeatmapFilterGroup.create(
                                "Comparison Name",
                                true,
                                ALL_VALUES_KEYWORD,
                                ((DifferentialExperiment) experiment).getDataColumnDescriptors().stream()
                                        .collect(toImmutableSetMultimap(Contrast::getDisplayName, Contrast::getId)))) :
                Optional.empty();

        ImmutableSet<String> orderedFactors =
                Streams.concat(
                            Stream.of(experiment.getDisplayDefaults().getDefaultQueryFactorType()),
                            experiment.getDisplayDefaults().getFactorTypes().stream(),
                            experiment.getExperimentalFactorHeaders().stream())
                        .filter(StringUtils::isNotBlank)    // defaultQueryFactorType in diff experiments is ""
                        .map(Factor::normalize)
                        .collect(toImmutableSet());

        // Empty for differential experiments, which is what we want: only “Comparison Name” should be primary
        ImmutableSet<String> primaryVariables = ImmutableSet.copyOf(experiment.getDisplayDefaults().getFactorTypes());

        Stream<HeatmapFilterGroup> factorGroups =
                orderedFactors.stream()
                        .map(factorHeader ->
                                HeatmapFilterGroup.create(
                                        factorHeader,
                                        primaryVariables.contains(factorHeader),
                                        experiment.getDisplayDefaults()
                                                .defaultFilterValuesForFactor(factorHeader)
                                                .orElse(ALL_VALUES_KEYWORD),
                                        mapAssayGroupsToFactors(assayId2AssayGroup, factorHeader, experiment)));

        Stream<HeatmapFilterGroup> sampleCharacteristicGroups =
                experiment.getSampleCharacteristicHeaders().stream()
                        .filter(sampleCharacteristicHeader -> !orderedFactors.contains(Factor.normalize(sampleCharacteristicHeader)))
                        .map(sampleCharacteristicHeader ->
                                HeatmapFilterGroup.create(
                                        sampleCharacteristicHeader,
                                        primaryVariables.contains(sampleCharacteristicHeader),
                                        ALL_VALUES_KEYWORD,
                                        mapAssayGroupsToSampleCharacteristics(assayId2AssayGroup, sampleCharacteristicHeader, experiment)));

        return Streams.concat(
                    mainFilterGroup.map(Stream::of).orElseGet(Stream::empty),
                    factorGroups,
                    sampleCharacteristicGroups)
                .collect(toImmutableList());
    }

    @NotNull
    private static ImmutableSetMultimap<String, String> mapAssayGroupsToFactors(
            @NotNull ImmutableMultimap<String, String> assayId2AssayGroup,
            @NotNull String factorHeader,
            @NotNull Experiment<@NotNull ?> experiment) {
        return assayId2AssayGroup.keySet().stream()
                .filter(assayId -> experiment.getFactor(assayId, factorHeader) != null)
                .collect(flatteningToImmutableSetMultimap(
                        assayId -> experiment.getFactor(assayId, factorHeader).getValue(),
                        assayId -> assayId2AssayGroup.get(assayId).stream()));
    }

    @NotNull
    private static ImmutableSetMultimap<String, String> mapAssayGroupsToSampleCharacteristics(
            @NotNull ImmutableMultimap<String, String> assayId2AssayGroup,
            @NotNull String sampleCharacteristicHeader,
            @NotNull Experiment<@NotNull?> experiment) {
        return assayId2AssayGroup.keySet().stream()
                .filter(assayId ->
                        experiment.getSampleCharacteristic(assayId, sampleCharacteristicHeader) != null)
                .collect(flatteningToImmutableSetMultimap(
                        assayId ->
                                experiment.getSampleCharacteristic(assayId, sampleCharacteristicHeader).getValue(),
                        assayId -> assayId2AssayGroup.get(assayId).stream()));
    }
}
