import express from 'express';
import {createMiddleware} from '@mswjs/http-middleware';
import {handlers} from './handlers.js';
import {CONFIG} from './config.js';

const app = express();

// Use MSW middleware with all handlers
app.use(createMiddleware(...handlers));

// Start server
app.listen(CONFIG.PORT, CONFIG.HOST, () => {
  console.log(
      `Mock web services running on http://${CONFIG.HOST}:${CONFIG.PORT}`,
  );
  console.log('Available endpoints:');
  console.log('  - GET   /api-bridge/health-check');
  console.log('  - GET   /api-bridge/discovery');
  console.log('  - POST  /api-bridge/bridge/:slug');
  console.log('  - GET   /esri/findAddressCandidates');
  console.log('  - GET   /esri/serviceAreaFeatures');
  console.log('  - GET   /geojson/data');
});
