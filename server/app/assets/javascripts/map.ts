import {Map} from 'maplibre-gl'

export const init = () => {
  const mapElement = document.querySelector('[id$="-map"]')

  if (mapElement?.id) {
    console.log(`map.ts is initializing for map with id: ${mapElement.id}`)
    renderMap(mapElement.id)
  } else {
    console.warn('No map element found.')
  }

  const geoJsonDataElement = document.querySelector('[id$="-data"]')
  if (geoJsonDataElement) {
    const geoJsonDataElementValue: string | null =
      geoJsonDataElement.getAttribute('value')
    const geoJsonData: JSON = geoJsonDataElementValue
      ? (JSON.parse(geoJsonDataElementValue) as JSON)
      : ({} as JSON)
    console.log('GeoJSON Data:', geoJsonData)
  }
}

const renderMap = (mapId: string) => {
  console.log(`Rendering map with ID: ${mapId}`)
  new Map({
    container: mapId,
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
    // TODO(#): Allow configurable center point
    center: [-122.3321, 47.6062],
    zoom: 8,
  })
}
