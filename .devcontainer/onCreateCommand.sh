#! /usr/bin/env bash

# DOC: For installing additional packages and configuration in the devcontainer.
# Runs before updateContentCommand and before user secrets are available in the container.

# Install tmux
sudo apt update
sudo apt install -y tmux

# Install Playwright
npm install --no-save --no-browsers @playwright/test

# Clean up apt cache to save space
sudo apt clean
