# syntax=docker/dockerfile:1@sha256:87999aa3d42bdc6bea60565083ee17e86d1f3339802f543c0d03998580f9cb89
FROM ubuntu:24.04@sha256:c4a8d5503dfb2a3eb8ab5f807da5bc69a85730fb49b5cfca2330194ebcc41c7b

RUN apt-get update -y && \
    apt-get install -y ca-certificates curl gnupg && \
    # Add nodejs to repo
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_22.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list && \
    # Cleanup packages and update the repos
    apt-get update && \
    # Install the packages
    apt-get install -y nodejs fonts-ubuntu && \
    # Smoke tests
    node --version && \
    npm --version && \
    npx --yes playwright@latest install-deps chromium && \
    # remove useless files from the current layer
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/lib/apt/lists.d/* && \
    apt-get autoremove && \
    apt-get clean && \
    apt-get autoclean

ENV PROJECT_DIR=/usr/src/civiform-browser-tests

WORKDIR $PROJECT_DIR

# Copy the node (npm) package files (package.json and package-lock.json)
# and save them to the npm cache.
# Do this before the rest of the server code, so they don't
# get re-downloaded every time code changes.
COPY package.json package-lock.json ${PROJECT_DIR}/

# Install npm dependencies and Playwright browsers (without --with-deps)
RUN npm ci && \
    npx playwright install chromium

COPY . ${PROJECT_DIR}

ENTRYPOINT ["/bin/bash"]

CMD ["/usr/src/civiform-browser-tests/bin/wait_for_server_start_and_run_tests.sh"]

# Save build results to anonymous volumes for reuse
VOLUME ["/usr/src/civiform-browser-tests/node_modules"]
VOLUME ["/usr/src/civiform-browser-tests"]

# Symlink the fonts config
# This is to solve https://github.com/civiform/civiform/issues/3225. It forces
# `fc-match` to pick a font that contains bold styles for the `system-ui`
# generic font family.
ENV FONTCONFIG_DIR=/root/.config/fontconfig
RUN mkdir -p ${FONTCONFIG_DIR} && \
    ln -s ${PROJECT_DIR}/fonts.conf ${FONTCONFIG_DIR}/fonts.conf

