import {http, HttpResponse} from 'msw';
import {readJsonFile} from '@/utils/fileReader.js';
import {CONFIG} from '@/config.js';

/**
 * GET /geojson/data
 *
 * Geojson - Data endpoint handler
 */
export const getGeoJsonData = http.get('*/geojson/data', async () => {
  try {
    return HttpResponse.json(
        readJsonFile(CONFIG.GEOJSON_RESOURCES_PATH, 'sample_locations.json'),
    );
  } catch {
    return new HttpResponse('Error loading GeoJSON data.', {status: 500});
  }
});

/**
 * All Geojson endpoint handlers
 */
export const geoJsonHandlers = [getGeoJsonData];
