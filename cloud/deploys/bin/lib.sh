#!/usr/bin/env bash

# source functions for working with various providers
if [[ "${SOURCED_AWS_LIB}" != "true" ]]; then
    source ../../aws/bin/lib.sh
fi

if [[ "${SOURCED_AZURE_LIB}" != "true" ]]; then
    source 
fi
