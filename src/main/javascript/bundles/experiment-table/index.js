import React from 'react'
import ReactDOM from 'react-dom'

import { TableManagerRouter, _validateAndDownloadExperimentFiles } from '@ebi-gene-expression-group/atlas-experiment-table'
import { withFetchLoader } from '@ebi-gene-expression-group/atlas-react-fetch-loader'

const TableManagerRouterWithFetchLoader = withFetchLoader(TableManagerRouter)

const renderRouter = (options, target) => {
  ReactDOM.render(
    <TableManagerRouterWithFetchLoader
      {...options}
      renameDataKeys={{experiments: `dataRows`}}/>,
    document.getElementById(target))
}

export { renderRouter, _validateAndDownloadExperimentFiles }
