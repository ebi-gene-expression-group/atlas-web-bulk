#!/usr/bin/env bash

function show_usage {
  echo "Usage: compile-front-end-package.sh [OPTION]..."
  echo "Transpile front end components of (Single Cell) Expression Atlas to Webpack bundles."
  echo ""
  echo "All options are disabled if omitted."
  echo -e "-i\tRemove package-lock.json and node_modules directory"
  echo -e "-u\tUpgrade packages of scope @ebi-gene-expression-group to their latest versions (pre-releases such as alpha/beta apply)"
  echo -e "-p\tGenerate Webpack bundles in production mode"
}

# Prerequistes
for APP in npm ncu
do
  if ! which $APP; then
    echo "$APP is not installed. Install Node and then run \`npm install -g npm-check-updates\`." && exit 1
  fi
done

WEBPACK_OPTS="--mode development --devtool source-map"
while getopts ":iuph" opt; do
  case $opt in
    i)
      INIT=true
      ;;
    u)
      UPGRADE=true
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

function update_npm_package {
  local BASENAME=$(basename "$PWD")
  if [ "$UPGRADE" = true ]; then
    echo ">> [$BASENAME] Running ncu to upgrade @ebi-gene-expression-group packages to latest (including pre-releases)..."
    ncu /@ebi-gene-expression-group/ --pre 1 -u
  fi
  if [ "$INIT" = true ]; then
    echo ">> [$BASENAME] Removing node_modules and package-lock.json for a clean install..."
    rm -rf node_modules package-lock.json
  fi
  echo ">> [$BASENAME] Running npm install..."
  npm install
  echo ">> [$BASENAME] Running npm audit fix..."
  npm audit fix
}

export -f update_npm_package

if [ -n "${npm_config_cache:-}" ]; then
  mkdir -p "${npm_config_cache}"
  echo "Using npm download cache at ${npm_config_cache}"
fi

cd app/src/main/javascript

find modules -type d -mindepth 1 -maxdepth 1 | \
  xargs -n1 -t -P 4 -I {} bash -c \
    "cd {}; update_npm_package; npm run prepare"
find bundles -type d -mindepth 1 -maxdepth 1 | \
  xargs -n1 -t -P 4 -I {} bash -c \
    "cd {}; update_npm_package"

update_npm_package
if [ "${SKIP_WEBPACK:-}" = true ]; then
  echo ">> Skipping webpack (SKIP_WEBPACK=true)"
  exit 0
fi
echo ">> $PWD$ npx webpack $WEBPACK_OPTS"
npx webpack $WEBPACK_OPTS
