# syntax=docker/dockerfile:1@sha256:b6afd42430b15f2d2a4c5a02b919e98a525b785b1aaff16747d2f623364e39b6
FROM node:24-alpine@sha256:c921b97d4b74f51744057454b306b418cf693865e73b8100559189605f6955b8

WORKDIR /usr/app
ADD test_oidc_provider.js oidc.js
ADD package.json package.json
ADD package-lock.json package-lock.json
RUN npm ci
CMD ["node", "oidc.js"]
