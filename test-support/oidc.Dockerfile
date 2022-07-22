# syntax=docker/dockerfile:1
FROM node:15-alpine

WORKDIR /usr/app
RUN npm install oidc-provider
ADD test_oidc_provider.js oidc.js
CMD ["node", "oidc.js"]
