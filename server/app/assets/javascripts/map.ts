import {
  LngLatLike,
  Map as MapLibreMap,
  MapGeoJSONFeature,
  Popup,
} from 'maplibre-gl'
import {GeoJsonProperties} from 'geojson'
import {MapData, CF_SELECT_LOCATION_BUTTON} from './map-util'
import {initLocationSelection} from './map-selection'
import {initFilters} from './map-filters'

const mapInstances = new Map<string, MapLibreMap>()
export const init = () => {
  const mapDataObject = window.app?.data?.maps || {}

  Object.entries(mapDataObject).forEach(([mapId, mapData]) => {
    const map = renderMap(mapId, mapData as MapData)
    mapInstances.set(mapId, map)
    initLocationSelection(mapId)
    initFilters(mapId, mapInstances)
  })
}

const renderMap = (mapId: string, mapData: MapData): MapLibreMap => {
  const settings = mapData.settings || {}
  const geoJson = mapData.geoJson || {}

  const map = new MapLibreMap({
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

    // TODO: Add custom icons to the map markers
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

      const popup = new Popup({closeButton: false}).setLngLat(coordinates)

      const popupContent = createPopupContent(settings, properties)
      if (popupContent) {
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

  return map
}

export const createPopupContent = (
  settings: MapData['settings'],
  properties: GeoJsonProperties,
): string | undefined => {
  if (!properties) return

  let popupContent = ''

  const name: string = properties[settings['nameGeoJsonKey']] as string
  if (name) {
    popupContent += `<div class="text-bold font-serif-sm padding-bottom-2">${name}</div>`
  }

  const address: string = properties[settings['addressGeoJsonKey']] as string
  if (address) {
    popupContent += `<div class="font-sans-sm">${address}</div>`
  }

  const detailsUrl: string = properties[
    settings['detailsUrlGeoJsonKey']
  ] as string
  if (detailsUrl) {
    popupContent += `<a class="font-sans-sm usa-link usa-link--external" href="${detailsUrl}" target="_blank">View more details</a>`
  }

  if (name) {
    const selectButton: string = `<button 
        type="button"
        class="usa-button usa-button--secondary margin-top-1 ${CF_SELECT_LOCATION_BUTTON}" 
        data-location-name="${name}">
        Select location
      </button>`
    popupContent += selectButton
  }

  return popupContent
}
