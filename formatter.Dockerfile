FROM adoptopenjdk/openjdk11:alpine-slim

ENV JAVA_FORMATTER_URL "https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar"

RUN apk update \
  && apk add --no-cache --update bash wget npm shfmt

RUN npm install -g typescript prettier @typescript-eslint/parser @typescript-eslint/eslint-plugin
RUN wget $JAVA_FORMATTER_URL -O /fmt.jar

COPY .prettierrc.js /.prettierrc.js
COPY .prettierignore /.prettierignore

VOLUME /code

CMD ["sh", "-c", "\
  cd /code; \
  echo 'Start format java'; \
  java -jar /fmt.jar --replace $(find . -name '*.java' | grep -v /target); \
  echo 'Start shfmt'; \
  echo 'Files formatted:'; \
  shfmt -bn -ci -i 2 -w -l \
  $(shfmt -f . | grep -v -e /node_modules); \
  echo 'Start prettier'; \
  npx prettier --write --config .prettierrc.js --ignore-path .prettierignore server; \
  npx prettier --write --config .prettierrc.js --ignore-path .prettierignore browser-test; \
  "]
