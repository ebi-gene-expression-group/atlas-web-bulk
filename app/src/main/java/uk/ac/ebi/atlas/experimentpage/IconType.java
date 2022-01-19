package uk.ac.ebi.atlas.experimentpage;

public enum IconType {
    PDF("icon-PDF"),
    TSV("icon-tsv"),
    XML("icon-xml"),
    TXT("icon-txt"),
    ARRAY_EXPRESS("icon-ae"),
    GEO("icon-geo"),
    ENA("icon-ena"),
    EGA("icon-ega"),
    REACTOME("icon-gsea-reactome"),
    INTERPRO("icon-gsea-interpro"),
    R_DATA("icon-Rdata"),
    EXPERIMENT_DESIGN("icon-experiment-design"),
    GENE_ONTOLOGY("icon-gsea-go"),
    MA_PLOT("icon-ma");

    private final String name;

    IconType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
