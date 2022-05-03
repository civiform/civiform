FROM adoptopenjdk/openjdk11:alpine-slim

ENV JAVA_FORMATTER_URL "https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar"

RUN apk update && \
  apk add --no-cache --update bash wget npm shfmt

RUN npm install -g typescript prettier @typescript-eslint/parser @typescript-eslint/eslint-plugin
RUN wget $JAVA_FORMATTER_URL -O /fmt.jar

COPY .prettierrc.js /.prettierrc.js
COPY .prettierignore /.prettierignore

VOLUME /code

CMD ["sh", "-c", \
  "java -jar /fmt.jar --replace $(find /code -name '*.java'); \
  cd /code; \
  shfmt -bn -ci -i 2 -w  $(shfmt -f . | grep -v node_modules/ | grep -v infra/); \
  cd server \
  npx prettier \
  --write --config /.prettierrc.js --ignore-path /.prettierignore /code" \
  ]
