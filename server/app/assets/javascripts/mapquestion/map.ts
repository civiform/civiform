import {LngLatLike, Map as MapLibreMap, Popup} from 'maplibre-gl'
import {GeoJsonProperties, Feature, FeatureCollection, Point} from 'geojson'
import {
  MapData,
  MapSettings,
  mapQuerySelector,
  CF_POPUP_CONTENT_TEMPLATE,
  CF_POPUP_CONTENT_LOCATION_NAME,
  CF_POPUP_CONTENT_LOCATION_ADDRESS,
  CF_POPUP_CONTENT_LOCATION_LINK_CONTAINER,
  CF_POPUP_CONTENT_LOCATION_LINK,
  LOCATIONS_SOURCE,
  LOCATIONS_LAYER,
  DEFAULT_MAP_CENTER_POINT,
  DEFAULT_MAP_ZOOM,
  DEFAULT_MAP_MARKER_TYPE,
  DEFAULT_MAP_MARKER_STYLE,
  DEFAULT_MAP_STYLE,
} from './map_util'

export const init = (): void => {
  const mapDataObject = window.app?.data?.maps || {}

  Object.entries(mapDataObject).forEach(([mapId, mapData]) => {
    try {
      renderMap(mapId, mapData as MapData)
    } catch (error) {
      console.warn(`Failed to render map ${mapId}:`, error)
    }
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
  geoJson: FeatureCollection,
): void => {
  // Preserve original IDs in properties because MapLibre only preserves properties when processing click events
  // Will need these later for filtering, selection, and submission
  const geoJsonWithOriginalIds = {
    ...geoJson,
    features: geoJson.features.map((feature) => ({
      ...feature,
      properties: {
        ...feature.properties,
        originalId: feature.id,
      },
    })),
  }

  map.addSource(LOCATIONS_SOURCE, {
    type: 'geojson',
    data: geoJsonWithOriginalIds,
  })

  // TODO(#11279): Add custom icons to the map markers
  map.addLayer({
    id: LOCATIONS_LAYER,
    type: DEFAULT_MAP_MARKER_TYPE,
    source: LOCATIONS_SOURCE,
    paint: DEFAULT_MAP_MARKER_STYLE,
  })
}

const createPopupContent = (
  popupContentTemplate: Node,
  settings: MapSettings,
  properties: GeoJsonProperties,
): Node | null => {
  if (!properties) return null
  const name: string = properties[settings.nameGeoJsonKey] as string
  const address: string = properties[settings.addressGeoJsonKey] as string
  const detailsUrl: string = properties[settings.detailsUrlGeoJsonKey] as string

  const popupContent = popupContentTemplate.cloneNode(true) as HTMLElement
  popupContent.classList.remove('hidden', CF_POPUP_CONTENT_TEMPLATE)

  if (name) {
    const nameElement =
      (popupContent.querySelector(
        `.${CF_POPUP_CONTENT_LOCATION_NAME}`,
      ) as HTMLElement) || null
    if (nameElement) {
      nameElement.textContent = name
      nameElement.classList.remove('hidden')
    }
  }

  if (address) {
    const addressElement =
      (popupContent.querySelector(
        `.${CF_POPUP_CONTENT_LOCATION_ADDRESS}`,
      ) as HTMLElement) || null
    if (addressElement) {
      addressElement.textContent = address
      addressElement.classList.remove('hidden')
    }
  }

  if (detailsUrl) {
    const detailsLinkElementContainer =
      (popupContent.querySelector(
        `.${CF_POPUP_CONTENT_LOCATION_LINK_CONTAINER}`,
      ) as HTMLElement) || null
    const detailsLinkElement =
      (popupContent.querySelector(
        `.${CF_POPUP_CONTENT_LOCATION_LINK}`,
      ) as HTMLAnchorElement) || null
    if (detailsLinkElementContainer && detailsLinkElement) {
      try {
        new URL(detailsUrl) // Validate URL format
        detailsLinkElement.href = detailsUrl
        detailsLinkElementContainer.classList.remove('hidden')
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
  settings: MapSettings,
): void => {
  map.on('click', LOCATIONS_LAYER, (e) => {
    const features: Feature[] | undefined = e.features
    if (!features || features.length === 0) {
      return
    }

    const geometry = features[0].geometry as Point
    const properties = features[0].properties

    if (!geometry || !properties) {
      return
    }

    const coordinates: LngLatLike = geometry.coordinates.slice() as LngLatLike

    const popup = new Popup({closeButton: false}).setLngLat(coordinates)

    const popupContentTemplate = mapQuerySelector(
      mapId,
      CF_POPUP_CONTENT_TEMPLATE,
    )
    if (!popupContentTemplate) {
      console.warn(`Map popup template not found for map: ${mapId}`)
      return null
    }

    const popupContent = createPopupContent(
      popupContentTemplate,
      settings,
      properties,
    )
    if (popupContent) {
      popup.setDOMContent(popupContent)
    }

    popup.addTo(map)
  })
}

const renderMap = (mapId: string, mapData: MapData): MapLibreMap => {
  if (!mapData.settings || !mapData.geoJson) {
    throw new Error(
      `Invalid map data for ${mapId}: missing settings or geoJson`,
    )
  }

  const settings: MapSettings = mapData.settings
  const geoJson: FeatureCollection = mapData.geoJson
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
