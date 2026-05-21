# syntax=docker/dockerfile:1@sha256:2780b5c3bab67f1f76c781860de469442999ed1a0d7992a5efdf2cffc0e3d769
# For production images, use the adoptium.net official JRE & JDK docker images.
FROM eclipse-temurin:25.0.3_9-jdk-alpine@sha256:30d9f87d702c2c1c601ed0d31e0c88ea1ea474ee7676cda7b7a59e759181c4dd AS stage1

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
    npm ci && \
    npm run build && \
    sbt update && \
    sbt dist && \
    unzip "${PROJECT_LOC}/target/universal/civiform-server-0.0.1.zip" -d / && \
    chmod +x /civiform-server-0.0.1/bin/civiform-server

# This is a common trick to shrink container sizes. We discard everything added
# during the build phase and use only the inflated artifacts created by sbt dist.
FROM eclipse-temurin:25.0.3_9-jre-alpine@sha256:c707c0d18cb9e8556380719f80d96a7529d0746fbb42143893949b98ed2f8943 AS stage2
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
