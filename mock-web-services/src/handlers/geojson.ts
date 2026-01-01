import {http, HttpResponse} from 'msw';
import {readJsonFile} from '../utils/fileReader.js';
import {CONFIG} from '../config.js';
import path from 'path';

export const geoJsonHandlers = [
  // GET /geojson/data
  http.get('*/geojson/data', async () => {
    try {
      const filePath = path.join(
          CONFIG.GEOJSON_RESOURCES_PATH,
          'sample_locations.json',
      );
      const geoJsonData = readJsonFile(filePath);
      console.log(JSON.stringify(geoJsonData));
      return HttpResponse.json(geoJsonData);
    } catch (error) {
      console.error(error);
      return new HttpResponse('Error loading GeoJSON data.', {status: 500});
    }
  }),
];
