package uk.ac.ebi.atlas.controllers.page;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum SpeciesIconSelector {

    ANOLIS_CAROLINENSIS("7", "blue"),
    ARABIDOPSIS_LYRATA("B", "green"),
    ARABIDOPSIS_LYRATA_SUBSP_LYRATA("B", "green"),
    ARABIDOPSIS_THALIANA("B", "green"),
    BETA_VULGARIS("B", "green"),
    BETA_VULGARIS_SUBSP_VULGARIS("B", "green"),
    BOS_TAURUS("C", "red"),
    BRACHYPODIUM_DISTACHYON("%", "green"),
    BRASSICA_NAPUS("B", "green"),
    BRASSICA_OLERACEA("B", "green"),
    BRASSICA_OLERACEA_VAR_CAPITATA("B", "green"),
    BRASSICA_RAPA("B", "green"),
    BRASSICA_RAPA_SUBSP_OLEIFERA("B", "green"),
    BRASSICA_RAPA_SUBSP_PEKINENSIS("B", "green"),
    BRASSICA_RAPA_SUBSP_RAPA("B", "green"),
    CAENORHABDITIS_ELEGANS("W", "blue"),
    CHLAMYDOMONAS_REINHARDTII("Y", "green"),
    CHLOROCEBUS_SABAEUS("r", "red"),
    DANIO_RERIO("Z", "blue"),
    DROSOPHILA_MELANOGASTER("F", "blue"),
    DROSOPHILA_PSEUDOOBSCURA("F", "blue"),
    EQUUS_CABALLUS("h", "red"),
    GALLUS_GALLUS("k", "red"),
    GLYCINE_MAX("^", "green"),
    GORILLA_GORILLA("G", "red"),
    HOMO_SAPIENS("H", "red"),
    HORDEUM_VULGARE("5", "green"),
    HORDEUM_VULGARE_SUBSP_VULGARE("5", "green"),
    MACACA_MULATTA("r", "red"),
    MONODELPHIS_DOMESTICA("9", "red"),
    MUS_MUSCULUS("M", "red"),
    MUS_MUSCULUS_DOMESTICUS("M", "red"),
    MUSA_ACUMINATA("P", "green"),
    MUSA_ACUMINATA_AAA_GROUP("P", "green"),
    ORYCTOLAGUS_CUNICULUS("t", "red"),
    ORYZA_SATIVA("6", "green"),
    ORYZA_SATIVA_INDICA_GROUP("6", "green"),
    ORYZA_SATIVA_JAPONICA_GROUP("6", "green"),
    OVIS_ARIES("x", "red"),
    PAN_PANISCUS("i", "red"),
    PAN_TROGLODYTES("i", "red"),
    PAPIO_ANUBIS("8", "red"),
    POPULUS_TRICHOCARPA("P", "green"),
    RATTUS_NORVEGICUS("R", "red"),
    SCHISTOSOMA_MANSONI("W", "blue"),
    SETARIA_ITALICA("%", "green"),
    SOLANUM_LYCOPERSICUM(")", "green"),
    SOLANUM_TUBEROSUM(")", "green"),
    SORGHUM_BICOLOR("P", "green"),
    SUS_SCROFA("p", "red"),
    TRITICUM_AESTIVUM("5", "green"),
    VITIS_VINIFERA("O", "green"),
    XENOPUS("f", "blue"),
    XENOPUS_TROPICALIS("f", "blue"),
    ZEA_MAYS("c", "green"),
    ZEA_MAYS_SUBSP_MAYS("c", "green"),
    DEFAULT("❔", "grey");

    private final String speciesIconCode;
    private final String speciesColorCode;

    SpeciesIconSelector(String speciesIconCode, String speciesColorCode) {
        this.speciesIconCode = speciesIconCode;
        this.speciesColorCode = speciesColorCode;
    }

    public static Map<String, Map<String, String>> getEnumMap() {
        Map<String, Map<String, String>> enumMap = new HashMap<>();
        for (SpeciesIconSelector value : values()) {
            enumMap.put(
                StringUtils.capitalize(value.name().toLowerCase().replace("_", " ")),
                Map.of(
                    "speciesIconCode", value.getSpeciesIconCode(),
                    "speciesColorCode", value.getSpeciesColorCode()
                )
            );
        }
        return enumMap;
    }

    public String getSpeciesIconCode() {
        return speciesIconCode;
    }

    public String getSpeciesColorCode() {
        return speciesColorCode;
    }
}
