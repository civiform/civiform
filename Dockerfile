# syntax=docker/dockerfile:1

FROM eclipse-temurin:11.0.19_7-jdk-jammy

ENV SBT_VERSION "${SBT_VERSION:-1.8.2}"
ENV INSTALL_DIR /usr/local
ENV SBT_HOME /usr/local/sbt
ENV PATH "${PATH}:${SBT_HOME}/bin"
ENV SBT_URL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME server
ENV PROJECT_LOC "${PROJECT_HOME}/${PROJECT_NAME}"


########################################################
### First, add dependancies that don't often change. ###
########################################################

# Update and add system dependancies

RUN apt update && \
  apt upgrade -y && \
  apt install -y openjdk-11-jdk bash curl wget git openssh-server ncurses-bin

# Install nodejs
RUN curl -fsSL https://deb.nodesource.com/setup_current.x | bash -
RUN apt install -y nodejs

# Install npm (node)
#RUN npm install -g npm@8.5.1

# Download sbt
RUN mkdir -p "${SBT_HOME}" && \
  wget -qO - "${SBT_URL}" | tar xz -C "${INSTALL_DIR}" && \
  echo -ne "- with sbt ${SBT_VERSION}\n" >> /root/.built

# Make the project dir and install sbt
# (cannot do it in root dir)
RUN mkdir -p "${PROJECT_LOC}"
WORKDIR "${PROJECT_LOC}"

# SBT downloads a lot at run-time.
RUN sbt update

########################################################
### Install dependancies and build the server,       ###
### In order of how frequently the files change      ###
########################################################

# Copy the node package files (package.json and package-lock.json)
# and save them to the npm cache.
# Do this before the rest of the server code, so they don't
# get re-downloaded every time code changes.
COPY "${PROJECT_NAME}"/package* .
RUN npm install

# Copy over the remainder of the server code
# Everything below here is re-run whenever any file changes.
COPY "${PROJECT_NAME}" "${PROJECT_LOC}"

# We need to save the build assets to a seperate directory (pushRemoteCache)
RUN sbt update compile pushRemoteCache -Dconfig.file=conf/application.dev.conf

########################################################
### Get the volumes and startup commands set up       ###
########################################################

ENTRYPOINT ["/bin/bash"]

# Save build results to anonymous volumes for reuse
# We do this first, so they don't get shadowed by the
# local server directory when running locally.
VOLUME [ "/usr/src/server/target","/usr/src/server/project/project", "/usr/src/server/project/target", "/usr/src/server/node_modules", "/usr/src/server/.bsp/","/usr/src/server/public/stylesheets/" ]
# Then map the server code to a volume, which can be shadowed
# locally.
VOLUME ["/usr/src/server"]

EXPOSE 9000
