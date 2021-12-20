FROM adoptopenjdk/openjdk11:alpine-slim
RUN apk update
RUN apk add --no-cache --update bash wget
RUN wget https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar -O /fmt.jar
RUN apk add --update npm
RUN npm install -g typescript typescript-formatter
VOLUME /code
CMD ["sh", "-c", "java -jar /fmt.jar --replace $(find /code -name '*.java'); cd /code; tsfmt -r $(find /code/app -name '*.ts' -not -path '*node_modules*' -not -path '*node-modules*')"]
