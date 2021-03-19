#!/usr/bin/env bash

set -euo pipefail

docker run -it --rm --ipc=host mcr.microsoft.com/playwright:bionic \
    $PWD/src
    /bin/bash
