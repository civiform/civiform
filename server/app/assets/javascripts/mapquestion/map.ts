import {LngLatLike, Map as MapLibreMap, Popup, GeoJSONSource} from 'maplibre-gl'
import {GeoJsonProperties, Feature, FeatureCollection, Point} from 'geojson'
import {
  MapData,
  MapSettings,
  CF_POPUP_CONTENT_TEMPLATE,
  CF_POPUP_CONTENT_LOCATION_NAME,
  CF_POPUP_CONTENT_LOCATION_ADDRESS,
  CF_POPUP_CONTENT_LOCATION_LINK,
  DEFAULT_MAP_CENTER_POINT,
  DEFAULT_MAP_ZOOM,
  DEFAULT_MAP_STYLE,
  CF_POPUP_CONTENT_BUTTON,
  CF_SELECTED_LOCATIONS_CONTAINER,
  CF_LOCATIONS_LIST_CONTAINER,
  DATA_FEATURE_ID_ATTR,
  DATA_MAP_ID_ATTR,
  MapMessages,
  LOCATIONS_SOURCE,
  LOCATIONS_LAYER,
  DEFAULT_LOCATION_ICON,
  SELECTED_LOCATION_ICON,
  DEFAULT_ICON_IMAGE_SOURCE,
  SELECTED_ICON_IMAGE_SOURCE,
  mapQuerySelector,
  CF_MAP_MARKER_ICON_TEMPLATE,
  CF_MAP_MARKER_ICON_SELECTED_TEMPLATE,
} from './map_util'
import {
  initLocationSelection,
  selectLocationsFromMap,
  updateSelectedLocations,
} from './map_question_selection'
import {initFilters} from './map_question_filters'
import {
  CF_MAP_QUESTION_PAGINATION_BUTTON,
  CF_MAP_QUESTION_PAGINATION_NEXT_BUTTON,
  CF_MAP_QUESTION_PAGINATION_PREVIOUS_BUTTON,
  DATA_PAGE_ATTRIBUTE,
  getPaginationNavComponent,
  getPaginationState,
  goToPage,
  initPagination,
} from './map_question_pagination'

export const init = (): void => {
  const mapMessages = window.app?.data?.messages as MapMessages
  const mapDataObject = window.app?.data?.maps || {}

  Object.entries(mapDataObject).forEach(([mapId, mapData]) => {
    try {
      const mapElement = renderMap(mapId, mapData as MapData, mapMessages)
      initLocationSelection(mapId, mapMessages)
      initFilters(mapId, mapElement, mapMessages, mapData as MapData)
      initPagination(mapId)
      setupEventListenersForMap(mapId, mapElement, mapMessages)
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
  mapId: string,
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
        selected: false, // Track selection state in feature properties
      },
    })),
  }

  const defaultIconTemplate = mapQuerySelector(
    mapId,
    CF_MAP_MARKER_ICON_TEMPLATE,
  ) as HTMLTemplateElement
  const selectedIconTemplate = mapQuerySelector(
    mapId,
    CF_MAP_MARKER_ICON_SELECTED_TEMPLATE,
  ) as HTMLTemplateElement

  const defaultIconSrc =
    (defaultIconTemplate?.content.querySelector('img') as HTMLImageElement)
      ?.src || DEFAULT_ICON_IMAGE_SOURCE
  const selectedIconSrc =
    (selectedIconTemplate?.content.querySelector('img') as HTMLImageElement)
      ?.src || SELECTED_ICON_IMAGE_SOURCE

  Promise.all([map.loadImage(defaultIconSrc), map.loadImage(selectedIconSrc)])
    .then(([defaultImage, selectedImage]) => {
      map.addImage(DEFAULT_LOCATION_ICON, defaultImage.data)
      map.addImage(SELECTED_LOCATION_ICON, selectedImage.data)

      map.addSource(LOCATIONS_SOURCE, {
        type: 'geojson',
        data: geoJsonWithOriginalIds,
      })

      map.addLayer({
        id: LOCATIONS_LAYER,
        type: 'symbol',
        source: LOCATIONS_SOURCE,
        layout: {
          'icon-image': [
            'case',
            ['get', 'selected'],
            SELECTED_LOCATION_ICON,
            DEFAULT_LOCATION_ICON,
          ],
          'icon-size': 1,
          'icon-allow-overlap': true,
        },
      })
    })
    .catch((error) => {
      console.error(`Error loading marker icons for map ${mapId}:`, error)
    })
}

