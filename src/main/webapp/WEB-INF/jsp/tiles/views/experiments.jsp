<%@ page contentType="text/html;charset=UTF-8" %>

<div id="experiments"></div>
<link rel="stylesheet" href="https://ebi.emblstatic.net/web_guidelines/EBI-Icon-fonts/v1.3/fonts.css" type="text/css" media="all" />

<script defer src="${pageContext.request.contextPath}/resources/js-bundles/experimentTable.bundle.js"></script>

<script>
    document.addEventListener('DOMContentLoaded', function(event) {
        experimentTable.render({
            host: '${pageContext.request.contextPath}/',
            resource: 'json/experiments',
            tableHeader:
                    [
                        {type: `sort`, title: `Type`, width: 50, dataParam: `experimentType`,
                            image: {
                                MICROARRAY_1COLOUR_MICRORNA_DIFFERENTIAL: {
                                    src: "${pageContext.request.contextPath}/resources/images/experiments-table/differential.png",
                                    alt: "Microarray 1-colour miRNA"
                                },
                                PROTEOMICS_BASELINE: {
                                    src: "${pageContext.request.contextPath}/resources/images/experiments-table/baseline.png",
                                    alt: "Proteomics baseline"
                                },
                                RNASEQ_MRNA_BASELINE: {
                                    src: "${pageContext.request.contextPath}/resources/images/experiments-table/baseline.png",
                                    alt: "RNA-Seq mRNA baseline"
                                },
                                MICROARRAY_2COLOUR_MRNA_DIFFERENTIAL: {
                                    src: "${pageContext.request.contextPath}/resources/images/experiments-table/differential.png",
                                    alt: "Microarray 2-colour mRNA"
                                },
                                MICROARRAY_1COLOUR_MRNA_DIFFERENTIAL: {
                                    src: "${pageContext.request.contextPath}/resources/images/experiments-table/differential.png",
                                    alt: "Microarray 1-colour mRNA"
                                },
                                RNASEQ_MRNA_DIFFERENTIAL: {
                                    src: "${pageContext.request.contextPath}/resources/images/experiments-table/differential.png",
                                    alt: "RNA-Seq mRNA differential"
                                }
                            }
                        },
                        {type: `sort`, title: `Loaded date`, width: 120, dataParam: `lastUpdate`},
                        {type: `search`, title: `species`, width: 200, dataParam: `species`},
                        {type: `search`, title: `experiment description`, width: 400, dataParam: `experimentDescription`,
                            link: `experimentAccession`, resource: `experiments`, endpoint: `Results`},
                        {type: `sort`, title: `Number of assays`, width: 120, dataParam: `numberOfAssays`,
                            link: `experimentAccession`, resource: `experiments`, endpoint: `Experiment%20Design`},
                        {type: `sort`, title: `Comparisons`, width: 100, dataParam: `numberOfContrasts`,
                            link: `experimentAccession`, resource: `experiments`, endpoint: `Experiment%20Design`},
                        {type: `search`, title: `experiment factors`, width: 200, dataParam: `experimentalFactors`},
                        {type: `search`, title: `array designs`, width: 220, dataParam: `arrayDesignNames`,
                            link: `arrayDesigns`, resource: `https://www.ebi.ac.uk/arrayexpress/arrays`, endpoint: ``}
                    ],
            species: '${species}',
            enableDownload: false
        }, 'experiments');
    });
</script>
