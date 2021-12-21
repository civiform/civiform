FROM adoptopenjdk/openjdk11:alpine-slim
RUN apk update
RUN apk add --no-cache --update bash wget
RUN wget https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar -O /fmt.jar
RUN apk add --update npm
RUN npm install -g typescript prettier @typescript-eslint/parser @typescript-eslint/eslint-plugin
COPY .prettierrc.js /.prettierrc.js
COPY .prettierignore /.prettierignore

VOLUME /code

CMD ["sh", "-c", "java -jar /fmt.jar --replace $(find /code -name '*.java'); cd /code; npx prettier --write --config /.prettierrc.js --ignore-path /.prettierignore /code"]
