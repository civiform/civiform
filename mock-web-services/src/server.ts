import express from 'express';
import {createMiddleware} from '@mswjs/http-middleware';
import {apiBridgeHandlers} from './handlers/apibridge.js';
import {esriHandlers} from './handlers/esri.js';
import {geoJsonHandlers} from './handlers/geojson.js';
import {CONFIG} from './config.js';

const app = express();

// Combine all MSW handlers
const handlers = [...apiBridgeHandlers, ...esriHandlers, ...geoJsonHandlers];

// Use MSW middleware with all handlers
app.use(createMiddleware(...handlers));

// Start server
app.listen(CONFIG.PORT, CONFIG.HOST, () => {
  console.log(
      `Mock web services running on http://${CONFIG.HOST}:${CONFIG.PORT}`,
  );
  console.log('Available endpoints:');
  console.log('  - /api-bridge/health-check');
  console.log('  - /api-bridge/discovery');
  console.log('  - /api-bridge/bridge/:slug');
  console.log('  - /esri/findAddressCandidates');
  console.log('  - /esri/serviceAreaFeatures');
  console.log('  - /geojson/data');
});
