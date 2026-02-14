/**
 * Mock Web Services Handlers
 *
 * This module exports MSW request handlers for CiviForm mock services.
 * These handlers can be reused in browser tests, unit tests, or any
 * MSW-compatible environment.
 *
 * Usage in tests:
 * ```typescript
 * import { handlers } from 'mock-web-services';
 * import { setupServer } from 'msw/node';
 *
 * const server = setupServer(...handlers);
 * ```
 */

export {apiBridgeHandlers} from '@/handlers/apibridge.js';
export {esriHandlers} from '@/handlers/esri.js';
export {geoJsonHandlers} from '@/handlers/geojson.js';

// Re-export types for convenience
export * from '@/types/apibridge.js';

// Combined handlers for easy import
import {apiBridgeHandlers} from '@/handlers/apibridge.js';
import {esriHandlers} from '@/handlers/esri.js';
import {geoJsonHandlers} from '@/handlers/geojson.js';

export const handlers = [
  ...apiBridgeHandlers,
  ...esriHandlers,
  ...geoJsonHandlers,
];
