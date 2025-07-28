import {Map} from 'maplibre-gl'

export const init = () => {
  const geoJsonDataObject = window.app.data.maps || {}
  const mapElements = document.querySelectorAll('[id^="cf-map-"]')

  mapElements.forEach((mapElement) => {
    const mapId: string = mapElement.id
    let geoJsonData: GeoJSON.FeatureCollection | undefined
    try {
      geoJsonData = JSON.parse(
        geoJsonDataObject[mapId],
      ) as GeoJSON.FeatureCollection
    } catch (e) {
      console.error(`Failed to parse GeoJSON for ${mapId}`, e)
      return
    }

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
