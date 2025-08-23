import {LngLatLike, Map as MapLibreMap, Popup} from 'maplibre-gl'
import {GeoJsonProperties, Feature} from 'geojson'
import {
  MapData,
  mapQuerySelector,
  CF_POPUP_CONTENT_TEMPLATE,
  LOCATIONS_LAYER,
  DEFAULT_MAP_CENTER_POINT,
  DEFAULT_MAP_ZOOM,
  DEFAULT_MAP_MARKER_TYPE,
  DEFAULT_MAP_MARKER_STYLE,
  DEFAULT_MAP_STYLE,
} from './map_util'

// Container for all the maps on the page
const maps = new Map<string, MapLibreMap>()
export const init = (): void => {
  const mapDataObject = window.app?.data?.maps || {}

  Object.entries(mapDataObject).forEach(([mapId, mapData]) => {
    const mapElement = renderMap(mapId, mapData as MapData)
    maps.set(mapId, mapElement)
  })
}

const createMap = (mapId: string) => {
  return new MapLibreMap({
    container: mapId,
    style: DEFAULT_MAP_STYLE,
    // TODO(#11095): Allow configurable center point
    center: DEFAULT_MAP_CENTER_POINT,
    zoom: DEFAULT_MAP_ZOOM,
  })
}

const addLocationsToMap = (
  map: MapLibreMap,
  geoJson: GeoJSON.FeatureCollection,
): void => {
  // Preserve original IDs in properties because MapLibre only preserves properties when processing click events
  // Will need these later for filtering and selection
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

  // TODO(#11279): Add custom icons to the map markers
  map.addLayer({
    id: LOCATIONS_LAYER,
    type: DEFAULT_MAP_MARKER_TYPE,
    source: 'locations',
    paint: DEFAULT_MAP_MARKER_STYLE,
  })
}

const createPopupContent = (
  mapId: string,
  settings: MapData['settings'],
  properties: GeoJsonProperties,
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

  const popupContent =
    (popupContentTemplate.cloneNode(true) as HTMLElement) || null
  if (!popupContent) return null
  popupContent.classList.remove('hidden', CF_POPUP_CONTENT_TEMPLATE)

  if (name) {
    const nameElement =
      (popupContent.querySelector(
        '.cf-popup-content-location-name',
      ) as HTMLElement) || null
    if (nameElement) {
      nameElement.textContent = name
      nameElement.classList.remove('hidden')
    }
  }

  if (address) {
    const addressElement =
      (popupContent.querySelector(
        '.cf-popup-content-location-address',
      ) as HTMLElement) || null
    if (addressElement) {
      addressElement.textContent = address
      addressElement.classList.remove('hidden')
    }
  }

  if (detailsUrl) {
    const detailsLinkElementContainer =
      (popupContent.querySelector(
        '.cf-popup-content-location-link-container',
      ) as HTMLElement) || null
    const detailsLinkElement =
      (popupContent.querySelector(
        '.cf-popup-content-location-link',
      ) as HTMLAnchorElement) || null
    if (detailsLinkElementContainer && detailsLinkElement) {
      try {
        const url = new URL(detailsUrl)
        if (url.protocol === 'http:' || url.protocol === 'https:') {
          detailsLinkElement.href = detailsUrl
          detailsLinkElementContainer.classList.remove('hidden')
        } else {
          console.warn('Invalid URL protocol, skipping link:', detailsUrl)
        }
      } catch {
        console.warn('Invalid URL format, skipping link:', detailsUrl)
      }
    }
  }

  if (!name && !address && !detailsUrl) {
    return null
  }

  return popupContent
}

const addPopupsToMap = (
  mapId: string,
  map: MapLibreMap,
  settings: MapData['settings'],
): void => {
  map.on('click', LOCATIONS_LAYER, (e) => {
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

    const popupContent = createPopupContent(mapId, settings, properties)
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

    map.on('mouseenter', LOCATIONS_LAYER, (): void => {
      const canvas = map.getCanvas()
      if (canvas) {
        canvas.style.cursor = 'pointer'
      }
    })

    map.on('mouseleave', LOCATIONS_LAYER, (): void => {
      const canvas = map.getCanvas()
      if (canvas) {
        canvas.style.cursor = ''
      }
    })
  })

  return map
}
