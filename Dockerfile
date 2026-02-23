# syntax=docker/dockerfile:1@sha256:b6afd42430b15f2d2a4c5a02b919e98a525b785b1aaff16747d2f623364e39b6

FROM eclipse-temurin:25.0.1_8-jdk-alpine@sha256:7ace075f29555df6696750ee3caffea4fb542a1db8a5b1e7578129162f071c03

ARG SBT_VERSION
ENV SBT_VERSION="${SBT_VERSION}"
ENV INSTALL_DIR=/usr/local
ENV SBT_HOME=/usr/local/sbt
ENV PATH="${PATH}:${SBT_HOME}/bin"
ENV SBT_URL="https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"

ENV PROJECT_HOME=/usr/src
ENV PROJECT_NAME=server
ENV PROJECT_LOC="${PROJECT_HOME}/${PROJECT_NAME}"


########################################################
### First, add dependancies that don't often change. ###
########################################################

# Update and add system dependancies
RUN set -o pipefail && \
  apk update && \
  apk add --upgrade apk-tools && \
  apk upgrade --available && \
  apk add --no-cache --update bash wget npm git

# Install npm (node)
RUN npm install -g npm

# Download sbt
RUN set -o pipefail && \
  mkdir -p "${SBT_HOME}" && \
  wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
  echo -ne "- with sbt ${SBT_VERSION}\n" >> /root/.built

# Make the project dir and install sbt
# (cannot do it in root dir)
RUN mkdir -p "${PROJECT_LOC}"
WORKDIR "${PROJECT_LOC}"

# SBT downloads a lot at run-time.
RUN sbt update --allow-empty

########################################################
### Install dependancies and build the server,       ###
### In order of how frequently the files change      ###
########################################################

# Copy the node package files (package.json and package-lock.json)
# and save them to the npm cache.
# Do this before the rest of the server code, so they don't
# get re-downloaded every time code changes.
COPY "${PROJECT_NAME}"/package* .
RUN npm ci

# Copy over the remainder of the server code
# Everything below here is re-run whenever any file changes.
COPY "${PROJECT_NAME}" "${PROJECT_LOC}"

# Build front end css/js files
RUN npm run build

# We need to save the build assets to a seperate directory (pushRemoteCache)
RUN sbt update compile pushRemoteCache -Dconfig.file=conf/application.dev.conf

########################################################
### Get the volumes and startup commands set up       ###
########################################################

ENTRYPOINT ["/bin/bash"]

# Save build results to anonymous volumes for reuse
# We do this first, so they don't get shadowed by the
# local server directory when running locally.
VOLUME [ "/usr/src/server/target","/usr/src/server/project/project", "/usr/src/server/project/target", "/usr/src/server/node_modules", "/usr/src/server/.bsp/","/usr/src/server/public/stylesheets/" ]
# Then map the server code to a volume, which can be shadowed
# locally.
VOLUME ["/usr/src/server"]

EXPOSE 9000
