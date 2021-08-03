var homepageExperimentsSummaryPanel=(window.webpackJsonp_name_=window.webpackJsonp_name_||[]).push([[8],{1323:function(e,t,r){"use strict";r.r(t),r.d(t,"render",(function(){return f}));var n=r(0),a=r.n(n),o=r(7),i=r.n(o),u=r(567),s=r.n(u),l=r(568),c=Object(l.withFetchLoader)(s.a),f=function(e,t){i.a.render(a.a.createElement(c,e),document.getElementById(t))}},1324:function(e,t,r){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.default=void 0;var n=u(r(0)),a=u(r(1)),o=u(r(528)),i=u(r(1325));function u(e){return e&&e.__esModule?e:{default:e}}function s(e){return(s="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(e){return typeof e}:function(e){return e&&"function"==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?"symbol":typeof e})(e)}function l(){return(l=Object.assign||function(e){for(var t=1;t<arguments.length;t++){var r=arguments[t];for(var n in r)Object.prototype.hasOwnProperty.call(r,n)&&(e[n]=r[n])}return e}).apply(this,arguments)}function c(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}function f(e,t){for(var r=0;r<t.length;r++){var n=t[r];n.enumerable=n.enumerable||!1,n.configurable=!0,"value"in n&&(n.writable=!0),Object.defineProperty(e,n.key,n)}}function d(e,t){return(d=Object.setPrototypeOf||function(e,t){return e.__proto__=t,e})(e,t)}function p(e){var t=function(){if("undefined"==typeof Reflect||!Reflect.construct)return!1;if(Reflect.construct.sham)return!1;if("function"==typeof Proxy)return!0;try{return Date.prototype.toString.call(Reflect.construct(Date,[],(function(){}))),!0}catch(e){return!1}}();return function(){var r,n=y(e);if(t){var a=y(this).constructor;r=Reflect.construct(n,arguments,a)}else r=n.apply(this,arguments);return m(this,r)}}function m(e,t){return!t||"object"!==s(t)&&"function"!=typeof t?function(e){if(void 0===e)throw new ReferenceError("this hasn't been initialised - super() hasn't been called");return e}(e):t}function y(e){return(y=Object.setPrototypeOf?Object.getPrototypeOf:function(e){return e.__proto__||Object.getPrototypeOf(e)})(e)}var b=function(e){!function(e,t){if("function"!=typeof t&&null!==t)throw new TypeError("Super expression must either be null or a function");e.prototype=Object.create(t&&t.prototype,{constructor:{value:e,writable:!0,configurable:!0}}),t&&d(e,t)}(s,e);var t,r,a,u=p(s);function s(){return c(this,s),u.apply(this,arguments)}return t=s,(r=[{key:"render",value:function(){var e=this.props,t=e.host,r=e.latestExperiments,a=e.featuredExperiments,u=e.responsiveCardsRowProps,s=e.tabsId;return[n.default.createElement("ul",{key:"tabs",className:"tabs","data-tabs":!0,id:s},[a.length?n.default.createElement("li",{key:"featured",className:"tabs-title is-active",style:{textTransform:"capitalize"}},n.default.createElement("a",{href:"#featured"},"Featured experiments")):null,n.default.createElement("li",{key:"latest",className:"tabs-title".concat(a.length?"":" is-active"),style:{textTransform:"capitalize"}},n.default.createElement("a",{href:"#latest"},"Latest experiments"))]),a.length?n.default.createElement("div",{key:"tabs-content-featured",className:"tabs-content","data-tabs-content":s},n.default.createElement("div",{className:"tabs-panel is-active",id:"featured"},n.default.createElement(o.default,l({cards:a},u)))):null,n.default.createElement("div",{key:"tabs-content-latest",className:"tabs-content","data-tabs-content":s},n.default.createElement("div",{className:"tabs-panel".concat(a.length?"":" is-active"),id:"latest"},n.default.createElement(i.default,{experiments:r,host:t})))]}},{key:"componentDidMount",value:function(){this.props.onComponentDidMount()}}])&&f(t.prototype,r),a&&f(t,a),s}(n.default.Component);b.propTypes={host:a.default.string.isRequired,featuredExperiments:o.default.propTypes.cards,latestExperiments:a.default.arrayOf(a.default.shape({experimentType:a.default.string.isRequired,experimentAccession:a.default.string.isRequired,experimentDescription:a.default.string.isRequired,numberOfAssays:a.default.number.isRequired,lastUpdate:a.default.string.isRequired,species:a.default.string.isRequired})).isRequired,onComponentDidMount:a.default.func,responsiveCardsRowProps:a.default.object,tabsId:a.default.string},b.defaultProps={onComponentDidMount:function(){},responsiveCardsRowProps:{},tabsId:"experiments-summary-tabs"};var v=b;t.default=v},1325:function(e,t,r){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.default=void 0;var n=i(r(0)),a=i(r(1)),o=i(r(1326));function i(e){return e&&e.__esModule?e:{default:e}}function u(){return(u=Object.assign||function(e){for(var t=1;t<arguments.length;t++){var r=arguments[t];for(var n in r)Object.prototype.hasOwnProperty.call(r,n)&&(e[n]=r[n])}return e}).apply(this,arguments)}var s=function(e){var t=e.experiments,r=e.host;return n.default.createElement("ul",{style:{listStyle:"none",marginLeft:"offset"}},Array.isArray(t)&&t.map((function(e,t){return n.default.createElement("li",{key:t},n.default.createElement(o.default,u({host:r},e)))})))};s.propTypes={experiments:a.default.arrayOf(a.default.shape({experimentType:a.default.string.isRequired,experimentAccession:a.default.string.isRequired,experimentDescription:a.default.string.isRequired,numberOfAssays:a.default.number.isRequired,lastUpdate:a.default.string.isRequired,species:a.default.string.isRequired})).isRequired,host:a.default.string.isRequired};var l=s;t.default=l},1326:function(e,t,r){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.default=void 0;var n=u(r(0)),a=u(r(1)),o=u(r(11)),i=u(r(14));function u(e){return e&&e.__esModule?e:{default:e}}function s(){var e=c(["\n  margin-bottom: 0px;\n  width: 140px;\n  cursor: default;\n  background-color: ",";\n  :hover{\n    opacity: 1.0;\n    background-color: ",";\n  }\n"]);return s=function(){return e},e}function l(){var e=c(["\n  padding-right: 0px;\n  vertical-align: middle;\n  display: table-cell;\n"]);return l=function(){return e},e}function c(e,t){return t||(t=e.slice(0)),Object.freeze(Object.defineProperties(e,{raw:{value:Object.freeze(t)}}))}var f=o.default.div(l()),d=o.default.span(s(),(function(e){return e.backgroundColor}),(function(e){return e.backgroundColor})),p=function(e){var t=e.experimentType,r=e.experimentAccession,a=e.experimentDescription,o=e.numberOfAssays,u=e.lastUpdate,s=e.species,l=e.host,c=(0,i.default)("experiments/".concat(r),l).toString();return n.default.createElement("div",{style:{display:"block",marginBottom:"1rem"}},!t.startsWith("SINGLE_CELL")&&n.default.createElement(f,null,n.default.createElement(d,{className:"button",backgroundColor:"#007c82"},t.endsWith("BASELINE")?"Baseline":"Differential")),n.default.createElement(f,{className:"hide-for-small-only"},n.default.createElement(d,{className:"button",backgroundColor:"gray",style:{minWidth:"120px"},title:"Number of assays in experiment"},o.toString().replace(/\B(?=(\d{3})+(?!\d))/g,",")," assays")),n.default.createElement(f,{className:"hide-for-small-only"},n.default.createElement("a",{className:"button",style:{marginBottom:"0px",backgroundColor:"#3497C5"},href:c},"Results")),n.default.createElement(f,{style:{paddingLeft:"1rem"}},n.default.createElement("small",null,u),n.default.createElement("br",null),n.default.createElement("a",{href:c},a," – ",n.default.createElement("i",null,s))))};p.propTypes={experimentType:a.default.string.isRequired,experimentAccession:a.default.string.isRequired,experimentDescription:a.default.string.isRequired,numberOfAssays:a.default.number.isRequired,lastUpdate:a.default.string.isRequired,species:a.default.string.isRequired,host:a.default.string.isRequired};var m=p;t.default=m},1327:function(e,t,r){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.AnimatedLoadingMessage=t.default=void 0;var n=l(r(0)),a=l(r(1)),o=function(e){if(e&&e.__esModule)return e;if(null===e||"object"!==c(e)&&"function"!=typeof e)return{default:e};var t=s();if(t&&t.has(e))return t.get(e);var r={},n=Object.defineProperty&&Object.getOwnPropertyDescriptor;for(var a in e)if(Object.prototype.hasOwnProperty.call(e,a)){var o=n?Object.getOwnPropertyDescriptor(e,a):null;o&&(o.get||o.set)?Object.defineProperty(r,a,o):r[a]=e[a]}r.default=e,t&&t.set(e,r);return r}(r(11)),i=l(r(14)),u=l(r(1328));function s(){if("function"!=typeof WeakMap)return null;var e=new WeakMap;return s=function(){return e},e}function l(e){return e&&e.__esModule?e:{default:e}}function c(e){return(c="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(e){return typeof e}:function(e){return e&&"function"==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?"symbol":typeof e})(e)}function f(e,t){var r=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),r.push.apply(r,n)}return r}function d(e){for(var t=1;t<arguments.length;t++){var r=null!=arguments[t]?arguments[t]:{};t%2?f(Object(r),!0).forEach((function(t){m(e,t,r[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(r)):f(Object(r)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(r,t))}))}return e}function p(e,t){if(null==e)return{};var r,n,a=function(e,t){if(null==e)return{};var r,n,a={},o=Object.keys(e);for(n=0;n<o.length;n++)r=o[n],t.indexOf(r)>=0||(a[r]=e[r]);return a}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)r=o[n],t.indexOf(r)>=0||Object.prototype.propertyIsEnumerable.call(e,r)&&(a[r]=e[r])}return a}function m(e,t,r){return t in e?Object.defineProperty(e,t,{value:r,enumerable:!0,configurable:!0,writable:!0}):e[t]=r,e}function y(e,t,r,n,a,o,i){try{var u=e[o](i),s=u.value}catch(e){return void r(e)}u.done?t(s):Promise.resolve(s).then(n,a)}function b(e){return function(){var t=this,r=arguments;return new Promise((function(n,a){var o=e.apply(t,r);function i(e){y(o,n,a,i,u,"next",e)}function u(e){y(o,n,a,i,u,"throw",e)}i(void 0)}))}}function v(e,t){for(var r=0;r<t.length;r++){var n=t[r];n.enumerable=n.enumerable||!1,n.configurable=!0,"value"in n&&(n.writable=!0),Object.defineProperty(e,n.key,n)}}function h(e,t){return(h=Object.setPrototypeOf||function(e,t){return e.__proto__=t,e})(e,t)}function g(e){var t=function(){if("undefined"==typeof Reflect||!Reflect.construct)return!1;if(Reflect.construct.sham)return!1;if("function"==typeof Proxy)return!0;try{return Date.prototype.toString.call(Reflect.construct(Date,[],(function(){}))),!0}catch(e){return!1}}();return function(){var r,n=P(e);if(t){var a=P(this).constructor;r=Reflect.construct(n,arguments,a)}else r=n.apply(this,arguments);return O(this,r)}}function O(e,t){return!t||"object"!==c(t)&&"function"!=typeof t?function(e){if(void 0===e)throw new ReferenceError("this hasn't been initialised - super() hasn't been called");return e}(e):t}function P(e){return(P=Object.setPrototypeOf?Object.getPrototypeOf:function(e){return e.__proto__||Object.getPrototypeOf(e)})(e)}function w(){var e=E(['\n  ::before {\n    content: "Loading, please wait";\n    animation: '," 1s linear infinite alternate;\n  }\n"]);return w=function(){return e},e}function j(){var e=E(['\n  0%   { content: "Loading, please wait"; }\n  33%  { content: "Loading, please wait."; }\n  66%  { content: "Loading, please wait.."; }\n  100% { content: "Loading, please wait..."; }\n']);return j=function(){return e},e}function E(e,t){return t||(t=e.slice(0)),Object.freeze(Object.defineProperties(e,{raw:{value:Object.freeze(t)}}))}var _=(0,o.keyframes)(j()),R=o.default.p(w(),_);t.AnimatedLoadingMessage=R;t.default=function(e){var t=function(t){!function(e,t){if("function"!=typeof t&&null!==t)throw new TypeError("Super expression must either be null or a function");e.prototype=Object.create(t&&t.prototype,{constructor:{value:e,writable:!0,configurable:!0}}),t&&h(e,t)}(y,t);var r,a,o,s,l,c,f=g(y);function y(e){var t;return function(e,t){if(!(e instanceof t))throw new TypeError("Cannot call a class as a function")}(this,y),(t=f.call(this,e)).state={data:null,isLoading:!0,error:null},t}return r=y,a=[{key:"componentDidUpdate",value:(c=b(regeneratorRuntime.mark((function e(){return regeneratorRuntime.wrap((function(e){for(;;)switch(e.prev=e.next){case 0:if(null!==this.state.data||null!==this.state.error){e.next=3;break}return e.next=3,this._loadAsyncData((0,i.default)(this.props.resource,this.props.host).toString());case 3:case"end":return e.stop()}}),e,this)}))),function(){return c.apply(this,arguments)})},{key:"componentDidMount",value:(l=b(regeneratorRuntime.mark((function e(){return regeneratorRuntime.wrap((function(e){for(;;)switch(e.prev=e.next){case 0:return e.next=2,this._loadAsyncData((0,i.default)(this.props.resource,this.props.host).toString());case 2:case"end":return e.stop()}}),e,this)}))),function(){return l.apply(this,arguments)})},{key:"_loadAsyncData",value:(s=b(regeneratorRuntime.mark((function e(t){var r,n,a=this;return regeneratorRuntime.wrap((function(e){for(;;)switch(e.prev=e.next){case 0:return e.prev=0,e.next=3,fetch(t);case 3:if((r=e.sent).ok){e.next=6;break}throw new Error("".concat(t," => ").concat(r.status));case 6:return e.next=8,r.json();case 8:n=e.sent,Object.keys(this.props.renameDataKeys).forEach((function(e){if(n[e]){var t=n[e];delete n[e],Object.assign(n,m({},a.props.renameDataKeys[e],t))}})),this.setState({data:n,isLoading:!1,error:null}),e.next=16;break;case 13:e.prev=13,e.t0=e.catch(0),this.setState({data:null,isLoading:!1,error:{description:"There was a problem communicating with the server. Please try again later.",name:e.t0.name,message:e.t0.message}});case 16:case"end":return e.stop()}}),e,this,[[0,13]])}))),function(e){return s.apply(this,arguments)})},{key:"componentDidCatch",value:function(e,t){this.setState({error:{description:"There was a problem rendering this component.",name:e.name,message:"".concat(e.message," – ").concat(t)}})}},{key:"render",value:function(){var t=this.props,r=t.errorPayloadProvider,a=t.loadingPayloadProvider,o=t.fulfilledPayloadProvider,i=p(t,["errorPayloadProvider","loadingPayloadProvider","fulfilledPayloadProvider"]),s=this.state,l=s.data,c=s.isLoading,f=s.error;return f?r?n.default.createElement(e,d(d({},i),r(f))):n.default.createElement(u.default,{error:f}):c?a?n.default.createElement(e,d(d({},i),a())):n.default.createElement(R,null):n.default.createElement(e,d(d(d({},i),l),o(l)))}}],o=[{key:"getDerivedStateFromProps",value:function(e,t){var r=(0,i.default)(e.resource,e.host).toString();return r!==t.url?{data:null,isLoading:!0,error:null,url:r}:null}}],a&&v(r.prototype,a),o&&v(r,o),y}(n.default.Component);return t.propTypes={host:a.default.string.isRequired,resource:a.default.string.isRequired,loadingPayloadProvider:a.default.func,errorPayloadProvider:a.default.func,fulfilledPayloadProvider:a.default.func,renameDataKeys:a.default.objectOf(a.default.string)},t.defaultProps={loadingPayloadProvider:null,errorPayloadProvider:null,fulfilledPayloadProvider:function(){},renameDataKeys:{}},t}},1328:function(e,t,r){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),t.default=void 0;var n=o(r(0)),a=o(r(1));function o(e){return e&&e.__esModule?e:{default:e}}var i=function(e){var t=e.error;return n.default.createElement("div",{className:"row column"},n.default.createElement("div",{className:"callout alert small"},n.default.createElement("h5",null,"Oops!"),n.default.createElement("p",null,t.description,n.default.createElement("br",null),"If the error persists, in order to help us debug the issue, please copy the URL and this message and send it to us via ",n.default.createElement("a",{href:"https://www.ebi.ac.uk/support/gxasc"},"the EBI Support & Feedback system"),":"),n.default.createElement("code",null,"".concat(t.name,": ").concat(t.message))))};i.propTypes={error:a.default.shape({description:a.default.string.isRequired,name:a.default.string.isRequired,message:a.default.string.isRequired})};var u=i;t.default=u},567:function(e,t,r){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),Object.defineProperty(t,"default",{enumerable:!0,get:function(){return a.default}});var n,a=(n=r(1324))&&n.__esModule?n:{default:n}},568:function(e,t,r){"use strict";Object.defineProperty(t,"__esModule",{value:!0}),Object.defineProperty(t,"withFetchLoader",{enumerable:!0,get:function(){return a.default}});var n,a=(n=r(1327))&&n.__esModule?n:{default:n}}},[[1323,0]]]);