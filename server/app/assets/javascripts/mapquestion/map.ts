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
  CF_LOCATION_CHECKBOX,
  hasReachedMaxSelections,
  calculateMapCenter,
  POPUP_LAYER,
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
  popupContent.classList.add(
    'display-flex',
    'flex-column',
    'padding-4',
    POPUP_LAYER,
  )
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
  canvas.setAttribute('aria-label', getMessages().mapRegionAltText)

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

      // Focus on the location count element after pagination
      const locationCount = mapQuerySelector(
        mapId,
        CF_LOCATION_COUNT,
      ) as HTMLElement
      if (locationCount) {
        locationCount.focus()
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
  ) as HTMLElement

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

      let nextSelectedLocationFeatureID = null

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
        // move focus to a relevant location
        const childDivs = selectedLocationsContainer.querySelectorAll(
          '.' + CF_LOCATION_CHECKBOX,
        )
        const elementsArray = Array.from(childDivs)

        // Find the index of element being removed
        const index = elementsArray.indexOf(target.parentElement as Element)

        // if there is a next location, focus on it
        if (index + 1 < childDivs.length) {
          const nextElement = elementsArray[index + 1] as HTMLElement
          nextSelectedLocationFeatureID =
            nextElement.getAttribute(DATA_FEATURE_ID)
        } else if (index - 1 >= 0) {
          // else if there is a previous location, focus on it
          const previousElement = elementsArray[index - 1] as HTMLElement
          nextSelectedLocationFeatureID =
            previousElement.getAttribute(DATA_FEATURE_ID)
        } else {
          // no remaining selected locations so we should focus on container as a whole
          selectedLocationsContainer.focus()
        }
      }

      updateSelectedLocations(mapId)
      updateOpenPopupButtons(mapId)
      if (nextSelectedLocationFeatureID) {
        const nextSelectedLocation = selectedLocationsContainer?.querySelector(
          `[${DATA_FEATURE_ID}="${nextSelectedLocationFeatureID}"] input[type="checkbox"]`,
        ) as HTMLElement
        nextSelectedLocation.focus()
      }
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
