<%@ page contentType="text/html;charset=UTF-8" %>

<div id="experiments"></div>
<link rel="stylesheet" href="https://ebi.emblstatic.net/web_guidelines/EBI-Icon-fonts/v1.3/fonts.css" type="text/css" media="all" />

<script defer src="${pageContext.request.contextPath}/resources/js-bundles/experimentTable.bundle.js"></script>

<script>
    document.addEventListener('DOMContentLoaded', function(event) {
      experimentTable.renderRouter(
        {
          tableHeaders: [
            {
              label: 'Type',
              dataKey: 'experimentType',
              sortable: true,
              width: 0.5,
              image: {
                Differential: {
                  src: '${pageContext.request.contextPath}/resources/images/experiments-table/differential.png',
                  alt: 'Differential experiment',
                  title: 'Differential experiment'
                },
                Baseline: {
                  src: '${pageContext.request.contextPath}/resources/images/experiments-table/baseline.png',
                  alt: 'Baseline experiment',
                  title: 'Baseline experiment'
                }
              },
              linkTo: function(dataRow) { return '${pageContext.request.contextPath}/experiments?experimentType=' + dataRow.experimentType.toLowerCase(); }
            },
            {
              label: 'Load date',
              dataKey: 'loadDate',
              sortable: true,
              width: 0.5
            },
            {
              label: 'Species',
              dataKey: 'species',
              searchable: true,
              sortable: true
            },
            {
              label: 'Title',
              dataKey: 'experimentDescription',
              searchable: true,
              sortable: true,
              linkTo: function(dataRow) { return 'experiments/' + dataRow.experimentAccession + '/Results'; },
              width: 2
            },
            {
              label: 'Assays',
              dataKey: 'numberOfAssays',
              sortable: true,
              linkTo: function(dataRow) { return 'experiments/' + dataRow.experimentAccession + '/Experiment%20Design'; },
              width: 0.5
            },
            {
              label: 'Experimental factors',
              dataKey: 'experimentalFactors',
              searchable: true,
              linkTo: function(dataRow) { return 'experiments/' + dataRow.experimentAccession + '/Experiment%20Design'; }
            },
            {
              label: 'Technology',
              dataKey: 'technologyType',
              sortable: false
            }
          ],
          dropdownFilters: [
            {
              label: 'Kingdom',
              dataKey: 'kingdom'
            },
            {
              label: 'Experiment Type',
              dataKey: 'experimentType'
            }
          ],
          rowSelectionColumn: {
            label: 'Download',
            dataKey: 'experimentAccession',
            tableHeaderCellOnClick: experimentTable._validateAndDownloadExperimentFiles('${pageContext.request.contextPath}/'),
            tooltipContent:
              '<ul>' +
              '<li>Expression matrices in TPMs or log<sub>2</sub>fold-change</li>' +
              '<li>Experiment design file with experimental metadata</li>' +
              '</ul>',
            width: 0.75
          },
          basename: '${pageContext.request.contextPath}',
          host: '${pageContext.request.contextPath}/',
          resource: 'json/experiments'
        },
        'experiments');
    });
</script>
