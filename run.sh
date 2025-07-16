#!/usr/bin/env bash

mkdir -p /tmp/civiform-prod/db

docker compose -f compose.prod.yml up -d
docker compose -f compose.prod.yml logs --follow
docker compose -f compose.prod.yml down
