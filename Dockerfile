ARG PLATFORM="amd64"

# The AdoptOpenJDK image fails to run on M1 Macs because it is incompatible with ARM architecture. This
# workaround uses an aarch64 (arm64) image instead when an optional platform argument is set to arm64.
# Docker's BuildKit skips unused stages so the image for the platform that isn't used will not be built.

FROM adoptopenjdk/openjdk11:jdk-11.0.14.1_1-alpine-slim as amd64
FROM bellsoft/liberica-openjdk-alpine:11.0.11-9-aarch64 as arm64

FROM ${PLATFORM}

ENV SBT_VERSION "1.6.2"
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH "${PATH}:${SBT_HOME}/bin"
ENV SBT_URL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"

RUN set -o pipefail && \
    apk update && \
    apk add --upgrade apk-tools && \
    apk upgrade --available && \
    apk add --no-cache --update bash wget npm git openssh && \
    npm install -g npm@8.5.1 && \
    mkdir -p "${SBT_HOME}" && \
    wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
    echo -ne "- with sbt ${SBT_VERSION}\n" >> /root/.built

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME server

COPY "${PROJECT_NAME}" "${PROJECT_HOME}/${PROJECT_NAME}"

COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 9000
WORKDIR "${PROJECT_HOME}/${PROJECT_NAME}"
