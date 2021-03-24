#!/usr/bin/env bash

set -euo pipefail

start_time=$(date +%s)
deadline=$(($start_time + 200))

echo polling to check server start

until $(curl --output /dev/null --silent --head --fail --max-time 2 http://civiform:9000); do
    if (( $(date +%s) > $deadline )); then
        echo "deadline exceeded waiting for server start"
        exit 1
    fi
done

echo detected server start

./bin/truncate_tables.sh

debug=0
for arg in "$@"; do
    case $arg in
        -d|--debug)
        debug=1
        shift # Remove --debug from processing
        ;;
    esac
done

if (( $debug == 1 )); then
    DEBUG=pw:api yarn test $@
else
    yarn test $@
fi

