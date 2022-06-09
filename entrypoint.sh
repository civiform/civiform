#! /bin/bash

npm install
sbt "$@"
apt-get update; apt-get install curl
