FROM adoptopenjdk/openjdk11:alpine-slim

ENV JAVA_FORMATTER_URL "https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar"

RUN apk update && \
    apk add --no-cache --update bash wget npm

RUN npm install -g typescript prettier @typescript-eslint/parser @typescript-eslint/eslint-plugin
RUN wget $JAVA_FORMATTER_URL -O /fmt.jar

COPY .prettierrc.js /.prettierrc.js
COPY .prettierignore /.prettierignore

VOLUME /code

ENV EXCLUDE_FILE_A="/code/app/views/style/Styles.java"
ENV EXCLUDE_FILE_B="/code/app/views/style/ReferenceClasses.java"
ENV SUB_CMD="$(find /code -name '*.java' | grep -vF $EXCLUDE_FILE_A | grep -vF $EXCLUDE_FILE_B)"
ENV JAVA_CMD="java -jar /fmt.jar --replace $SUB_CMD; cd /code; npx prettier --write --config /.prettierrc.js --ignore-path /.prettierignore /code"

CMD ["sh", "-c", "eval $JAVA_CMD"]
