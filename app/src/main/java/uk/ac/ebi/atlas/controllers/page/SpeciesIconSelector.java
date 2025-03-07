package uk.ac.ebi.atlas.controllers.page;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum SpeciesIconSelector {

    ANOLIS_CAROLINENSIS("7", "blue"),
    ARABIDOPSIS("B", "green"),
    BETA_VULGARIS("B", "green"),
    BOS_TAURUS("C", "red"),
    BRACHYPODIUM_DISTACHYON("%", "green"),
    BRASSICA("B", "green"),
    CAENORHABDITIS_ELEGANS("W", "blue"),
    CHLAMYDOMONAS_REINHARDTII("Y", "green"),
    CHLOROCEBUS_SABAEUS("r", "red"),
    DANIO_RERIO("Z", "blue"),
    DROSOPHILA_MELANOGASTER("F", "blue"),
    EQUUS_CABALLUS("h", "red"),
    GALLUS_GALLUS("k", "red"),
    GLYCINE_MAX("^", "green"),
    GORILLA_GORILLA("G", "red"),
    HOMO_SAPIENS("H", "red"),
    HORDEUM_VULGARE("5", "green"),
    MACACA_MULATTA("r", "red"),
    MONODELPHIS_DOMESTICA("9", "red"),
    MUS_MUSCULUS("M", "red"),
    MUSA_ACUMINATA("P", "green"),
    ORYCTOLAGUS_CUNICULUS("t", "red"),
    ORYZA_SATIVA("6", "green"),
    OVIS_ARIES("x", "red"),
    PAN_PANISCUS("i", "red"),
    PAN_TROGLODYTES("i", "red"),
    PAPIO_ANUBIS("8", "red"),
    POPULUS_TRICHOCARPA("P", "green"),
    RATTUS_NORVEGICUS("R", "red"),
    SCHISTOSOMA_MANSONI("W", "blue"),
    ETARIA_ITALICA("%", "green"),
    SOLANUM_LYCOPERSICUM(")", "green"),
    SORGHUM_BICOLOR("P", "green"),
    SUS_SCROFA("p", "red"),
    TRITICUM_AESTIVUM("5", "green"),
    VITIS_VINIFERA("O", "green"),
    XENOPUS("f", "blue"),
    ZEA_MAYS("c", "green"),
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
