# syntax=docker/dockerfile:1@sha256:2780b5c3bab67f1f76c781860de469442999ed1a0d7992a5efdf2cffc0e3d769
FROM node:24-slim@sha256:03eae3ef7e88a9de535496fb488d67e02b9d96a063a8967bae657744ecd513f2

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

