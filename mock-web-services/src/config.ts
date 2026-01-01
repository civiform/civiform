import path from 'path';

export const CONFIG = {
  PORT: 8000,
  HOST: '0.0.0.0',
  ESRI_RESOURCES_PATH: path.join(__dirname, '../../server/test/resources/esri'),
  GEOJSON_RESOURCES_PATH: path.join(
      __dirname,
      '../../server/test/resources/geojson',
  ),
};
