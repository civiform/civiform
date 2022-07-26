# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM alpine

ENV SBT_VERSION "1.6.2"
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH "${PATH}:${SBT_HOME}/bin"
ENV SBT_URL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME server
ENV PROJECT_LOC "${PROJECT_HOME}/${PROJECT_NAME}"

RUN set -o pipefail && \
  apk update && \
  apk add --upgrade apk-tools && \
  apk upgrade --available && \
  apk add --no-cache --update openjdk11 bash wget npm git openssh ncurses

RUN set -o pipefail && \
  npm install -g npm@8.5.1

RUN set -o pipefail && \
  mkdir -p "${SBT_HOME}" && \
  wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
  echo -ne "- with sbt ${SBT_VERSION}\n" >> /root/.built

COPY "${PROJECT_NAME}" "${PROJECT_LOC}"
COPY entrypoint.sh /entrypoint.sh

# We need to save the build assets to a seperate directory (pushRemoteCache)
RUN cd "${PROJECT_LOC}" && \
  npm install && \
  sbt update && \
  sbt compile pushRemoteCache -Dconfig.file=conf/application.dev.conf

ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 9000
WORKDIR "${PROJECT_HOME}/${PROJECT_NAME}"
