# syntax=docker/dockerfile:1@sha256:4edf897a3ffa55b89f906fc8cc78afdb3f1834cc9c7083565e611a8a7d5fe99e
# For production images, use the adoptium.net official JRE & JDK docker images.
FROM eclipse-temurin:25.0.5.1_2-jdk-alpine@sha256:22edbb1021420ae6e248519d1af07577742314d67d4c60fcbb2e285b7e47fa78 AS stage1

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
    # npm >= 12 required by engines/engine-strict
    npm install -g npm@^12 && \
    mkdir -p "$SBT_HOME" && \
    wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built

ENV PROJECT_HOME=/usr/src
ENV PROJECT_NAME=server
ENV PROJECT_LOC="${PROJECT_HOME}/${PROJECT_NAME}"

COPY "${PROJECT_NAME}" "${PROJECT_LOC}"
RUN cd "${PROJECT_LOC}" && \
    npm ci && \
    npm run build && \
    sbt update && \
    sbt dist && \
    unzip "${PROJECT_LOC}/target/universal/civiform-server-0.0.1.zip" -d / && \
    chmod +x /civiform-server-0.0.1/bin/civiform-server

# This is a common trick to shrink container sizes. We discard everything added
# during the build phase and use only the inflated artifacts created by sbt dist.
FROM eclipse-temurin:25.0.5.1_2-jre-alpine@sha256:69c2a26e9894016e274c880fa542956d005b3755f84db765145185e87405bd89 AS stage2

# Upgrade packages for stage2 to include latest versions, and add the
# unprivileged user the server runs as.
RUN set -o pipefail && \
    apk update && \
    apk add --upgrade apk-tools && \
    apk upgrade --available && \
    apk add --no-cache --update bash && \
    addgroup -S -g 10001 civiform && \
    adduser -S -D -H -u 10001 -G civiform civiform

# The server writes its PID file and logs under its install directory, so the
# server user owns it.
COPY --from=stage1 --chown=10001:10001 /civiform-server-0.0.1 /civiform-server-0.0.1

# Logback resolves its log file path against the working directory before Play
# sets the application home, so run from the install directory.
WORKDIR /civiform-server-0.0.1

# Numeric so Kubernetes runAsNonRoot can check it.
USER 10001:10001

ARG image_tag
ENV CIVIFORM_IMAGE_TAG=$image_tag
ARG git_commit_sha
LABEL civiform.git.commit_sha=$git_commit_sha

CMD ["/civiform-server-0.0.1/bin/civiform-server", "-Dconfig.file=/civiform-server-0.0.1/conf/application.conf"]
