FROM adoptopenjdk/openjdk11:alpine-slim

ENV JAVA_FORMATTER_URL "https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar"
RUN wget $JAVA_FORMATTER_URL -O /fmt.jar

RUN apk update && apk add --no-cache --update \
  bash wget npm shfmt git py3-pip

RUN npm install -g typescript \
  prettier \
  @typescript-eslint/parser \
  @typescript-eslint/eslint-plugin && \
  pip install yapf

COPY .prettierrc.js /.prettierrc.js
COPY .prettierignore /.prettierignore
COPY .eslintrc.json /.eslintrc.json
COPY .eslintignore /.eslintignore
COPY .editorconfig /.editorconfig

COPY formatter/fmt /fmt

VOLUME /code

ENTRYPOINT ["/fmt"]
