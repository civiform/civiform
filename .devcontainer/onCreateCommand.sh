#! /usr/bin/env bash

# DOC: For installing additional packages and configuration in the devcontainer.
# Runs before updateContentCommand and before user secrets are available in the container.

# Install tmux
sudo apt update
sudo apt install -y tmux

# Install Playwright
npm install --no-save @playwright/test

# Avoid space issues on the codespace
npm cache clean --force