import React from 'react'
import PropTypes from 'prop-types'
import ReactTooltip from 'react-tooltip'

import ContrastInfo from './ContrastInfo'

const TooltipLoader = ({id, result}) => {
    return (
      <ReactTooltip id={id} type={`light`} className={`foobar`}>
        <ContrastInfo {...result}/>
      </ReactTooltip>
    )
}

TooltipLoader.propTypes = {
  host: PropTypes.string.isRequired,
  resource: PropTypes.string.isRequired,
  result: PropTypes.object,
  id: PropTypes.string.isRequired
}

export default TooltipLoader
