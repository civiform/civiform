FROM mcr.microsoft.com/playwright:focal

ENV PROJECT_DIR /usr/src/civiform-browser-tests

COPY . ${PROJECT_DIR}
RUN cd ${PROJECT_DIR} && yarn install

WORKDIR $PROJECT_DIR
