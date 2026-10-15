# syntax=docker/dockerfile:1@sha256:4edf897a3ffa55b89f906fc8cc78afdb3f1834cc9c7083565e611a8a7d5fe99e
FROM node:24-alpine@sha256:44fce696f38ba8c987c4db89954051000bbd05ffd8d8659ba15ed4a8602e937f

# node:24 bundles npm 11; engines/engine-strict require npm >= 12
RUN npm install -g npm@^12

WORKDIR /usr/app
ADD test_oidc_provider.js oidc.js
ADD package.json package.json
ADD package-lock.json package-lock.json
RUN npm ci
CMD ["node", "oidc.js"]
