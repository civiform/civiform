# syntax=docker/dockerfile:1@sha256:b6afd42430b15f2d2a4c5a02b919e98a525b785b1aaff16747d2f623364e39b6
FROM eclipse-temurin:25.0.2_10-jdk-alpine@sha256:da683f4f02f9427597d8fa162b73b8222fe08596dcebaf23e4399576ff8b037e

ENV JAVA_FORMATTER_URL="https://github.com/google/google-java-format/releases/download/v1.34.1/google-java-format-1.34.1-all-deps.jar"

RUN wget $JAVA_FORMATTER_URL -O /fmt.jar && \
    apk update && \
    apk add --no-cache --update bash wget npm shfmt git py3-pip py3-yapf && \
    apk cache clean

# Below we pre-install nodejs depdendencies for various
# TS codebases we have. We need all dependencies in order to
# run type-based checks with eslint. For each directory that
# contains package.json we run `npm ci` and save resulted `node_modules`
# directory as volume.
ENV FORMATTER_DIR=/code/formatter
ENV BROWSER_TEST_DIR=/code/browser-test
ENV SERVER_DIR=/code/server

COPY .prettier* .editorconfig* /
COPY formatter/package.json formatter/package-lock.json $FORMATTER_DIR/
COPY browser-test/package.json browser-test/package-lock.json $BROWSER_TEST_DIR/
COPY server/package.json server/package-lock.json $SERVER_DIR/

RUN cd $FORMATTER_DIR && \
    npm ci && \
    cd $BROWSER_TEST_DIR && \
    npm ci && \
    cd $SERVER_DIR && \
    npm ci

WORKDIR $SERVER_DIR

ENTRYPOINT ["/code/formatter/fmt"]

VOLUME ["/code/browser-test/node_modules"]
VOLUME ["/code/server/node_modules"]
VOLUME ["/code/formatter/node_modules"]
VOLUME ["/code"]
