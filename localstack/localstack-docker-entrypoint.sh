#!/bin/sh
# Start nginx in daemon mode since we intend for the main
# process to be the original entrypoint for localstack.
nginx

exec docker-entrypoint.sh "$@"