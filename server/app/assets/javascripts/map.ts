import { LngLatLike, Map, Marker, Popup } from 'maplibre-gl'
import { FeatureCollection } from 'geojson';

const testData: FeatureCollection = {
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "name": "Little Explorers Preschool",
        "address": "123 Maple St, Seattle, WA 98101"
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-122.3421, 47.6101]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "Bright Beginnings Academy",
        "address": "456 Pine St, Seattle, WA 98101"
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-122.335, 47.6119]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "Rainier Kids Center",
        "address": "789 Rainier Ave S, Seattle, WA 98144",
        "open_late": true
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-122.308, 47.5902]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "Greenwood Daycare",
        "address": "101 Greenwood Ave N, Seattle, WA 98103",
        "open_late": true
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-122.355, 47.6941]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "name": "Capitol Hill Child Care",
        "address": "202 Broadway E, Seattle, WA 98102"
      },
      "geometry": {
        "type": "Point",
        "coordinates": [-122.3208, 47.6215]
      }
    }
  ]
}


export function init() {
  populateMap();
}

export function populateMap() {
  const map = new Map({
    container: 'map', // container id
    style: {
      version: 8,
      sources: {
        osm: {
          type: 'raster',
          tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
          tileSize: 256,
          attribution: '© OpenStreetMap contributors'
        }
      },
      layers: [
        { id: 'osm', type: 'raster', source: 'osm' }
      ],
    },
    center: [ -122.3321, 47.6062], // starting position [lng, lat]
    zoom: 8, // starting zoom
  })

  testData.features.forEach(feature => {
    if (feature.geometry.type === 'Point') {
      new Marker()
        .setLngLat([feature.geometry.coordinates[0], feature.geometry.coordinates[1]])
        .setPopup(new Popup().setText(feature.properties?.name))
        .addTo(map);
    }
  })
}
