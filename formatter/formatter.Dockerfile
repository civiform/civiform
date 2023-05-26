# syntax=docker/dockerfile:1

FROM alpine:3.18.0

ENV JAVA_FORMATTER_URL "https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar"
RUN wget $JAVA_FORMATTER_URL -O /fmt.jar

RUN apk update && apk add --no-cache --update \
  openjdk11 bash wget npm shfmt git py3-pip

# Install python formatter
RUN pip install yapf

COPY .prettier* /
COPY .editorconfig* /

# Below we pre-install nodejs depdendencies for various
# TS codebases we have. We need all dependencies in order to
# run type-based checks with eslint. For each directory that
# contains package.json we run npm install and save resulted `node_modules`
# directory as volume.

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
COPY browser-test/package-lock.json $BROWSER_TEST_DIR
WORKDIR $BROWSER_TEST_DIR
RUN npm install

# Fetch node js dependencies for `server` directory.
ENV SERVER_DIR /code/server
RUN mkdir -p $SERVER_DIR
COPY server/package.json $SERVER_DIR
COPY server/package-lock.json $SERVER_DIR
WORKDIR $SERVER_DIR
RUN npm install


ENTRYPOINT ["/code/formatter/fmt"]

VOLUME ["/code/browser-test/node_modules"]
VOLUME ["/code/server/node_modules"]
VOLUME ["/code/formatter/node_modules"]
VOLUME ["/code"]
