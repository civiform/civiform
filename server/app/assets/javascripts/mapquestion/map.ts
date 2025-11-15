import {Feature, FeatureCollection, GeoJsonProperties, Point} from 'geojson'
import {GeoJSONSource, LngLatLike, Map as MapLibreMap, Popup} from 'maplibre-gl'
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
import {
  initLocationSelection,
  selectLocationsFromMap,
  updateSelectedLocations,
} from './map_question_selection'
import {
  CF_LOCATIONS_LIST_CONTAINER,
  CF_POPUP_CONTENT_BUTTON,
  CF_POPUP_CONTENT_LOCATION_ADDRESS,
  CF_POPUP_CONTENT_LOCATION_LINK,
  CF_POPUP_CONTENT_LOCATION_NAME,
  CF_POPUP_CONTENT_TAG,
  CF_POPUP_CONTENT_TEMPLATE,
  CF_SELECTED_LOCATIONS_CONTAINER,
  CF_SELECT_LOCATION_BUTTON_CLICKED,
  CF_TOGGLE_HIDDEN,
  DATA_FEATURE_ID,
  DATA_MAP_ID,
  DEFAULT_LOCATION_ICON,
  DEFAULT_MAP_CENTER_POINT,
  DEFAULT_MAP_MARKER_TYPE,
  DEFAULT_MAP_STYLE,
  DEFAULT_MAP_ZOOM,
  getMessages,
  localizeString,
  LOCATIONS_LAYER,
  LOCATIONS_SOURCE,
  MapData,
  MapSettings,
  mapQuerySelector,
  SELECTED_LOCATION_ICON,
  CF_LOCATION_COUNT,
  CF_SWITCH_TO_LIST_VIEW_BUTTON,
  CF_SWITCH_TO_MAP_VIEW_BUTTON,
  hasReachedMaxSelections,
  calculateMapCenter,
  POPUP_LAYER,
  FOCUS_LAYER,
  FOCUS_SOURCE,
} from './map_util'

export const init = (): void => {
  const mapDataObject = window.app?.data?.maps || {}

  Object.entries(mapDataObject).forEach(([mapId, mapData]) => {
    try {
      const mapElement = renderMap(mapId, mapData as MapData)
      initLocationSelection(mapId)
      initFilters(mapId, mapElement, mapData as MapData)
      initPagination(mapId)
      setupEventListenersForMap(mapId, mapElement)
    } catch (error) {
      console.warn(`Failed to render map ${mapId}:`, error)
    }
  })
}

const createMap = (mapId: string, geoJson: FeatureCollection) => {
  const calculatedCenter = calculateMapCenter(geoJson)
  const center = calculatedCenter || DEFAULT_MAP_CENTER_POINT

  return new MapLibreMap({
    container: mapId,
    style: DEFAULT_MAP_STYLE,
    center: center,
    zoom: DEFAULT_MAP_ZOOM,
  })
}

