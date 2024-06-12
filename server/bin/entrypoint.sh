#! /bin/bash

# This is file is used with `docker exec` in bin scripts by civiform engineers.
# It's not used by prod civiform.

npm install
sbt "$@"
