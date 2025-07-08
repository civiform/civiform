import {Map} from 'maplibre-gl'

export const init = () => {
  // base map for testing
  new Map({
    container: 'map', // container id
    style: {
      version: 8,
      sources: {
        osm: {
          type: 'raster',
          tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
          tileSize: 256,
          attribution: '© OpenStreetMap contributors',
        },
      },
      layers: [{id: 'osm', type: 'raster', source: 'osm'}],
    },
    center: [-122.3321, 47.6062], // starting position [lng, lat]
    zoom: 8, // starting zoom
  })
}
