#!/usr/bin/env bash

function show_usage {
  echo "Usage: compile-front-end-package.sh [OPTION]..."
  echo "Transpile front end components of (Single Cell) Expression Atlas to Webpack bundles."
  echo ""
  echo "All options are disabled if omitted."
  echo -e "-i\tRemove package-lock.json and node_modules directory"
  echo -e "-u\tUpgrade packages of scope @ebi-gene-expression-group to their latest versions (pre-releases such as alpha/beta apply)"
  echo -e "-a\tRun npm audit fix after npm install in each package (also enabled by NPM_AUDIT_FIX=true)"
  echo -e "-p\tGenerate Webpack bundles in production mode"
  echo ""
  echo "Environment variables:"
  echo -e "PARALLEL_JOBS\tNumber of parallel npm installs (default: number of CPUs)"
}

# Prerequistes
for APP in npm ncu
do
  if ! which $APP; then
    echo "$APP is not installed. Install Node and then run \`npm install -g npm-check-updates\`." && exit 1
  fi
done

WEBPACK_OPTS="--mode development --devtool source-map"
while getopts ":iupa" opt; do
  case $opt in
    i)
      INIT=true
      ;;
    u)
      UPGRADE=true
      ;;
    a)
      AUDIT=true
      ;;
    p)
      WEBPACK_OPTS="--mode production"
      ;;
    h)
      show_usage
      exit 0
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      echo ""
      show_usage
      exit 1
      ;;
  esac
done

if [ "${NPM_AUDIT_FIX}" = true ]; then
  AUDIT=true
fi

: "${NPM_CONFIG_LOGLEVEL:=silent}"
export NPM_CONFIG_LOGLEVEL

: "${PARALLEL_JOBS:=$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)}"

function update_npm_package {
  if [ "$UPGRADE" = true ]; then
      echo ">> $PWD$ ncu /@ebi-gene-expression-group/ --pre 1 -u"
      ncu /@ebi-gene-expression-group/ --pre 1 -u
    fi
    if [ "$INIT" = true ]; then
      echo ">> $PWD$ rm -rf node_modules package-lock.json"
      rm -rf node_modules package-lock.json
    fi
    if [ "$INIT" = true ] || [ "$UPGRADE" = true ] || [ ! -f package-lock.json ]; then
      echo ">> $PWD$ npm install --silent"
      npm install --silent
    else
      echo ">> $PWD$ npm ci --silent"
      npm ci --silent
    fi
    if [ "$AUDIT" = true ]; then
      echo ">> $PWD$ npm audit fix --silent"
      npm audit fix --silent
    fi
}

export INIT UPGRADE AUDIT
export -f update_npm_package

cd app/src/main/javascript

find modules -type d -mindepth 1 -maxdepth 1 | \
  xargs -n1 -t -P "${PARALLEL_JOBS}" -I {} bash -c \
    "cd {}; update_npm_package; npm --silent run prepare"
find bundles -type d -mindepth 1 -maxdepth 1 | \
  xargs -n1 -t -P "${PARALLEL_JOBS}" -I {} bash -c \
    "cd {}; update_npm_package"

update_npm_package
echo ">> $PWD$ npx webpack $WEBPACK_OPTS"
npx webpack $WEBPACK_OPTS
