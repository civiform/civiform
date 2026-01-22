import {http, HttpResponse} from 'msw';
import {readJsonFile} from '@/utils/fileReader.js';
import {CONFIG} from '@/config.js';

function getJsonFromFile(fileName: string): any {
  return readJsonFile(CONFIG.ESRI_RESOURCES_PATH, fileName);
}

/**
 * GET /esri/findAddressCandidates
 *
 * ESRI - Find address candidates endpoint handler
 */
export const getFindAddressCandidates = http.get(
    '*/esri/findAddressCandidates',
    async ({request}) => {
      try {
        const url = new URL(request.url);
        const address = url.searchParams.get('address');

        if (address === 'Address In Area' || address === 'Legit Address') {
          const data = getJsonFromFile('findAddressCandidates.json');
          return HttpResponse.json(data);
        } else if (address === 'Bogus Address') {
          const data = getJsonFromFile('findAddressCandidatesNoCandidates.json');
          return HttpResponse.json(data);
        } else if (address === 'Empty Response') {
          const data = getJsonFromFile('findAddressCandidatesEmptyResponse.json');
          return HttpResponse.json(data);
        } else if (address === 'Esri Error Response') {
          const data = getJsonFromFile('esriErrorResponse.json');
          return HttpResponse.json(data);
        } else {
          throw new Error('Invalid mock request');
        }
      } catch {
        return new HttpResponse('Bad request.', {status: 400});
      }
    },
);

/**
 * GET /esri/serviceAreaFeatures
 *
 * ESRI - Service Area Features endpoint handler
 */
export const getServiceAreaFeatures = http.get(
    '*/esri/serviceAreaFeatures',
    async ({request}) => {
      try {
        const url = new URL(request.url);
        const geometryParam = url.searchParams.get('geometry');
        if (!geometryParam) {
          throw new Error('Missing geometry parameter');
        }

        const geometry = JSON.parse(geometryParam.replace(/'/g, '"'));
        const latitude = geometry.y;

        if (latitude === 100.0) {
          const data = getJsonFromFile('serviceAreaFeatures.json');
          return HttpResponse.json(data);
        } else if (latitude === 101.0) {
          const data = getJsonFromFile('serviceAreaFeaturesNoFeatures.json');
          return HttpResponse.json(data);
        } else if (latitude === 102.0) {
          const data = getJsonFromFile('serviceAreaFeaturesNotInArea.json');
          return HttpResponse.json(data);
        } else {
          throw new Error('Invalid mock request');
        }
      } catch {
        return new HttpResponse('Bad request.', {status: 400});
      }
    },
);

/**
 * All ESRI endpoint handlers
 */
export const esriHandlers = [getFindAddressCandidates, getServiceAreaFeatures];
