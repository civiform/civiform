# syntax=docker/dockerfile:1@sha256:dabfc0969b935b2080555ace70ee69a5261af8a8f1b4df97b9e7fbcf6722eddf
# The eclipse-temurin image and the standard openJDK11 fails to run on M1 Macs because it is incompatible with ARM architecture. This
# workaround uses an aarch64 (arm64) image instead when an optional platform argument is set to arm64.
# Docker's BuildKit skips unused stages so the image for the platform that isn't used will not be built.

FROM eclipse-temurin:17.0.16_8-jdk-alpine@sha256:eb42bc053cbff0d750d76fa0705b6faec2677131a1358d0bafcc844051b8872c AS amd64
FROM bellsoft/liberica-openjdk-alpine:17.0.16-12@sha256:ed3d715eb5d00e7929d47b3bd4c4b872d773dc4830cf34222ccc9ab3ab1c9a84 AS arm64

FROM ${TARGETARCH}

ENV JAVA_FORMATTER_URL="https://github.com/google/google-java-format/releases/download/v1.22.0/google-java-format-1.22.0-all-deps.jar"

RUN wget $JAVA_FORMATTER_URL -O /fmt.jar && \
    apk update && \
    apk add --no-cache --update bash wget npm shfmt git py3-pip py3-yapf && \
    apk cache clean

# Below we pre-install nodejs depdendencies for various
# TS codebases we have. We need all dependencies in order to
# run type-based checks with eslint. For each directory that
# contains package.json we run npm install and save resulted `node_modules`
# directory as volume.
ENV FORMATTER_DIR=/code/formatter
ENV BROWSER_TEST_DIR=/code/browser-test
ENV SERVER_DIR=/code/server

COPY .prettier* .editorconfig* /
COPY formatter/package.json formatter/package-lock.json $FORMATTER_DIR/
COPY browser-test/package.json browser-test/package-lock.json $BROWSER_TEST_DIR/
COPY server/package.json server/package-lock.json $SERVER_DIR/

RUN cd $FORMATTER_DIR && \
    npm install && \
    cd $BROWSER_TEST_DIR && \
    npm install && \
    cd $SERVER_DIR && \
    npm install

WORKDIR $SERVER_DIR

ENTRYPOINT ["/code/formatter/fmt"]

VOLUME ["/code/browser-test/node_modules"]
VOLUME ["/code/server/node_modules"]
VOLUME ["/code/formatter/node_modules"]
VOLUME ["/code"]
