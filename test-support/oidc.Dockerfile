# syntax=docker/dockerfile:1
FROM node:15-alpine

WORKDIR /usr/app
ADD test_oidc_provider.js oidc.js
ADD package.json package.json
RUN npm install
CMD ["node", "oidc.js"]
