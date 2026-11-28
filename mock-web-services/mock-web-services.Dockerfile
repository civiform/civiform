# syntax=docker/dockerfile:1@sha256:572cebbc2b15e6f84c077742952674889cb63cd5e9b80abdffcbe587b6f19183
FROM node:24-slim@sha256:873f586f3794f52281bd039bf02c048a0be08fde619338f9941bad1eb6b34530

# node:24 bundles npm 11; engines/engine-strict require npm >= 12
RUN npm install -g npm@^12 && \
    useradd --create-home appuser --no-log-init

USER appuser

WORKDIR /app

# Copy package files
COPY --chown=appuser:appuser package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY --chown=appuser:appuser tsconfig.json ./
COPY --chown=appuser:appuser src ./src

# Build TypeScript
RUN npm run build

# Copy shared test resources from server container
COPY --from=server /test/resources/esri /server/test/resources/esri
COPY --from=server /test/resources/geojson /server/test/resources/geojson

EXPOSE 8000

CMD ["npm", "start"]