const addLocationsToMap = (
  mapId: string,
  map: MapLibreMap,
  geoJson: FeatureCollection,
): void => {
  // Convert 'selected' property from string 'true'/'false' to boolean
  const geoJsonWithSelectedBooleans = {
    ...geoJson,
    features: geoJson.features.map((feature) => ({
      ...feature,
      properties: {
        ...feature.properties,
        selected: feature.properties?.selected === 'true',
      },
    })),
  }

  Promise.all([
    map.loadImage(window.app?.data?.iconUrls?.locationIcon),
    map.loadImage(window.app?.data?.iconUrls?.selectedLocationIcon),
  ])
    .then(([defaultImage, selectedImage]) => {
      map.addImage(DEFAULT_LOCATION_ICON, defaultImage.data)
      map.addImage(SELECTED_LOCATION_ICON, selectedImage.data)

      map.addSource(LOCATIONS_SOURCE, {
        type: 'geojson',
        data: geoJsonWithSelectedBooleans,
      })

      map.addLayer({
        id: LOCATIONS_LAYER,
        type: DEFAULT_MAP_MARKER_TYPE,
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
  const tagKey: string = settings.tagGeoJsonKey as string
  const tagValue: string = settings.tagGeoJsonValue as string
  const featureId: string = properties.originalId as string

  if (!featureId) {
    return null
  }

  if (!name && !address && !detailsUrl) {
    return null
  }

  const popupContent = document.createElement('div')
  popupContent.classList.add('flex', 'flex-column', 'padding-4', POPUP_LAYER)
  popupContent.setAttribute('data-map-id', mapId)
  popupContent.setAttribute('data-feature-id', featureId)
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
      linkElement.ariaLabel = localizeString(getMessages().locationLinkTextSr, [
        name,
      ])
      popupContent.appendChild(linkElement)
    } catch {
      console.warn('Invalid URL format, skipping link:', detailsUrl)
    }
  }

  if (tagKey && tagValue && properties[tagKey] === tagValue) {
    const tagElement = templateContent
      .namedItem(CF_POPUP_CONTENT_TAG)
      ?.cloneNode(true) as HTMLElement
    if (tagElement) {
      popupContent.appendChild(tagElement)
    }
  }

  const buttonElement = templateContent
    .namedItem(CF_POPUP_CONTENT_BUTTON)
    ?.cloneNode(true) as HTMLButtonElement
  if (buttonElement) {
    if (properties.selected) {
      buttonElement.classList.add(CF_SELECT_LOCATION_BUTTON_CLICKED)
      buttonElement.textContent = localizeString(
        getMessages().mapSelectedButtonText,
      )
    }
    buttonElement.setAttribute('data-feature-id', featureId)
    buttonElement.setAttribute(DATA_MAP_ID, mapId)

    if (hasReachedMaxSelections(mapId) && !properties.selected) {
      buttonElement.disabled = true
    }

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

const initKeyboardNavigation = (
  mapId: string,
  map: MapLibreMap,
  settings: MapSettings,
): void => {
  let focusedFeatureIndex = -1
  let iconNavigationMode = false
  const canvas = map.getCanvas()

  // Create ARIA live region for mode announcements
  const mapContainer = document.getElementById(mapId)
  const liveRegion = document.createElement('div')
  liveRegion.setAttribute('aria-live', 'polite')
  liveRegion.setAttribute('aria-atomic', 'true')
  liveRegion.setAttribute('class', 'usa-sr-only')
  liveRegion.setAttribute('data-map-id', mapId)
  liveRegion.setAttribute('data-navigation-status', '')
  if (mapContainer) {
    mapContainer.appendChild(liveRegion)
  }

  const announceMode = (message: string): void => {
    liveRegion.textContent = message
  }

  // Add focus indicator layer
  map.addSource(FOCUS_SOURCE, {
    type: 'geojson',
    data: {
      type: 'FeatureCollection',
      features: [],
    },
  })

  map.addLayer({
    id: FOCUS_LAYER,
    type: 'circle',
    source: FOCUS_SOURCE,
    paint: {
      'circle-radius': 20,
      'circle-color': 'transparent',
      'circle-stroke-width': 3,
      'circle-stroke-color': '#005ea2',
    },
  })

  const getVisibleFeatures = (): Feature[] => {
    const source = map.getSource(LOCATIONS_SOURCE) as GeoJSONSource
    if (!source) return []

    const allFeatures = map.querySourceFeatures(LOCATIONS_SOURCE)
    return allFeatures.filter((feature) => feature.geometry.type === 'Point')
  }

  const updateFocusIndicator = (feature: Feature | null): void => {
    const focusSource = map.getSource(FOCUS_SOURCE) as GeoJSONSource
    if (!focusSource) return

    if (feature && feature.geometry.type === 'Point') {
      focusSource.setData({
        type: 'FeatureCollection',
        features: [feature],
      })

      // Pan to the focused feature
      const coords = feature.geometry.coordinates as [number, number]
      map.easeTo({
        center: coords,
        duration: 300,
      })
    } else {
      focusSource.setData({
        type: 'FeatureCollection',
        features: [],
      })
    }
  }

  const openPopupForFeature = (feature: Feature): void => {
    if (feature.geometry.type !== 'Point') return

    const geometry = feature.geometry
    const properties = feature.properties
    const coordinates: LngLatLike = geometry.coordinates.slice() as LngLatLike

    const popup = new Popup().setLngLat(coordinates)

    const popupContentTemplate = mapQuerySelector(
      mapId,
      CF_POPUP_CONTENT_TEMPLATE,
    ) as HTMLTemplateElement
    if (!popupContentTemplate) {
      console.warn(`Map popup template not found for map: ${mapId}`)
      return
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
  }

  const enterIconNavigationMode = (): void => {
    const features = getVisibleFeatures()
    if (features.length === 0) {
      announceMode('No locations available to navigate.')
      return
    }

    iconNavigationMode = true
    focusedFeatureIndex = 0
    updateFocusIndicator(features[focusedFeatureIndex])

    // Disable map's default keyboard navigation
    map.keyboard.disable()

    announceMode(
      `Icon navigation mode activated. Use arrow keys to navigate between ${features.length} locations. Press Space or Enter to open details, Escape to exit.`,
    )
  }

  const exitIconNavigationMode = (): void => {
    iconNavigationMode = false
    focusedFeatureIndex = -1
    updateFocusIndicator(null)

    // Re-enable map's default keyboard navigation
    map.keyboard.enable()

    announceMode(
      'Icon navigation mode deactivated. Use arrow keys to pan the map.',
    )
  }

  canvas.addEventListener('keydown', (e: KeyboardEvent) => {
    const features = getVisibleFeatures()

    // If not in icon navigation mode, Enter activates it
    if (!iconNavigationMode) {
      if (e.key === 'Enter') {
        e.preventDefault()
        enterIconNavigationMode()
      }
      // Let other keys pass through for normal map navigation
      return
    }

    // In icon navigation mode, handle arrow keys and other controls
    if (features.length === 0) return

    switch (e.key) {
      case 'ArrowDown':
      case 'ArrowRight':
        e.preventDefault()
        focusedFeatureIndex = (focusedFeatureIndex + 1) % features.length
        updateFocusIndicator(features[focusedFeatureIndex])
        break

      case 'ArrowUp':
      case 'ArrowLeft':
        e.preventDefault()
        focusedFeatureIndex =
          focusedFeatureIndex <= 0
            ? features.length - 1
            : focusedFeatureIndex - 1
        updateFocusIndicator(features[focusedFeatureIndex])
        break

      case 'Enter':
      case ' ':
        e.preventDefault()
        if (focusedFeatureIndex >= 0 && focusedFeatureIndex < features.length) {
          openPopupForFeature(features[focusedFeatureIndex])
        }
        break

      case 'Escape':
        e.preventDefault()
        exitIconNavigationMode()
        break
    }
  })

  // Clear focus and exit mode when canvas loses focus
  canvas.addEventListener('blur', () => {
    if (iconNavigationMode) {
      exitIconNavigationMode()
    }
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
  const map = createMap(mapId, geoJson)

  const canvas: HTMLCanvasElement = map.getCanvas()
  const ariaLabel = `${getMessages().mapRegionAltText}. Press Enter to navigate between location icons. Use arrow keys to pan the map.`
  canvas.setAttribute('aria-label', ariaLabel)

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
    initKeyboardNavigation(mapId, map, settings)

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
): void => {
  const mapContainer = document.getElementById(mapId)
  if (!mapContainer) return

  mapContainer.addEventListener('click', (e) => {
    const target = e.target as HTMLButtonElement
    if (!target) return

    const targetName = target.getAttribute('name')
    if (!targetName) return

    const featureId = target.getAttribute(DATA_FEATURE_ID)
    if (!featureId) return

    if (targetName === CF_POPUP_CONTENT_BUTTON && featureId) {
      const isSelected = !target.classList.contains(
        CF_SELECT_LOCATION_BUTTON_CLICKED,
      )
      selectLocationsFromMap(featureId, mapId, isSelected)
      updateSelectedMarker(mapElement, featureId, isSelected)
      updatePopupButtonState(mapId, featureId, isSelected)
    }
  })

  const paginationNav = getPaginationNavComponent(mapId)
  if (!paginationNav) return
  paginationNav.addEventListener('click', (e) => {
    const target = e.target as HTMLButtonElement
    const targetName = target?.getAttribute('name')

    const paginationButtons = [
      CF_MAP_QUESTION_PAGINATION_BUTTON,
      CF_MAP_QUESTION_PAGINATION_PREVIOUS_BUTTON,
      CF_MAP_QUESTION_PAGINATION_NEXT_BUTTON,
    ]

    if (targetName && paginationButtons.includes(targetName)) {
      e.preventDefault()

      const page =
        targetName === CF_MAP_QUESTION_PAGINATION_BUTTON
          ? parseInt(target.getAttribute(DATA_PAGE_ATTRIBUTE) || '1', 10)
          : getPaginationState(mapId, paginationNav).currentPage +
            (targetName === CF_MAP_QUESTION_PAGINATION_NEXT_BUTTON ? 1 : -1)

      goToPage(mapId, page)
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

      const featureId = target.getAttribute(DATA_FEATURE_ID)
      if (featureId) {
        updateSelectedMarker(mapElement, featureId, target.checked)
        updatePopupButtonState(mapId, featureId, target.checked)
      }

      updateSelectedLocations(mapId)
      updateOpenPopupButtons(mapId)
    })
  }

  // Change handler for checkboxes in the selected locations list
  if (selectedLocationsContainer) {
    selectedLocationsContainer.addEventListener('change', (e) => {
      const target = e.target as HTMLInputElement
      if (target == null || target.type !== 'checkbox') return

      const featureId = target.getAttribute(DATA_FEATURE_ID)
      if (!target.checked && featureId) {
        updateSelectedMarker(mapElement, featureId, false)
        updatePopupButtonState(mapId, featureId, target.checked)

        // Find and uncheck the original checkbox
        const originalCheckbox = locationsListContainer?.querySelector(
          `[${DATA_FEATURE_ID}="${featureId}"] input[type="checkbox"]`,
        ) as HTMLInputElement
        if (originalCheckbox && originalCheckbox.type === 'checkbox') {
          originalCheckbox.checked = false
        }
      }

      updateSelectedLocations(mapId)
      updateOpenPopupButtons(mapId)
    })
  }

  const locationCount = mapQuerySelector(mapId, CF_LOCATION_COUNT)
  const switchToListButton = mapQuerySelector(
    mapId,
    CF_SWITCH_TO_LIST_VIEW_BUTTON,
  ) as HTMLButtonElement
  const switchToMapButton = mapQuerySelector(
    mapId,
    CF_SWITCH_TO_MAP_VIEW_BUTTON,
  ) as HTMLButtonElement

  if (!locationsListContainer || !paginationNav || !locationCount) {
    return
  }

  if (switchToListButton) {
    switchToListButton.addEventListener('click', () => {
      // Switch to list view
      switchToListButton.classList.add(CF_TOGGLE_HIDDEN)
      switchToMapButton?.classList.remove(CF_TOGGLE_HIDDEN)
      mapContainer.classList.add(CF_TOGGLE_HIDDEN)
      locationsListContainer.classList.remove(CF_TOGGLE_HIDDEN)
      paginationNav.classList.remove(CF_TOGGLE_HIDDEN)
      locationCount.classList.remove(CF_TOGGLE_HIDDEN)
      updateViewStatus(mapId)
    })
  }

  if (switchToMapButton) {
    switchToMapButton.addEventListener('click', () => {
      // Switch to map view
      switchToMapButton.classList.add(CF_TOGGLE_HIDDEN)
      switchToListButton?.classList.remove(CF_TOGGLE_HIDDEN)
      mapContainer.classList.remove(CF_TOGGLE_HIDDEN)
      locationsListContainer.classList.add(CF_TOGGLE_HIDDEN)
      paginationNav.classList.add(CF_TOGGLE_HIDDEN)
      locationCount.classList.add(CF_TOGGLE_HIDDEN)
      updateViewStatus(mapId, true)
    })
  }
}

export const updateSelectedMarker = (
  mapElement: MapLibreMap,
  featureId: string,
  isSelected: boolean,
) => {
  const source = mapElement.getSource(LOCATIONS_SOURCE) as GeoJSONSource

  void source.getData().then((data) => {
    const featureCollection = data as FeatureCollection
    const updatedFeatures = featureCollection.features.map(
      (feature: Feature) => {
        if (feature.properties?.originalId === featureId) {
          return {
            ...feature,
            properties: {
              ...feature.properties,
              selected: isSelected,
            },
          }
        }
        return feature
      },
    )

    source.setData({
      ...featureCollection,
      features: updatedFeatures,
    })
  })
}

const updatePopupButtonState = (
  mapId: string,
  featureId: string,
  isSelected: boolean,
): void => {
  const mapContainer = document.getElementById(mapId)
  if (!mapContainer) return

  const popupButton = mapContainer.querySelector(
    `[data-map-id="${mapId}"][data-feature-id="${featureId}"][name=${CF_POPUP_CONTENT_BUTTON}]`,
  ) as HTMLButtonElement
  if (!popupButton) return

  if (isSelected) {
    popupButton.classList.add(CF_SELECT_LOCATION_BUTTON_CLICKED)
    popupButton.textContent = localizeString(
      getMessages().mapSelectedButtonText,
    )
    popupButton.disabled = false
  } else {
    popupButton.classList.remove(CF_SELECT_LOCATION_BUTTON_CLICKED)
    popupButton.textContent = getMessages().mapSelectLocationButtonText
    popupButton.disabled = hasReachedMaxSelections(mapId)
  }
}

const updateOpenPopupButtons = (mapId: string): void => {
  // Find open popup button (there will be 0 or 1)
  const popupButton = mapQuerySelector(
    mapId,
    CF_POPUP_CONTENT_BUTTON,
  ) as HTMLButtonElement
  if (!popupButton) {
    return
  }

  const maxReached = hasReachedMaxSelections(mapId)

  const isSelectedButton = popupButton.classList.contains(
    CF_SELECT_LOCATION_BUTTON_CLICKED,
  )

  popupButton.disabled = !isSelectedButton && maxReached
}

const updateViewStatus = (mapId: string, toMapView: boolean = false): void => {
  const statusElement = document.querySelector(
    `[data-map-id=${mapId}][data-switch-view-status]`,
  ) as HTMLElement
  if (statusElement) {
    statusElement.textContent = toMapView
      ? localizeString(getMessages().switchToMapViewSr)
      : localizeString(getMessages().switchToListViewSr)
    // Clear the text after announcement to prevent navigation to it
    setTimeout(() => {
      statusElement.textContent = ''
    }, 1000)
  }
}
