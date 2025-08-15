import {LngLatLike, Map as MapLibreMap, Popup} from 'maplibre-gl'
import {GeoJsonProperties, Feature} from 'geojson'
import {
  MapData,
  CF_SELECT_LOCATION_BUTTON,
  mapQuerySelector,
  CF_POPUP_CONTENT_TEMPLATE,
} from './map_util'
import {initLocationSelection} from './map_question_selection'
import {initFilters} from './map_question_filters'

// Container for all the maps on the page
const maps = new Map<string, MapLibreMap>()
export const init = (): void => {
  const mapDataObject = window.app?.data?.maps || {}

  Object.entries(mapDataObject).forEach(([mapId, mapData]) => {
    const mapElement = renderMap(mapId, mapData as MapData)
    maps.set(mapId, mapElement)
    initLocationSelection(mapId)
    initFilters(mapId, mapElement, mapData as MapData)
  })
}

const createMap = (mapId: string) => {
  return new MapLibreMap({
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

const addLocationsToMap = (
  map: MapLibreMap,
  geoJson: GeoJSON.FeatureCollection,
): void => {
  // Preserve original IDs in properties because MapLibre only preserves properties when processing click events
  const modifiedGeoJson = {
    ...geoJson,
    features: geoJson.features.map((feature) => ({
      ...feature,
      properties: {
        ...feature.properties,
        originalId: feature.id,
      },
    })),
  }

  map.addSource('locations', {
    type: 'geojson',
    data: modifiedGeoJson,
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
}

const createPopupContent = (
  mapId: string,
  settings: MapData['settings'],
  properties: GeoJsonProperties,
  featureId: string,
): Node | null => {
  if (!properties) return null
  const name: string = properties[settings['nameGeoJsonKey']] as string
  const address: string = properties[settings['addressGeoJsonKey']] as string
  const detailsUrl: string = properties[
    settings['detailsUrlGeoJsonKey']
  ] as string

  const popupContentTemplate = mapQuerySelector(
    mapId,
    CF_POPUP_CONTENT_TEMPLATE,
  )
  if (!popupContentTemplate) return null

  const popupContent = popupContentTemplate.cloneNode(true) as HTMLElement
  popupContent.classList.remove('hidden', CF_POPUP_CONTENT_TEMPLATE)

  if (name) {
    const nameElement = popupContent.querySelector(
      '.cf-popup-content-location-name',
    ) as HTMLElement
    nameElement.textContent = name
    nameElement.classList.remove('hidden')
  }

  if (address) {
    const addressElement = popupContent.querySelector(
      '.cf-popup-content-location-address',
    ) as HTMLElement
    addressElement.textContent = address
    addressElement.classList.remove('hidden')
  }

  if (detailsUrl) {
    const detailsLinkElement = popupContent.querySelector(
      '.cf-popup-content-location-link',
    ) as HTMLAnchorElement
    try {
      const url = new URL(detailsUrl)
      if (url.protocol === 'http:' || url.protocol === 'https:') {
        detailsLinkElement.href = detailsUrl
        detailsLinkElement.classList.remove('hidden')
      } else {
        console.warn('Invalid URL protocol, skipping link:', detailsUrl)
      }
    } catch {
      console.warn('Invalid URL format, skipping link:', detailsUrl)
    }
  }

  if (featureId) {
    const selectLocationButton = popupContent.querySelector(
      `.${CF_SELECT_LOCATION_BUTTON}`,
    ) as HTMLButtonElement
    selectLocationButton.setAttribute('data-map-id', mapId)
    selectLocationButton.setAttribute('data-feature-id', featureId)
    selectLocationButton.classList.remove('hidden')
  }

  return popupContent
}

const addPopupsToMap = (
  mapId: string,
  map: MapLibreMap,
  settings: MapData['settings'],
): void => {
  map.on('click', 'locations-layer', (e) => {
    const features: Feature[] | undefined = e.features
    if (!features || features.length === 0) {
      return
    }

    const geometry = features[0].geometry as GeoJSON.Point
    const properties = features[0].properties

    if (!geometry || !properties) {
      return
    }

    const coordinates: LngLatLike = geometry.coordinates.slice() as LngLatLike

    const popup = new Popup({closeButton: false}).setLngLat(coordinates)

    const originalId: string = properties.originalId as string
    const popupContent = createPopupContent(
      mapId,
      settings,
      properties,
      originalId,
    )
    if (popupContent) {
      popup.setDOMContent(popupContent)
    }

    popup.addTo(map)
  })
}

const renderMap = (mapId: string, mapData: MapData): MapLibreMap => {
  const settings = mapData.settings || {}
  const geoJson = mapData.geoJson || {}

  const map = createMap(mapId)

  map.on('load', () => {
    addLocationsToMap(map, geoJson)
    addPopupsToMap(mapId, map, settings)

    map.on('mouseenter', 'locations-layer', (): void => {
      const canvas = map.getCanvas()
      if (canvas) {
        canvas.style.cursor = 'pointer'
      }
    })

    map.on('mouseleave', 'locations-layer', (): void => {
      const canvas = map.getCanvas()
      if (canvas) {
        canvas.style.cursor = ''
      }
    })
  })

  return map
}
