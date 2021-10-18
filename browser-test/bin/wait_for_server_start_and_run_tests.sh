#!/usr/bin/env bash

set -euo pipefail

start_time=$(date +%s)
deadline=$(($start_time + 200))
server_url=http://civiform:9000

echo polling to check server start

until $(curl --output /dev/null --silent --head --fail --max-time 2 $server_url); do
    if (( $(date +%s) > $deadline )); then
        echo "deadline exceeded waiting for server start"
        exit 1
    fi
done

echo detected server start

debug=0
for arg do
    shift
    # if debug flag, set the var and leave it out of the forwarded args list
    [ "$arg" = "--debug" ] && debug=1 && continue
    set -- "$@" "$arg"
done

if (( $debug == 1 )); then
    DEBUG=pw:api BASE_URL=$server_url yarn test $@
else
    BASE_URL=$server_url yarn test $@
fi
