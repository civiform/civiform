#!/usr/bin/env bash

set -e

source cloud/deploys/dev_azure/civiform_config.sh

for i in "$@"; do
  case "${i}" in
    --tag=*)
      export IMAGE_TAG="${i#*=}"
      ;;
  esac
done
