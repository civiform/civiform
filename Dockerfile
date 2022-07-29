# syntax=docker/dockerfile:1

# The eclipse-temurin image and the standard openJDK11 fails to run on M1 Macs because it is incompatible with ARM architecture. This
# workaround uses an aarch64 (arm64) image instead when an optional platform argument is set to arm64.
# Docker's BuildKit skips unused stages so the image for the platform that isn't used will not be built.

FROM eclipse-temurin:11.0.16_8-jdk-alpine as amd64
FROM bellsoft/liberica-openjdk-alpine:11.0.16-8 as arm64

FROM ${TARGETARCH}

ENV SBT_VERSION "1.6.2"
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH "${PATH}:${SBT_HOME}/bin"
ENV SBT_URL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME server
ENV PROJECT_LOC "${PROJECT_HOME}/${PROJECT_NAME}"

RUN set -o pipefail && \
  apk update && \
  apk add --upgrade apk-tools && \
  apk upgrade --available && \
  apk add --no-cache --update openjdk11 bash wget npm git openssh ncurses

RUN set -o pipefail && \
  npm install -g npm@8.5.1

RUN set -o pipefail && \
  mkdir -p "${SBT_HOME}" && \
  wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
  echo -ne "- with sbt ${SBT_VERSION}\n" >> /root/.built

RUN mkdir -p "${PROJECT_LOC}"
WORKDIR "${PROJECT_LOC}"

RUN sbt update

COPY "${PROJECT_NAME}"/package* .
RUN npm install

# Don't copy the project source until here, so everything above gets cached.
COPY "${PROJECT_NAME}" "${PROJECT_LOC}"

# We need to save the build assets to a seperate directory (pushRemoteCache)
RUN sbt update compile pushRemoteCache -Dconfig.file=conf/application.dev.conf

COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]

# Save build results to anonymous volumes for reuse
VOLUME [ "/usr/src/server/target","/usr/src/server/project/project", "/usr/src/server/project/target", "/usr/src/server/node_modules", "/usr/src/server/.bsp/","/usr/src/server/public/stylesheets/" ]
# The server code is it's own volume
VOLUME ["/usr/src/server"]

EXPOSE 9000
