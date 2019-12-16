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
                        {type: 'sort', title: 'Type', width: 50, dataParam: 'experimentType',
                            image: {
                                Differential: {
                                    src: '${pageContext.request.contextPath}/resources/images/experiments-table/differential.png',
                                    alt: 'Differential experiment'
                                },
                                Baseline: {
                                    src: '${pageContext.request.contextPath}/resources/images/experiments-table/baseline.png',
                                    alt: 'Baseline experiment'
                                }
                            }
                        },
                        {type: 'sort', title: 'Loaded date', width: 120, dataParam: 'lastUpdate'},
                        {type: 'search', title: 'species', width: 200, dataParam: 'species'},
                        {type: 'search', title: 'experiment description', width: 400, dataParam: 'experimentDescription',
                            link: 'experimentAccession', resource: 'experiments', endpoint: 'Results'},
                        {type: 'sort', title: 'Number of assays', width: 120, dataParam: 'numberOfAssays',
                            link: 'experimentAccession', resource: 'experiments', endpoint: 'Experiment%20Design'},
                        {type: 'sort', title: 'Comparisons', width: 100, dataParam: 'numberOfContrasts',
                            link: 'experimentAccession', resource: 'experiments', endpoint: 'Experiment%20Design'},
                        {type: 'search', title: 'experiment factors', width: 200, dataParam: 'experimentalFactors'},
                        {type: 'sort', title: 'Technology', width: 220, dataParam: 'technologyType'}
                    ],
            tableFilters : [
                {label: `Kingdom`, dataParam: `kingdom`},
                {label: `Experiment Type`, dataParam: `experimentType`}
            ],
            species: '${species}',
            enableDownload: true
        }, 'experiments');
    });
</script>