const createPopupContent = (
  mapId: string,
  templateContent: HTMLCollection,
  settings: MapSettings,
  properties: GeoJsonProperties,
): Node | null => {
  if (!properties) return null
  const name: string = properties[settings.nameGeoJsonKey] as string
  const address: string = properties[settings.addressGeoJsonKey] as string
  const detailsUrl: string = properties[settings.detailsUrlGeoJsonKey] as string
  const featureId: string = properties.originalId as string

  if (!featureId) {
    return null
  }

  if (!name && !address && !detailsUrl) {
    return null
  }

  const popupContent = document.createElement('div')
  popupContent.classList.add('flex', 'flex-column', 'padding-4')
  if (name) {
    const nameElement = templateContent
      .namedItem(CF_POPUP_CONTENT_LOCATION_NAME)
      ?.cloneNode(true) as HTMLElement
    nameElement.textContent = name
    popupContent.appendChild(nameElement)
  }

  if (address) {
    const addressElement = templateContent
      .namedItem(CF_POPUP_CONTENT_LOCATION_ADDRESS)
      ?.cloneNode(true) as HTMLElement
    addressElement.textContent = address
    popupContent.appendChild(addressElement)
  }

  if (detailsUrl) {
    try {
      new URL(detailsUrl) // Validate URL format
      const linkElement = templateContent
        .namedItem(CF_POPUP_CONTENT_LOCATION_LINK)
        ?.cloneNode(true) as HTMLAnchorElement
      linkElement.href = detailsUrl
      popupContent.appendChild(linkElement)
    } catch {
      console.warn('Invalid URL format, skipping link:', detailsUrl)
    }
  }

  const buttonElement = templateContent
    .namedItem(CF_POPUP_CONTENT_BUTTON)
    ?.cloneNode(true) as HTMLButtonElement
  if (buttonElement) {
    buttonElement.setAttribute('data-feature-id', featureId)
    buttonElement.setAttribute(DATA_MAP_ID_ATTR, mapId)
    popupContent.appendChild(buttonElement)
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

    const popup = new Popup().setLngLat(coordinates)

    const popupContentTemplate = mapQuerySelector(
      mapId,
      CF_POPUP_CONTENT_TEMPLATE,
    ) as HTMLTemplateElement
    if (!popupContentTemplate) {
      console.warn(`Map popup template not found for map: ${mapId}`)
      return null
    }

    const popupContent = createPopupContent(
      mapId,
      popupContentTemplate.content.children,
      settings,
      properties,
    )
    if (popupContent) {
      popup.setDOMContent(popupContent)
    }

    popup.addTo(map)
  })
}

