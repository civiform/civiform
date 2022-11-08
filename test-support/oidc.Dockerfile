# syntax=docker/dockerfile:1
FROM node:19-alpine

WORKDIR /usr/app
ADD test_oidc_provider.js oidc.js
ADD package.json package.json
ADD package-lock.json package-lock.json
RUN npm install
CMD ["node", "oidc.js"]
