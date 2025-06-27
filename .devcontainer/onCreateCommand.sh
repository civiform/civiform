#! /usr/bin/env bash

# DOC: For installing additional packages and configuration in the devcontainer.
# Runs before updateContentCommand and before user secrets are available in the container.

# Install tmux
sudo apt update
sudo apt install -y tmux

# Install Graphite
npm install -g @withgraphite/graphite-cli@stable

# Install Playwright
npm install --no-save @playwright/test

# Install pre-commit
# Use pipx for installing Python applications in isolated environments
sudo apt install -y pipx
pipx ensurepath
pipx install pre-commit
pre-commit install
