# syntax=docker/dockerfile:1@sha256:b6afd42430b15f2d2a4c5a02b919e98a525b785b1aaff16747d2f623364e39b6
FROM ubuntu:24.04@sha256:c35e29c9450151419d9448b0fd75374fec4fff364a27f176fb458d472dfc9e54

RUN apt-get update -y && \
    apt-get install -y ca-certificates curl gnupg && \
    # Add nodejs to repo
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_22.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list && \
    # Cleanup packages and update the repos
    apt-get remove -y --purge cmdtest && \
    apt-get update && \
    # Install the packages
    apt-get install -y nodejs fonts-ubuntu && \
    # Update npm
    npm install -g npm && \
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

