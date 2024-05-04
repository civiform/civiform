#! /usr/bin/env bash

# DOC: For installing additional packages and configuration in the devcontainer.
# Runs before updateContentCommand and before user secrets are available in the container.

echo "hello"

# Install tmux
sudo apt update
sudo apt install -y tmux

# Graphite
npm install -g @withgraphite/graphite-cli@stable && npm install --no-save @playwright/test
