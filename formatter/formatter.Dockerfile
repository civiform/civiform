FROM eclipse-temurin:11.0.15_10-jdk-alpine

ENV JAVA_FORMATTER_URL "https://github.com/google/google-java-format/releases/download/google-java-format-1.9/google-java-format-1.9-all-deps.jar"
RUN wget $JAVA_FORMATTER_URL -O /fmt.jar

RUN apk update && apk add --no-cache --update \
  bash wget npm shfmt git py3-pip

RUN pip install yapf

COPY formatter/package.json /package.json
COPY formatter/package-lock.json /package-lock.json

RUN npm install

COPY formatter/fmt /fmt
VOLUME /code

ENTRYPOINT ["/fmt"]