const renderMap = (
  mapId: string,
  mapData: MapData,
  messages: MapMessages,
): MapLibreMap => {
  if (!mapData.settings || !mapData.geoJson) {
    throw new Error(
      `Invalid map data for ${mapId}: missing settings or geoJson`,
    )
  }

  const settings: MapSettings = mapData.settings
  const geoJson: FeatureCollection = mapData.geoJson
  const map = createMap(mapId)

  const canvas: HTMLCanvasElement = map.getCanvas()
  canvas.setAttribute('aria-label', messages.mapRegionAltText)

  // Add focus styles to the map container when the canvas is focused
  const mapContainer = document.getElementById(mapId)
  if (mapContainer) {
    canvas.addEventListener('focus', () => {
      mapContainer.classList.add('usa-focus')
    })

    canvas.addEventListener('blur', () => {
      mapContainer.classList.remove('usa-focus')
    })
  }

  map.on('load', () => {
    addLocationsToMap(mapId, map, geoJson)
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

const setupEventListenersForMap = (
  mapId: string,
  mapElement: MapLibreMap,
  messages: MapMessages,
): void => {
  const mapContainer = document.getElementById(mapId)
  if (!mapContainer) return

  mapContainer.addEventListener('click', (e) => {
    const target = e.target as HTMLButtonElement
    if (!target) return

    const targetName = target.getAttribute('name')
    if (!targetName) return

    const mapId = target.getAttribute(DATA_MAP_ID_ATTR)
    if (!mapId) return

    switch (targetName) {
      case CF_POPUP_CONTENT_BUTTON: {
          const featureId = target.getAttribute(DATA_FEATURE_ID_ATTR)
          if (featureId) {
            selectLocationsFromMap(featureId, mapId, messages)
            updateSelectedMarker(mapElement, featureId, true)
          }
        }
      case CF_MAP_QUESTION_PAGINATION_BUTTON:
      case CF_MAP_QUESTION_PAGINATION_PREVIOUS_BUTTON:
      case CF_MAP_QUESTION_PAGINATION_NEXT_BUTTON: {
        e.preventDefault()
        let page: number
        if (targetName === CF_MAP_QUESTION_PAGINATION_BUTTON) {
          page = parseInt(target.getAttribute(DATA_PAGE_ATTRIBUTE) || '1', 10)
        } else {
          const paginationNav = getPaginationNavComponent(mapId)
          if (!paginationNav) return
          const state = getPaginationState(mapId, paginationNav)
          page =
            targetName === CF_MAP_QUESTION_PAGINATION_PREVIOUS_BUTTON
              ? state.currentPage - 1
              : state.currentPage + 1
        }
        goToPage(mapId, page)
        return
      }
    }
  })

  const locationsListContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  )
  const selectedLocationsContainer = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATIONS_CONTAINER,
  )

  // Change handler for location checkboxes in the locations list
  if (locationsListContainer) {
    locationsListContainer.addEventListener('change', (e) => {
      const target = e.target as HTMLInputElement
      if (target == null || target.type !== 'checkbox') return

      const featureId = target.getAttribute(DATA_FEATURE_ID_ATTR)
      if (featureId) {
        updateSelectedMarker(mapElement, featureId, target.checked)
      }

      updateSelectedLocations(mapId, messages)
    })
  }

  // Change handler for checkboxes in the selected locations list
  if (selectedLocationsContainer) {
    selectedLocationsContainer.addEventListener('change', (e) => {
      const target = e.target as HTMLInputElement
      if (target == null || target.type !== 'checkbox') return

      const featureId = target.getAttribute(DATA_FEATURE_ID_ATTR)
      if (!target.checked && featureId) {
        updateSelectedMarker(mapElement, featureId, false)

        // Find and uncheck the original checkbox
        const originalCheckbox = locationsListContainer?.querySelector(
          `[${DATA_FEATURE_ID_ATTR}="${featureId}"] input[type="checkbox"]`,
        ) as HTMLInputElement
        if (originalCheckbox && originalCheckbox.type === 'checkbox') {
          originalCheckbox.checked = false
        }
      }

      updateSelectedLocations(mapId, messages)
    })
  }
}

export const updateSelectedMarker = (
  mapElement: MapLibreMap,
  featureId: string,
  selected: boolean,
) => {
  const source = mapElement.getSource(LOCATIONS_SOURCE) as GeoJSONSource

  const data = source._data as FeatureCollection
  const updatedFeatures = data.features.map((feature) => {
    if (feature.properties?.originalId === featureId) {
      return {
        ...feature,
        properties: {
          ...feature.properties,
          selected: selected,
        },
      }
    }
    return feature
  })

  source.setData({
    ...data,
    features: updatedFeatures,
  })
}
