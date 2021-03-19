FROM mcr.microsoft.com/playwright:focal

ENV PROJECT_HOME /usr/src
ENV PROJECT_NAME civiform-browser-tests

COPY . ${PROJECT_HOME}/${PROJECT_NAME}
RUN cd $PROJECT_HOME/$PROJECT_NAME && yarn install

WORKDIR $PROJECT_HOME/$PROJECT_NAME
