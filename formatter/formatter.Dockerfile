# syntax=docker/dockerfile:1
# The eclipse-temurin image and the standard openJDK11 fails to run on M1 Macs because it is incompatible with ARM architecture. This
# workaround uses an aarch64 (arm64) image instead when an optional platform argument is set to arm64.
# Docker's BuildKit skips unused stages so the image for the platform that isn't used will not be built.

FROM eclipse-temurin:17.0.4_8-jdk-alpine as amd64
FROM bellsoft/liberica-openjdk-alpine:11.0.16-8 as arm64

FROM ${TARGETARCH}

ENV JAVA_FORMATTER_URL "https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar"
RUN wget $JAVA_FORMATTER_URL -O /fmt.jar

RUN apk update && apk add --no-cache --update \
  openjdk11 bash wget npm shfmt git py3-pip terraform

RUN pip install yapf
RUN npm install --global yarn

COPY .prettier* /
COPY .editorconfig* /

# Below we pre-install nodejs depdendencies for various
# TS codebases we have. We need all dependencies in order to
# run type-based checks with eslint. For each directory that
# contains package.json we run npm install or yarn install and
# save resulted `node_modules` directory as volume.

# Fetch node js dependencies for `formatter` directory.
ENV FORMATTER_DIR /code/formatter
RUN mkdir -p $FORMATTER_DIR
COPY formatter/package.json $FORMATTER_DIR
COPY formatter/package-lock.json $FORMATTER_DIR
WORKDIR $FORMATTER_DIR
RUN npm install

# Fetch node js dependencies for `browser-test` directory.
ENV BROWSER_TEST_DIR /code/browser-test
RUN mkdir -p $BROWSER_TEST_DIR
COPY browser-test/package.json $BROWSER_TEST_DIR
COPY browser-test/yarn.lock $BROWSER_TEST_DIR
WORKDIR $BROWSER_TEST_DIR
RUN yarn install

# Fetch node js dependencies for `server` directory.
ENV SERVER_DIR /code/server
RUN mkdir -p $SERVER_DIR
COPY server/package.json $SERVER_DIR
COPY server/package-lock.json $SERVER_DIR
WORKDIR $SERVER_DIR
RUN npm install


ENTRYPOINT ["/bin/bash"]
CMD ["/code/formatter/fmt"]

VOLUME ["/code/browser-test/node_modules"]
VOLUME ["/code/server/node_modules"]
VOLUME ["/code/formatter/node_modules"]
VOLUME ["/code"]
