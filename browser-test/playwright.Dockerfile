# syntax=docker/dockerfile:1
FROM mcr.microsoft.com/playwright:focal

ENV PROJECT_DIR /usr/src/civiform-browser-tests
# Store playwright browsers within node_modules directory. This way playwright
# library and browsers placed together and less likely go out of sync if
# there are manipulations with docker volumes.
# https://playwright.dev/docs/browsers#managing-browser-binaries-1
ENV PLAYWRIGHT_BROWSERS_PATH 0
WORKDIR $PROJECT_DIR

# Copy the node (yarn) package files (package.json and yarn.lock)
# and save them to the npm cache.
# Do this before the rest of the server code, so they don't
# get re-downloaded every time code changes.
COPY package.json ${PROJECT_DIR}
COPY yarn.lock ${PROJECT_DIR}
RUN yarn install
RUN npx playwright install

COPY . ${PROJECT_DIR}

# Re-run, to install from cache after overriting it.
RUN yarn install
RUN npx playwright install

ENTRYPOINT ["/bin/bash"]

CMD ["/usr/src/civiform-browser-tests/bin/wait_for_server_start_and_run_tests.sh"]

# Save build results to anonymous volumes for reuse
VOLUME ["/usr/src/civiform-browser-tests/node_modules"]
VOLUME ["/usr/src/civiform-browser-tests"]
