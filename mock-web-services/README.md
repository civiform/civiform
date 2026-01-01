# Mock Web Services

Mock web services for CiviForm testing, providing mock implementations of:

- **API Bridge**: Health check, discovery, and bridge endpoints with error simulation
- **ESRI**: Address validation and service area endpoints (file-based responses)
- **GeoJSON**: Sample location data for map question testing

## Technology Stack

- **Runtime**: Node.js 22
- **Language**: TypeScript 5.9.3
- **Framework**: Express.js with MSW (Mock Service Worker)
- **Build**: TypeScript compiler
- **Handlers**: MSW request handlers (reusable across different environments)

## Development

### Local Development

```bash
# Install dependencies
npm install

# Run in development mode
npm run dev

# Type check
npm run type-check

# Lint
npm run lint
```

### Building

```bash
# Build TypeScript to JavaScript
npm run build

# Run production build
npm start
```

### Docker Build

```bash
# Build Docker image
bin/build-mock-web-services

# Or use docker directly
docker build -f mock-web-services.Dockerfile -t civiform-mock-web-services .
```

## Endpoints

### API Bridge (`/api-bridge`)

- `GET /health-check` - Health check endpoint with timestamp response
- `GET /discovery` - Discovery endpoint returning available service endpoints with schemas
- `POST /bridge/:slug` - Bridge endpoint for data processing

**Error Simulation**: Use the `Emulate-Response-Code` header to simulate different HTTP responses:

- `400` - Bad Request
- `401` - Unauthorized
- `404` - Not Found
- `422` - Unprocessable Entity (with validation errors)
- `429` - Too Many Requests (includes `Retry-After` header)
- `500` - Internal Server Error

**Example**:

```bash
curl -H "Emulate-Response-Code: 429" http://localhost:8000/api-bridge/health-check
```

### ESRI (`/esri`)

- `GET /findAddressCandidates?address=<value>` - Address validation endpoint
  - Valid test addresses: "Address In Area", "Legit Address", "Bogus Address", "Empty Response", "Esri Error Response"
- `GET /serviceAreaFeatures?geometry=<json>` - Service area validation endpoint
  - Valid test latitudes (geometry.y): 100.0, 101.0, 102.0

### GeoJSON (`/geojson`)

- `GET /data` - Returns sample location data in GeoJSON format

## Testing

Run smoke tests to verify all endpoints:

```bash
./smoke-tests.sh
```

Expected output: All endpoints return HTTP 200.

## Architecture

The service uses **MSW (Mock Service Worker)** handlers to define mock endpoints. This approach provides:

- **Reusable handlers**: Import handlers in browser tests, unit tests, or other MSW environments
- **Type-safe mocking**: Full TypeScript support with request/response types
- **Consistent behavior**: Same handlers work in Node.js server and test environments

The service loads pre-created JSON response files from shared test resources:

- ESRI responses: `/server/test/resources/esri/*.json`
- GeoJSON data: `/server/test/resources/geojson/sample_locations.json`

These files are shared with Java unit tests (`FakeEsriClient.java`) to ensure consistency.

### Reusing Handlers

The MSW handlers are exported and can be reused in other parts of the codebase:

```typescript
// Import all handlers
import {handlers} from './mock-web-services/src'

// Or import specific handler groups
import {
  apiBridgeHandlers,
  esriHandlers,
  geoJsonHandlers,
} from './mock-web-services/src'

// Use in MSW browser/node setup
import {setupServer} from 'msw/node'
const server = setupServer(...handlers)

// Or use in browser tests
import {setupWorker} from 'msw/browser'
const worker = setupWorker(...handlers)
```

## Documentation

Additional documentation can be found in the wiki at the [Mock Web Services](https://github.com/civiform/civiform/wiki/Mock-Web-Services) page.
