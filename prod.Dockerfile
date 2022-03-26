FROM adoptopenjdk/openjdk11:jdk-11.0.10_9-alpine-slim AS stage1

ENV SBT_VERSION "1.6.2"
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH "${PATH}:${SBT_HOME}/bin"
ENV SBT_URL "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz"

RUN set -o pipefail && \
    apk update && \
    apk add --upgrade apk-tools && \
    apk upgrade --available && \
    apk add --no-cache --update bash wget npm git openssh && \
    mkdir -p "$SBT_HOME" && \
    wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME universal-application-tool-0.0.1

COPY "${PROJECT_NAME}" "${PROJECT_HOME}/${PROJECT_NAME}"
RUN cd "${PROJECT_HOME}/${PROJECT_NAME}" && \
    npm install -g npm@8.5.1 && \
    npm install && \
    sbt update && \
    sbt dist

# This is a common trick to shrink container sizes.  we just throw away all that build stuff and use only the jars
# we built with sbt dist.
FROM adoptopenjdk/openjdk11:jdk-11.0.10_9-alpine-slim AS stage2
COPY --from=stage1 /usr/src/universal-application-tool-0.0.1/target/universal/universal-application-tool-0.0.1.zip /civiform.zip

ARG image_tag
ENV CIVIFORM_IMAGE_TAG=$image_tag

RUN apk add bash

RUN unzip /civiform.zip; chmod +x /universal-application-tool-0.0.1/bin/universal-application-tool
CMD ["/universal-application-tool-0.0.1/bin/universal-application-tool", "-Dconfig.file=/universal-application-tool-0.0.1/conf/application.conf"]
