package uk.ac.ebi.atlas.experimentpage.json.opentargets;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import uk.ac.ebi.atlas.commons.readers.TsvStreamer;
import uk.ac.ebi.atlas.commons.streams.ObjectInputStream;
import uk.ac.ebi.atlas.model.experiment.sample.AssayGroup;
import uk.ac.ebi.atlas.model.GeneProfilesList;
import uk.ac.ebi.atlas.model.OntologyTerm;
import uk.ac.ebi.atlas.model.Profile;
import uk.ac.ebi.atlas.model.experiment.sdrf.SampleCharacteristic;
import uk.ac.ebi.atlas.model.experiment.ExperimentDesign;
import uk.ac.ebi.atlas.model.experiment.ExperimentType;
import uk.ac.ebi.atlas.model.experiment.sdrf.Factor;
import uk.ac.ebi.atlas.model.experiment.sample.Contrast;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExperiment;
import uk.ac.ebi.atlas.model.experiment.differential.DifferentialExpression;
import uk.ac.ebi.atlas.model.experiment.differential.microarray.MicroarrayProfile;
import uk.ac.ebi.atlas.profiles.IterableObjectInputStream;
import uk.ac.ebi.atlas.profiles.MinMaxProfileRanking;
import uk.ac.ebi.atlas.profiles.differential.DifferentialProfileStreamOptions;
import uk.ac.ebi.atlas.profiles.stream.ProfileStreamFactory;
import uk.ac.ebi.atlas.resource.DataFileHub;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EvidenceService<X extends DifferentialExpression,
                             E extends DifferentialExperiment,
                             O extends DifferentialProfileStreamOptions,
                             P extends Profile<Contrast, X, P>> {
    private static final double MIN_P_VALUE = 1e-234;
    private static final String ACTIVITY_URL_TEMPLATE = "http://identifiers.org/cttv.activity/{0}";

    private final ProfileStreamFactory<Contrast, X, E, O, P> differentialProfileStreamFactory;
    private final DataFileHub dataFileHub;
    private final String expressionAtlasVersion;

    public EvidenceService(ProfileStreamFactory<Contrast, X, E, O, P> differentialProfileStreamFactory,
                           DataFileHub dataFileHub,
                           String expressionAtlasVersion) {
        this.differentialProfileStreamFactory = differentialProfileStreamFactory;
        this.dataFileHub = dataFileHub;
        this.expressionAtlasVersion = expressionAtlasVersion;
    }

    public void evidenceForExperiment(E experiment,
                                      Function<Contrast, O> queryForOneContrast,
                                      Consumer<JsonObject> yield) {
        if (shouldSkip(experiment)) {
            return;
        }

        var diseaseAssociations = getDiseaseAssociations(experiment);
        if (diseaseAssociations.size() == 0) {
            return;
        }

        var methodDescription = getMethodDescriptionFromAnalysisMethodsFile(experiment);
        var rankPerContrastPerGene = getPercentileRanks(experiment);

        for (var contrast : diseaseAssociations.keySet()) {
            for (var profile :
                    differentialProfileStreamFactory.select(
                            experiment, queryForOneContrast.apply(contrast), Collections.emptySet(),
                            p -> p.getExpression(contrast) != null,
                            new MinMaxProfileRanking<>(
                                    Comparator.comparing(p -> -Math.abs(p.getExpressionLevel(contrast))),
                                    GeneProfilesList::new))) {

                // If experiment is microarray, retrieve probe ID
                var probeId = profile instanceof MicroarrayProfile ?
                        Optional.of(((MicroarrayProfile) profile).getDesignElementName()) :
                        Optional.<String>empty();

                var expression = profile.getExpression(contrast);
                if (expression != null) {
                    piecesOfEvidence(
                            experiment,
                            methodDescription,
                            diseaseAssociations.get(contrast),
                            expression,
                            rankPerContrastPerGene.get(profile.getId()).get(contrast),
                            profile.getId(),
                            probeId,
                            contrast,
                            yield);
                }
            }
        }
    }

    private boolean shouldSkip(E experiment) {
        return !experiment.getSpecies().isUs() ||
                experiment.getType().isMicroRna() ||
                cellLineAsSampleCharacteristicButNoDiseaseAsFactor(experiment.getExperimentDesign());
    }

    private void piecesOfEvidence(E experiment,
                                  String methodDescription,
                                  DiseaseAssociation linkToDisease,
                                  X expression,
                                  Integer foldChangeRank,
                                  String ensemblGeneId,
                                  Optional<String> probeId,
                                  Contrast contrast,
                                  Consumer<JsonObject> yield) {
        for (var diseaseUri : linkToDisease.diseaseInfo().getValueOntologyTerms()) {
            yield.accept(
                    pieceOfEvidence(
                            experiment,
                            methodDescription,
                            diseaseUri,
                            linkToDisease.biosampleInfo(),
                            Pair.of(linkToDisease.testSampleLabel(), linkToDisease.referenceSampleLabel()),
                            linkToDisease.confidence(),
                            expression,
                            foldChangeRank,
                            ensemblGeneId,
                            probeId,
                            contrast,
                            linkToDisease.isCttvPrimary(),
                            linkToDisease.organismPart()));
        }
    }

    private JsonObject pieceOfEvidence(E experiment,
                                       String methodDescription,
                                       OntologyTerm diseaseUri,
                                       SampleCharacteristic biosampleInfo,
                                       Pair<String, String> testAndReferenceLabels,
                                       DiseaseAssociation.CONFIDENCE confidence,
                                       X expression,
                                       Integer foldChangeRank,
                                       String ensemblGeneId,
                                       Optional<String> probeId,
                                       Contrast contrast,
                                       boolean isCttvPrimary,
                                       SampleCharacteristic organismPart) {
        return withLiteratureReferences(
                associationRecord(
                        uniqueAssociationFields(
                                ensemblGeneId,
                                experiment.getAccession(),
                                contrast.getDisplayName(),
                                probeId
                        ),
                        target(ensemblGeneId,
                                isCttvPrimary,
                                expression),
                        disease(
                                diseaseUri,
                                biosampleInfo),
                        evidence(
                                experiment,
                                ensemblGeneId,
                                expression,
                                foldChangeRank,
                                testAndReferenceLabels,
                                contrast,
                                confidence,
                                methodDescription,
                                organismPart)),
                experiment.getPubMedIds());
    }

    private JsonObject withLiteratureReferences(JsonObject object, Collection<String> pubmedIds) {
        if (!pubmedIds.isEmpty()) {
            var literature = new JsonObject();
            var references = new JsonArray();
            for (var pubmedId : pubmedIds) {
                var reference = new JsonObject();
                reference.addProperty("lit_id", MessageFormat.format(
                        "http://europepmc.org/abstract/MED/{0}", pubmedId));
                references.add(reference);
            }
            literature.add("references", references);
            object.add("literature", literature);
        }

        return object;
    }

    private JsonObject evidence(E experiment,
                                String ensemblGeneId,
                                X expression,
                                Integer foldChangeRank,
                                Pair<String, String> testAndReferenceLabels,
                                Contrast contrast,
                                DiseaseAssociation.CONFIDENCE confidence,
                                String methodDescription,
                                SampleCharacteristic organismPart) {
        var result = new JsonObject();
        result.addProperty("is_associated", true);
        result.addProperty(
                "unique_experiment_reference", MessageFormat.format("STUDYID_{0}", experiment.getAccession()));
        result.add("urls", linkUrls(experiment.getAccession(), ensemblGeneId));
        result.add("evidence_codes", evidenceCodes(experiment.getType()));
        result.add("log2_fold_change", log2FoldChange(expression, foldChangeRank));
        result.addProperty("test_sample", testAndReferenceLabels.getLeft());
        result.addProperty("reference_sample", testAndReferenceLabels.getRight());
        result.addProperty(
                "date_asserted", new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss'Z'").format(experiment.getLastUpdate()));
        result.addProperty("experiment_overview", experiment.getDescription());
        result.addProperty("comparison_name", contrast.getDisplayName());
        result.addProperty("organism_part", organismPartProperty(organismPart));
        result.addProperty("test_replicates_n", contrast.getTestAssayGroup().getAssays().size());
        result.addProperty("reference_replicates_n", contrast.getReferenceAssayGroup().getAssays().size());
        result.addProperty("confidence_level", confidence.name().toLowerCase());
        result.add("resource_score", resourceScore(expression, methodDescription));
        result.add("provenance_type", provenanceType());
        return result;
    }

    private JsonObject linkUrl(String niceName, String url) {
        var result = new JsonObject();
        result.addProperty("nice_name", niceName);
        result.addProperty("url", url);

        return result;
    }

    private String organismPartProperty(SampleCharacteristic organismPart) {
        return organismPart.getValueOntologyTerms().stream()
                .findFirst()
                .map(OntologyTerm::uri)
                .orElse(organismPart.getValue());
    }

    private JsonArray linkUrls(String experimentAccession, String ensemblGeneId) {
        var result = new JsonArray();
        result.add(linkUrl(
                "ArrayExpress Experiment overview",
                MessageFormat.format("http://identifiers.org/arrayexpress/{0}", experimentAccession)
        ));
        result.add(linkUrl(
                "Gene expression in Expression Atlas",
                MessageFormat.format(
                        "http://www.ebi.ac.uk/gxa/experiments/{0}?geneQuery={1}",
                        experimentAccession, ensemblGeneId) //change me to the new format!
        ));
        result.add(linkUrl(
                "Baseline gene expression in Expression Atlas",
                MessageFormat.format("http://www.ebi.ac.uk/gxa/genes/{0}", ensemblGeneId)
        ));
        return result;
    }

    /*
    original comment:
    fix me- we also need to account for the GSEA codes:
    http://purl.obolibrary.org/obo/ECO:0000358 differential geneset expression evidence from microarray experiment
    http://purl.obolibrary.org/obo/ECO:0000359 differential geneset expression evidence from RNA-seq experiment
    But we are not including this data in the JSON report for now.
    */
    private JsonArray evidenceCodes(ExperimentType experimentType) {
        var result = new JsonArray();
        if (experimentType.isMicroarray()) {
            result.add(new JsonPrimitive("http://purl.obolibrary.org/obo/ECO_0000058"));
        } else if (experimentType.isRnaSeqDifferential()) {
            result.add(new JsonPrimitive("http://purl.obolibrary.org/obo/ECO_0000295"));
        }
        return result;
    }

    private JsonObject log2FoldChange(X expression, Integer foldChangeRank) {
        var result = new JsonObject();
        result.addProperty("value", expression.getFoldChange());
        result.addProperty("percentile_rank", foldChangeRank);
        return result;
    }

    private double getPValue(X expression) {
        return Double.parseDouble(
                String.format(
                        "%3.2e",
                        expression.getPValue() == 0.0 ?
                                MIN_P_VALUE :
                                expression.getPValue()));
    }

    private JsonObject resourceScore(X expression, String methodDescription) {
        var result = new JsonObject();
        /*
        probability estimates shouldn't be zero but sometimes we get them from the pipeline as rounding errors
        use the smallest positive double greater than zero,
         */
        result.addProperty("value", getPValue(expression));

        var method = new JsonObject();
        method.addProperty("description", methodDescription);
        result.add("method", method);
        result.addProperty("type", "pvalue");
        return result;
    }

    private JsonObject provenanceType() {
        var result = new JsonObject();
        var database = new JsonObject();
        database.addProperty("version", expressionAtlasVersion);
        database.addProperty("id", "Expression_Atlas");
        result.add("database", database);
        return result;
    }

    private String geneUri(String ensemblGeneId) {
        return MessageFormat.format("http://identifiers.org/ensembl/{0}", ensemblGeneId);
    }

    private String experimentAccessionUri(String experimentAccession) {
        return MessageFormat.format("http://identifiers.org/gxa.expt/{0}", experimentAccession);
    }

    //https://github.com/opentargets/json_schema/blob/master/src/bioentity/disease.json
    private JsonObject disease(OntologyTerm diseaseUri, SampleCharacteristic biosampleInfo) {
        var result = new JsonObject();
        result.addProperty("id", diseaseUri.uri());
        result.add("biosample", biosampleInfo(biosampleInfo));
        return result;
    }

    private JsonObject biosampleInfo(SampleCharacteristic biosampleInfo) {
        var result = new JsonObject();
        result.addProperty("name", biosampleInfo.getValue());
        var ontologyTerm = biosampleInfo.getValueOntologyTerms().stream().findFirst();
        ontologyTerm.ifPresent(ontologyTerm1 -> result.addProperty("id", ontologyTerm1.uri()));
        return result;
    }

    private JsonObject uniqueAssociationFields(String ensemblGeneId,
                                               String experimentAccession,
                                               String comparisonName,
                                               Optional<String> probeId) {
        JsonObject result = new JsonObject();
        result.addProperty("geneID", geneUri(ensemblGeneId));
        result.addProperty("study_id", experimentAccessionUri(experimentAccession));
        result.addProperty("comparison_name", comparisonName);
        probeId.ifPresent(x -> result.addProperty("probe_id", x));
        return result;
    }

    private String activity(boolean isCttvPrimary, X expression) {
        if (isCttvPrimary) {
            if (expression.getFoldChange() > 0) {
                return MessageFormat.format(ACTIVITY_URL_TEMPLATE, "increased_transcript_level");
            } else if (expression.getFoldChange() < 0) {
                return MessageFormat.format(ACTIVITY_URL_TEMPLATE, "decreased_transcript_level");
            }
        }

        return MessageFormat.format(ACTIVITY_URL_TEMPLATE, "unknown");
    }

    private JsonObject target(String ensemblGeneId, boolean isCttvPrimary, X expression) {
        var result = new JsonObject();
        result.addProperty("id", geneUri(ensemblGeneId));
        result.addProperty("target_type", "http://identifiers.org/cttv.target/transcript_evidence");
        result.addProperty("activity", activity(isCttvPrimary, expression));

        return result;
    }

    private JsonObject associationRecord(JsonObject uniqueAssociationFields,
                                         JsonObject target,
                                         JsonObject disease,
                                         JsonObject evidence) {
        var result = new JsonObject();
        result.addProperty("sourceID", "expression_atlas");
        result.addProperty("type", "rna_expression");
        result.addProperty("access_level", "public");
        result.add("unique_association_fields", uniqueAssociationFields);
        result.add("target", target);
        result.add("disease", disease);
        result.add("evidence", evidence);
        return result;
    }

    private ImmutableMap<Contrast, DiseaseAssociation> getDiseaseAssociations(DifferentialExperiment experiment) {
        var result = ImmutableMap.<Contrast, DiseaseAssociation>builder();
        for (var contrast: experiment.getDataColumnDescriptors()) {
            DiseaseAssociation.tryCreate(experiment, contrast)
                    .ifPresent(diseaseAssociation -> result.put(contrast, diseaseAssociation));
        }
        return result.build();
    }

    @AutoValue
    abstract static class DiseaseAssociation {
        enum CONFIDENCE {
            LOW, MEDIUM, HIGH
        }

        public abstract SampleCharacteristic biosampleInfo();
        public abstract String referenceSampleLabel();
        public abstract String testSampleLabel();
        public abstract SampleCharacteristic diseaseInfo();
        public abstract CONFIDENCE confidence();
        public abstract boolean isCttvPrimary();
        public abstract SampleCharacteristic organismPart();

        public static Optional<DiseaseAssociation> tryCreate(DifferentialExperiment experiment, Contrast contrast) {
            var biosampleInfo = getBiosampleInfo(experiment.getExperimentDesign(), contrast.getTestAssayGroup());
            var diseaseInfo = getDiseaseInfo(experiment.getExperimentDesign(), contrast.getTestAssayGroup());

            if (biosampleInfo.isPresent() && diseaseInfo.isPresent()) {
                return Optional.of(
                        DiseaseAssociation.create(
                                biosampleInfo.get(),
                                experiment.getExperimentDesign(),
                                contrast,
                                experiment.doesContrastHaveCttvPrimaryAnnotation(contrast),
                                diseaseInfo.get()));
            } else {
                return Optional.empty();
            }
        }

        public static DiseaseAssociation create(SampleCharacteristic biosampleInfo,
                                                ExperimentDesign experimentDesign,
                                                Contrast contrast,
                                                boolean isCttvPrimary,
                                                SampleCharacteristic diseaseInfo) {
            var referenceSampleLabel = factorBasedSummaryLabel(experimentDesign, contrast.getReferenceAssayGroup());
            var testSampleLabel = factorBasedSummaryLabel(experimentDesign, contrast.getTestAssayGroup());
            var confidence =
                    determineStudyConfidence(
                            experimentDesign, diseaseInfo, contrast.getTestAssayGroup(), isCttvPrimary);
            var organismPart =
                    Optional.ofNullable(
                            experimentDesign.getSampleCharacteristic(
                                    contrast.getTestAssayGroup().getFirstAssayId(),
                                    "organism part"))
                            .orElse(SampleCharacteristic.create("organism part", ""));

            return new AutoValue_EvidenceService_DiseaseAssociation(
                    biosampleInfo,
                    referenceSampleLabel,
                    testSampleLabel,
                    diseaseInfo,
                    confidence,
                    isCttvPrimary,
                    organismPart);
        }
    }

    /*
    e.g. "induced into quiescence; serum starved"
    See:
    https://github.com/opentargets/json_schema/blob/master/src/evidence/expression.json
    "test_sample", "reference_sample"
     */
    private static String factorBasedSummaryLabel(final ExperimentDesign experimentDesign, AssayGroup assayGroup) {
        return experimentDesign.getFactorValues(assayGroup.getFirstAssayId()).values().stream()
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining("; "));
    }

    /*
    https://github.com/opentargets/json_schema/blob/master/src/bioentity/disease.json#L26
    This will either be an organism part, a cell line, or a cell type.
    When we couldn't determine this we used to issue a warning asking curators to curate this.
     */
    private static Optional<SampleCharacteristic> getBiosampleInfo(final ExperimentDesign experimentDesign,
                                                                   AssayGroup testAssayGroup) {
        return Stream.of("organism part", "cell line", "cell type").flatMap(
                x -> {
                    var s = experimentDesign.getSampleCharacteristic(testAssayGroup.getFirstAssayId(), x);
                    return s == null ? Stream.empty() : Stream.of(s);
                }).findFirst();
    }

    /*
    We expect zero or one but the original code represented this as a set.
    Original comment:
    # Go through the types (should probably always only be one)...
     */
    private static Optional<SampleCharacteristic> getDiseaseInfo(final ExperimentDesign experimentDesign,
                                                                 AssayGroup testAssayGroup) {
        return experimentDesign.getSampleCharacteristics(testAssayGroup.getFirstAssayId()).stream()
                .filter(
                        sampleCharacteristic ->
                                sampleCharacteristic.getHeader().toLowerCase().contains("disease") &&
                                        !StringUtils.containsAny(
                                                sampleCharacteristic.getValue().toLowerCase(),
                                                "normal",
                                                "healthy",
                                                "control"))
                .findFirst();
    }

    /*
    Does this contrast represent association with diseases?

    If the disease is in the experimental factors, and there are no other factors, the confidence is "high".
    If the disease is in the factors but there are other factors e.g. a treatment or
    genotype etc, the confidence is "medium". If the disease is only in the
    characteristics and something else is the factor e.g a treatment, the
    confidence is "low".
    */
    private static DiseaseAssociation.CONFIDENCE determineStudyConfidence(ExperimentDesign experimentDesign,
                                                                          SampleCharacteristic diseaseCharacteristic,
                                                                          AssayGroup testAssayGroup,
                                                                          boolean isCttvPrimary) {
        var factorSet = experimentDesign.getFactors(testAssayGroup.getFirstAssayId());
        if (factorSet != null) {
            if (!factorSet.factorsByType.containsKey(Factor.normalize(diseaseCharacteristic.getHeader()))) {
                return DiseaseAssociation.CONFIDENCE.LOW;
            } else {
                if (factorSet.size() > 1 || !isCttvPrimary) {
                    return DiseaseAssociation.CONFIDENCE.MEDIUM;
                } else {
                    return DiseaseAssociation.CONFIDENCE.HIGH;
                }
            }
        }
        return DiseaseAssociation.CONFIDENCE.LOW;
    }

    private Map<String, Map<Contrast, Integer>> getPercentileRanks(E experiment) {
        return readPercentileRanks(
                experiment,
                dataFileHub.getDifferentialExperimentFiles(experiment.getAccession()).percentileRanks.get());
    }

    private Map<String, Map<Contrast, Integer>> readPercentileRanks(E experiment, ObjectInputStream<String[]> lines) {
        var whichContrastInWhichLine = percentileRanksColumnsFromHeader(lines.readNext(), experiment);
        var result = new HashMap<String, Map<Contrast, Integer>>();
        for (var line : new IterableObjectInputStream<>(lines)) {
            var resultForThisGene = new HashMap<Contrast, Integer>();

            for (var entry : whichContrastInWhichLine.entrySet()) {
                String value = line[entry.getKey()];
                if (!"NA".equals(value)) {
                    resultForThisGene.put(entry.getValue(), Integer.parseInt(value));
                }
            }
            result.put(line[0], resultForThisGene);
        }
        return result;
    }

    private Map<Integer, Contrast> percentileRanksColumnsFromHeader(String[] header, E experiment) {
        var b = ImmutableMap.<Integer, Contrast>builder();
        for (int i = 1; i < header.length; i++) {
            Contrast contrast = experiment.getDataColumnDescriptor(StringUtils.trim(header[i]));
            if (contrast != null) {
                b.put(i, contrast);
            }
        }
        return b.build();
    }

    private String getMethodDescriptionFromAnalysisMethodsFile(E experiment) {
        try (
                TsvStreamer tsvStreamer =
                        dataFileHub.getExperimentFiles(experiment.getAccession()).analysisMethods.get()) {
            return tsvStreamer.get()
                    .filter(line -> line.length > 1)
                    .filter(line -> line[0].toLowerCase().contains("differential expression"))
                    .map(line -> line[1].trim().replace("<.+?>", ""))
                    .findFirst().orElse("");
        }
    }

    /*
    If something's a factor then it is also a characteristic unless we've made a mistake.
    Example mistake was E-GEOD-23764.
     */
    private boolean cellLineAsSampleCharacteristicButNoDiseaseAsFactor(ExperimentDesign experimentDesign) {
        return (experimentDesign.getSampleCharacteristicHeaders().contains("cell line") ||
                experimentDesign.getFactorHeaders().contains("cell line"))  &&
                !experimentDesign.getFactorHeaders().contains("disease");
    }
}
