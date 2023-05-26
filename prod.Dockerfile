# syntax=docker/dockerfile:1
FROM alpine:3.18.0 AS stage1

ENV SBT_VERSION "${SBT_VERSION:-1.8.2}"
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH "${PATH}:${SBT_HOME}/bin"
ENV SBT_URL "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz"

RUN set -o pipefail && \
    apk update && \
    apk add --upgrade apk-tools && \
    apk upgrade --available && \
    apk add --no-cache --update bash wget npm git openssh openjdk11 && \
    mkdir -p "$SBT_HOME" && \
    wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME server
ENV PROJECT_LOC "${PROJECT_HOME}/${PROJECT_NAME}"

COPY "${PROJECT_NAME}" "${PROJECT_LOC}"
RUN cd "${PROJECT_LOC}" && \
    npm install -g npm@8.5.1 && \
    npm install && \
    sbt update && \
    sbt dist && \
    unzip "${PROJECT_LOC}/target/universal/civiform-server-0.0.1.zip" -d / && \
    chmod +x /civiform-server-0.0.1/bin/civiform-server

# This is a common trick to shrink container sizes. We discard everything added
# during the build phase and use only the inflated artifacts created by sbt dist.
FROM alpine:3.18.0 AS stage2

# Upgrade packages for stage2 to include latest versions.
RUN set -o pipefail && \
    apk update && \
    apk add --upgrade apk-tools && \
    apk upgrade --available && \
    apk add --no-cache --update bash openssh openjdk11-jre

ARG image_tag
ENV CIVIFORM_IMAGE_TAG=$image_tag

ARG git_commit_sha
LABEL civiform.git.commit_sha=$git_commit_sha

COPY --from=stage1 /civiform-server-0.0.1 /civiform-server-0.0.1
CMD ["/civiform-server-0.0.1/bin/civiform-server", "-Dconfig.file=/civiform-server-0.0.1/conf/application.conf"]
