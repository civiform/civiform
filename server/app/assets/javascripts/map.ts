import {Map} from 'maplibre-gl'

export const init = () => {
  const mapElements = document.querySelectorAll('[id^="cf-map-"]')

  if (mapElements.length === 0) return

  mapElements.forEach((mapElement) => {
    const mapId: string = mapElement.id
    const geoJsonData = getGeoJsonData(mapId)

    renderMap(mapId, geoJsonData)
  })
}

const renderMap = (mapId: string, geoJsonData?: GeoJSON.FeatureCollection) => {
  console.log(`Rendering map with ID: ${mapId}`)

  if (geoJsonData) {
    console.log(`GeoJSON Data for ${mapId}:`, geoJsonData)
  }

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
    // TODO(#11095): Allow configurable center point
    center: [-122.3321, 47.6062],
    zoom: 8,
  })
}

const getGeoJsonData = (
  mapId: string,
): GeoJSON.FeatureCollection | undefined => {
  const rawData = document
    .querySelector(`[data-mapid="${mapId}"]`)
    ?.getAttribute('value')

  if (!rawData) {
    return
  }

  try {
    return JSON.parse(rawData) as GeoJSON.FeatureCollection
  } catch (e) {
    console.error(`Failed to parse GeoJSON for ${mapId}`, e)
    return
  }
}
