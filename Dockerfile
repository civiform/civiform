FROM adoptopenjdk/openjdk11:jdk-11.0.10_9-alpine-slim

# sbt

ENV SBT_VERSION 1.6.2
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH ${PATH}:${SBT_HOME}/bin

RUN apk update

# Install sbt
RUN apk add --no-cache --update bash wget && mkdir -p "$SBT_HOME" && \
    wget -qO - --no-check-certificate "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" |  tar xz -C $INSTALL_DIR && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built

# Install git
RUN apk add --no-cache git openssh

# Install node.js
RUN apk add --update npm

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME universal-application-tool-0.0.1

COPY ${PROJECT_NAME} ${PROJECT_HOME}/${PROJECT_NAME}

COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 9000
WORKDIR $PROJECT_HOME/$PROJECT_NAME
