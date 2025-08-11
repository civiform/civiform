# syntax=docker/dockerfile:1@sha256:38387523653efa0039f8e1c89bb74a30504e76ee9f565e25c9a09841f9427b05
FROM node:22-alpine@sha256:1b2479dd35a99687d6638f5976fd235e26c5b37e8122f786fcd5fe231d63de5b

WORKDIR /usr/app
ADD test_oidc_provider.js oidc.js
ADD package.json package.json
ADD package-lock.json package-lock.json
RUN npm install
CMD ["node", "oidc.js"]
