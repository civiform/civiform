#!/bin/sh
# Start nginx in daemon mode since we intend for the main
# process to be the original entrypoint for localstack.
nginx

# Call the original localstack entrypoint:
# https://github.com/localstack/localstack/blob/6a892aa43e41ff3df25631cbe8bbe0d8c75d9992/Dockerfile#L265
exec docker-entrypoint.sh "$@"
