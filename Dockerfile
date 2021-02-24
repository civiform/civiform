FROM adoptopenjdk/openjdk11:alpine-slim

# sbt

ENV SBT_VERSION 1.3.13
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
RUN apk add nodejs

# Copy play project and compile it.
# This will download all the ivy2 and sbt dependencies and install them
# in the container /root directory, which saves time on future runs,
# even if the dependencies change slightly.

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME universal-application-tool-0.0.1

RUN mkdir /formatters
RUN wget https://repo1.maven.org/maven2/com/google/errorprone/error_prone_core/2.5.1/error_prone_core-2.5.1-with-dependencies.jar -O /formatters/errorprone.jar
RUN wget https://repo1.maven.org/maven2/org/checkerframework/dataflow-shaded/3.7.1/dataflow-shaded-3.7.1.jar -O /formatters/dataflow.jar
RUN wget https://repo1.maven.org/maven2/com/google/code/findbugs/jFormatString/3.0.0/jFormatString-3.0.0.jar -O /formatters/jformatstring.jar
COPY ${PROJECT_NAME} ${PROJECT_HOME}/${PROJECT_NAME}
RUN cd $PROJECT_HOME/$PROJECT_NAME && \
    sbt clean compile

CMD ["sbt"]

EXPOSE 9000
WORKDIR $PROJECT_HOME/$PROJECT_NAME
