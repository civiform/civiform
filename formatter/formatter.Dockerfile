# syntax=docker/dockerfile:1@sha256:9857836c9ee4268391bb5b09f9f157f3c91bb15821bb77969642813b0d00518d
# The eclipse-temurin image and the standard openJDK11 fails to run on M1 Macs because it is incompatible with ARM architecture. This
# workaround uses an aarch64 (arm64) image instead when an optional platform argument is set to arm64.
# Docker's BuildKit skips unused stages so the image for the platform that isn't used will not be built.

FROM eclipse-temurin:17.0.15_6-jdk-alpine@sha256:ca5c0c9763d21cf1617cb68ef8c17228a202cd0358fa6d7510a1bbdbd49c74da AS amd64
FROM bellsoft/liberica-openjdk-alpine:17.0.13-12@sha256:3ca9f436cc6a806244b710bd461eb9b703d194965c3335c8569521ad8f2aaf9e AS arm64

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
