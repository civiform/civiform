# syntax=docker/dockerfile:1@sha256:38387523653efa0039f8e1c89bb74a30504e76ee9f565e25c9a09841f9427b05
# For production images, use the adoptium.net official JRE & JDK docker images.
FROM --platform=linux/amd64 eclipse-temurin:17.0.15_6-jdk-alpine@sha256:4a763f969e0ef9abd2c77047cfde1eb98ccf7de8ba4ff843a47da7bb9580515c AS stage1

ARG SBT_VERSION
ENV SBT_VERSION="${SBT_VERSION}"
ENV INSTALL_DIR=/usr/local
ENV SBT_HOME=/usr/local/sbt
ENV PATH="${PATH}:${SBT_HOME}/bin"
ENV SBT_URL="https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz"

RUN set -o pipefail && \
    apk update && \
    apk add --upgrade apk-tools && \
    apk upgrade --available && \
    apk add --no-cache --update bash wget npm git && \
    mkdir -p "$SBT_HOME" && \
    wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built

ENV PROJECT_HOME=/usr/src
ENV PROJECT_NAME=server
ENV PROJECT_LOC="${PROJECT_HOME}/${PROJECT_NAME}"

COPY "${PROJECT_NAME}" "${PROJECT_LOC}"
RUN cd "${PROJECT_LOC}" && \
    npm install -g npm && \
    npm install && \
    sbt addCustomAssets && \
    sbt update && \
    sbt dist && \
    unzip "${PROJECT_LOC}/target/universal/civiform-server-0.0.1.zip" -d / && \
    chmod +x /civiform-server-0.0.1/bin/civiform-server

# This is a common trick to shrink container sizes. We discard everything added
# during the build phase and use only the inflated artifacts created by sbt dist.
FROM --platform=linux/amd64 eclipse-temurin:17.0.15_6-jre-alpine@sha256:2f5f20b9dd8f70c30ed1eba9fdf362d2abf4eac45376a29fb7c8a5e919d0da68 AS stage2
COPY --from=stage1 /civiform-server-0.0.1 /civiform-server-0.0.1

# Upgrade packages for stage2 to include latest versions.
RUN set -o pipefail && \
    apk update && \
    apk add --upgrade apk-tools && \
    apk upgrade --available && \
    apk add --no-cache --update bash openssh

ARG image_tag
ENV CIVIFORM_IMAGE_TAG=$image_tag

ARG git_commit_sha
LABEL civiform.git.commit_sha=$git_commit_sha

CMD ["/civiform-server-0.0.1/bin/civiform-server", "-Dconfig.file=/civiform-server-0.0.1/conf/application.conf"]
