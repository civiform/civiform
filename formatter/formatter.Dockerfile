# syntax=docker/dockerfile:1
# The eclipse-temurin image and the standard openJDK11 fails to run on M1 Macs because it is incompatible with ARM architecture. This
# workaround uses an aarch64 (arm64) image instead when an optional platform argument is set to arm64.
# Docker's BuildKit skips unused stages so the image for the platform that isn't used will not be built.

FROM eclipse-temurin:11.0.16_8-jdk-alpine as amd64
FROM bellsoft/liberica-openjdk-alpine:11.0.16-8 as arm64

FROM ${TARGETARCH}

ENV JAVA_FORMATTER_URL "https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar"
RUN wget $JAVA_FORMATTER_URL -O /fmt.jar

RUN apk update && apk add --no-cache --update \
  openjdk11 bash wget npm shfmt git py3-pip

RUN npm install -g typescript \
  prettier \
  @typescript-eslint/parser \
  @typescript-eslint/eslint-plugin && \
  pip install yapf

COPY .prettierrc.js /.prettierrc.js
COPY .prettierignore /.prettierignore
COPY .editorconfig /.editorconfig
COPY formatter/fmt /fmt

VOLUME /code

ENTRYPOINT ["/fmt"]
