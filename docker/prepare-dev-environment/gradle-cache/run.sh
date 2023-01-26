#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# GRADLE_WRAPPER_DISTS_VOL_NAME
# GRADLE_RO_DEP_CACHE_VOL_NAME
source ${SCRIPT_DIR}/../../dev.env
# print_stage_name
# print_done
# print_error
source ${SCRIPT_DIR}/../utils.sh

function print_usage() {
  printf '\n%b\n\n' "Usage: ${0} [ -l FILE ]"
  printf '%b\n' "Create and populate a Gradle wrapper and RO dependency cache volume to speed up builds of"
  printf '%b\n' "Bulk Expression Atlas."
  printf '\n%b\n\n' "-h\tShow usage instructions"
}

LOG_FILE=/dev/stdout
while getopts "l:h" opt
do
  case ${opt} in
    l)
      LOG_FILE=${OPTARG}
      ;;
    h)
      print_usage
      exit 0
      ;;
    \?)
      printf '%b\n' "Invalid option: -${OPTARG}" >&2
      print_usage
      exit 2
      ;;
  esac
done

print_stage_name "🗑 Remove previous version of ${GRADLE_WRAPPER_DISTS_VOL_NAME} and ${GRADLE_RO_DEP_CACHE_VOL_NAME} if they exist"
docker volume rm ${GRADLE_WRAPPER_DISTS_VOL_NAME} ${GRADLE_RO_DEP_CACHE_VOL_NAME} >> ${LOG_FILE} 2>&1 || true
print_done

print_stage_name "💾 Create Docker volumes ${GRADLE_WRAPPER_DISTS_VOL_NAME} ${GRADLE_RO_DEP_CACHE_VOL_NAME}"
docker volume create ${GRADLE_WRAPPER_DISTS_VOL_NAME} >> ${LOG_FILE} 2>&1
docker volume create ${GRADLE_RO_DEP_CACHE_VOL_NAME} >> ${LOG_FILE} 2>&1
print_done

IMAGE_NAME=gxa-gradle-cache-builder
print_stage_name "🚧 Build Docker image ${IMAGE_NAME}"
docker build --no-cache \
-t ${IMAGE_NAME} ${SCRIPT_DIR} >> ${LOG_FILE} 2>&1
print_done

GRADLE_WRAPPER_DISTS_MAPPING=${GRADLE_WRAPPER_DISTS_VOL_NAME}:/gradle-wrapper-dists:rw
GRADLE_RO_DEP_CACHE_MAPPING=${GRADLE_RO_DEP_CACHE_VOL_NAME}:/gradle-ro-dep-cache:rw
print_stage_name "⚙ Spin up ephemeral container to copy local artifacts to dependency cache volume"
docker run --rm \
-v ${GRADLE_WRAPPER_DISTS_MAPPING} \
-v ${GRADLE_RO_DEP_CACHE_MAPPING} \
${IMAGE_NAME} >> ${LOG_FILE} 2>&1
print_done

printf '%b\n' "🙂 All done! You can inspect the volume contents mounting it in a container:"
printf '%b\n' "   docker run --rm \\"
printf '%b\n' "   -v ${GRADLE_WRAPPER_DISTS_MAPPING} \\"
printf '%b\n' "   -v ${GRADLE_RO_DEP_CACHE_MAPPING} \\"
printf '%b\n' "   -it ubuntu:jammy bash"