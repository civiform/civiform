# syntax=docker/dockerfile:1@sha256:b6afd42430b15f2d2a4c5a02b919e98a525b785b1aaff16747d2f623364e39b6
FROM node:24-alpine@sha256:2867d550cf9d8bb50059a0fff528741f11a84d985c732e60e19e8e75c7239c43

WORKDIR /usr/app
ADD test_oidc_provider.js oidc.js
ADD package.json package.json
ADD package-lock.json package-lock.json
RUN npm ci
CMD ["node", "oidc.js"]
