# syntax=docker/dockerfile:1@sha256:87999aa3d42bdc6bea60565083ee17e86d1f3339802f543c0d03998580f9cb89
FROM node:24-slim@sha256:b31e7a42fdf8b8aa5f5ed477c72d694301273f1069c5a2f71d53c6482e99a2fc

RUN useradd --create-home appuser --no-log-init

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

