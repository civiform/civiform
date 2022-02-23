FROM adoptopenjdk/openjdk11:jdk-11.0.10_9-alpine-slim

ENV SBT_VERSION "1.6.2"
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH "${PATH}:${SBT_HOME}/bin"
ENV SBT_URL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"

RUN set -o pipefail && \
    apk update && \
    apk add --no-cache --update bash wget npm git openssh && \
    mkdir -p "${SBT_HOME}" && \
    wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
    echo -ne "- with sbt ${SBT_VERSION}\n" >> /root/.built

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME universal-application-tool-0.0.1

COPY "${PROJECT_NAME}" "${PROJECT_HOME}/${PROJECT_NAME}"

COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 9000
WORKDIR "${PROJECT_HOME}/${PROJECT_NAME}"
