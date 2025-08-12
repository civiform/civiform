import {LngLatLike, Map, MapGeoJSONFeature, Popup} from 'maplibre-gl'

interface MapData {
  geoJson: GeoJSON.FeatureCollection
  settings: {
    [key: string]: string
  }
}

export const init = () => {
  const mapDataObject = window.app?.data?.maps || {}

  Object.entries(mapDataObject).forEach(([mapId, mapData]) => {
    renderMap(mapId, mapData as MapData)
  })
}

const renderMap = (mapId: string, mapData: MapData) => {
  const settings = mapData.settings || {}
  const geoJson = mapData.geoJson || {}

  const map = new Map({
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

  map.on('load', () => {
    map.addSource('locations', {
      type: 'geojson',
      data: geoJson,
    })
    map.addLayer({
      id: 'locations-layer',
      type: 'circle',
      source: 'locations',
      paint: {
        'circle-radius': 6,
        'circle-color': '#005EA2',
      },
    })
    map.on('click', 'locations-layer', (e) => {
      const features: MapGeoJSONFeature[] | undefined = e.features
      if (!features || features.length === 0) {
        return
      }

      const geometry = features[0].geometry as GeoJSON.Point
      const properties = features[0].properties as GeoJSON.GeoJsonProperties

      if (!geometry || !properties) {
        return
      }

      const coordinates: LngLatLike = geometry.coordinates.slice() as LngLatLike

      let popupContent = ''

      // Display name if available
      const nameKey = settings['nameKey']
      if (nameKey && properties[nameKey]) {
        const name = properties[nameKey] as string
        popupContent += `<div class="text-bold font-serif-sm padding-bottom-2">${name}</div>`
      }

      // Display address if available
      const addressKey = settings['addressKey']
      if (addressKey && properties[addressKey]) {
        const address = properties[addressKey] as string
        popupContent += `<div class="font-sans-sm">${address}</div>`
      }

      // Display details URL if available
      const detailsUrlKey = settings['detailsUrlKey']
      if (detailsUrlKey && properties[detailsUrlKey]) {
        const websiteurl = properties[detailsUrlKey] as string
        popupContent += `<a class="font-sans-sm usa-link usa-link--external" href="${websiteurl}" target="_blank">View more details</a>`
      }

      const popup = new Popup({closeButton: false}).setLngLat(coordinates)

      if (popupContent != null) {
        popup.setHTML(popupContent)
      }

      popup.addTo(map)
    })

    map.on('mouseenter', 'locations', () => {
      map.getCanvas().style.cursor = 'pointer'
    })

    map.on('mouseleave', 'locations', () => {
      map.getCanvas().style.cursor = ''
    })
  })
}
