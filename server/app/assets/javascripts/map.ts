import {
  FilterSpecification,
  LngLatLike,
  Map as MapLibreMap,
  MapGeoJSONFeature,
  Popup,
} from 'maplibre-gl'

import {
  buildMapFilterExpression,
  getFilterSelectsForMap,
  getMapSelector,
  CF_APPLY_FILTERS_BTN,
  CF_LOCATION_COUNT,
  CF_LOCATIONS_LIST_CONTAINER,
  CF_NO_SELECTIONS_MESSAGE,
  CF_RESET_FILTERS_BTN,
  CF_SELECTED_LOCATIONS_LIST,
  CF_SELECT_LOCATION_BUTTON,
  MapData,
  createPopupContent
} from './map_util'

const mapInstances = new Map<string, MapLibreMap>()
export const init = () => {
  console.log('Map init called')
  const mapDataObject = window.app?.data?.maps || {}
  console.log('Map data object:', mapDataObject)

  Object.entries(mapDataObject).forEach(([mapId, mapData]) => {
    console.log('Processing map:', mapId)
    const map = renderMap(mapId, mapData as MapData)
    mapInstances.set(mapId, map)
    initLocationSelection(mapId)
    initFilters(mapId)
  })
}

const renderMap = (mapId: string, mapData: MapData): MapLibreMap => {
  console.log('renderMap called with mapId:', mapId)
  const settings = mapData.settings || {}
  const geoJson = mapData.geoJson || {}

  console.log('Creating MapLibre map with container:', mapId)
  const mapContainer = document.getElementById(mapId)
  console.log('Map container element:', mapContainer)
  
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
  
  console.log('MapLibre map created:', map)

  map.on('load', () => {
    console.log('Map loaded successfully')
    console.log('Map container dimensions:', {
      width: mapContainer?.offsetWidth,
      height: mapContainer?.offsetHeight,
      clientWidth: mapContainer?.clientWidth,
      clientHeight: mapContainer?.clientHeight
    })
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

const initLocationSelection = (mapId: string) => {
  console.log('initLocationSelection called with mapId:', mapId)
  
  const selector = getMapSelector(mapId, CF_LOCATIONS_LIST_CONTAINER)
  console.log('Looking for selector:', selector)
  
  const locationsListContainer = document.querySelector(selector)
  console.log('Found container:', locationsListContainer)
  
  if (!locationsListContainer) return

  const checkboxes = locationsListContainer.querySelectorAll(
    'input[type="checkbox"]',
  )
  checkboxes.forEach((checkbox) => {
    checkbox.addEventListener('change', () =>
      updateSelectedLocationsForMap(mapId)
    )
  })

  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement
    if (target.classList.contains(CF_SELECT_LOCATION_BUTTON)) {
      const locationName = target.getAttribute('data-location-name')
      if (locationName) {
        selectLocationFromMap(locationName, mapId)
        const popups = document.querySelectorAll('.maplibregl-popup')
        popups.forEach((popup) => popup.remove())
      }
    }
  })
}

const handleLocationSelectionForMap = (event: Event, mapId: string) => {
}

const updateSelectedLocationsForMap = (mapId: string) => {
  const mapLocationsContainer = document.querySelector(
    getMapSelector(mapId, CF_LOCATIONS_LIST_CONTAINER),
  )
  if (!mapLocationsContainer) return

  const selectedCheckboxes = mapLocationsContainer.querySelectorAll(
    'input[type="checkbox"]:checked',
  )
  const noSelectionsMessage = document.querySelector(
    getMapSelector(mapId, CF_NO_SELECTIONS_MESSAGE),
  ) as HTMLElement
  const selectedLocationsList = document.querySelector(
    getMapSelector(mapId, CF_SELECTED_LOCATIONS_LIST),
  ) as HTMLElement

  if (!noSelectionsMessage || !selectedLocationsList) return

  if (selectedCheckboxes.length === 0) {
    // Show "no selections" message
    noSelectionsMessage.classList.remove('display-none')
    selectedLocationsList.classList.add('display-none')
  } else {
    // Hide "no selections" message and show selected locations
    noSelectionsMessage.classList.add('display-none')
    selectedLocationsList.classList.remove('display-none')

    // Clear and rebuild the selected locations list
    selectedLocationsList.innerHTML = ''

    selectedCheckboxes.forEach((checkbox) => {
      const locationLabel = mapLocationsContainer.querySelector(
        `label[for="${checkbox.id}"]`,
      )
      if (locationLabel) {
        const selectedItem = document.createElement('div')
        selectedItem.className = 'usa-checkbox'

        const selectedCheckboxId = `selected-${checkbox.id}`
        const selectedCheckbox = document.createElement('input')
        selectedCheckbox.type = 'checkbox'
        selectedCheckbox.checked = true
        selectedCheckbox.id = selectedCheckboxId
        selectedCheckbox.className = 'usa-checkbox__input'
        selectedCheckbox.addEventListener('change', (e) => {
          // Uncheck the original checkbox when this one is unchecked
          const originalCheckbox = document.getElementById(
            checkbox.id,
          ) as HTMLInputElement
          if (originalCheckbox) {
            originalCheckbox.checked = false
            // Use setTimeout to avoid immediate re-execution during the same event loop
            setTimeout(() => updateSelectedLocationsForMap(mapId), 0)
          }
        })

        const selectedLabel = document.createElement('label')
        selectedLabel.htmlFor = selectedCheckboxId
        selectedLabel.className = 'usa-checkbox__label'
        selectedLabel.innerHTML = locationLabel.innerHTML

        selectedItem.appendChild(selectedCheckbox)
        selectedItem.appendChild(selectedLabel)
        selectedLocationsList.appendChild(selectedItem)
      }
    })
  }
}

const selectLocationFromMap = (locationName: string, mapId: string) => {
  const mapLocationsContainer = document.querySelector(
    getMapSelector(mapId, CF_LOCATIONS_LIST_CONTAINER),
  )
  if (!mapLocationsContainer) return

  // Find the checkbox with the matching location name within this map's container
  const checkboxes = mapLocationsContainer.querySelectorAll(
    'input[type="checkbox"]',
  )
  checkboxes.forEach((checkbox) => {
    const checkboxValue = checkbox.getAttribute('value')
    if (checkboxValue === locationName) {
      ;(checkbox as HTMLInputElement).checked = true
      // Trigger the change event to update the selected locations
      checkbox.dispatchEvent(new Event('change'))
    }
  })
}

const featureMatchesFilters = (
  feature: any,
  filters: {[key: string]: string},
): boolean => {
  if (!feature?.properties || Object.keys(filters).length === 0) return true
  return Object.entries(filters).every(
    ([key, value]) => feature.properties[key] === value,
  )
}

const applyLocationFiltersForMap = (
  mapId: string,
  filterOverride?: {[key: string]: string},
) => {
  const filters = filterOverride ?? getFilterValuesForMap(mapId)

  // Apply to specific map
  const map = mapInstances.get(mapId)
  if (map) {
    map.setFilter('locations-layer', buildMapFilterExpression(filters))
  }

  // Apply to checkboxes within this map's container
  const mapData = window.app?.data?.maps?.[mapId] as MapData
  const features = mapData?.geoJson?.features || []
  const mapLocationsContainer = document.querySelector(
    getMapSelector(mapId, CF_LOCATIONS_LIST_CONTAINER),
  )
  if (!mapLocationsContainer) return

  const checkboxContainers =
    mapLocationsContainer.querySelectorAll('.usa-checkbox')

  checkboxContainers.forEach((container, index) => {
    ;(container as HTMLElement).style.display = featureMatchesFilters(
      features[index],
      filters,
    )
      ? 'block'
      : 'none'
  })

  updateLocationCountForMap(mapId)
}

const initFilters = (mapId: string) => {
  const applyBtn = document.querySelector(
    getMapSelector(mapId, CF_APPLY_FILTERS_BTN),
  )
  const resetBtn = document.querySelector(
    getMapSelector(mapId, CF_RESET_FILTERS_BTN),
  )

  applyBtn?.addEventListener('click', () => applyLocationFiltersForMap(mapId))

  resetBtn?.addEventListener('click', () => {
    const filterSelects = getFilterSelectsForMap(mapId)
    filterSelects.forEach((selectElement) => {
      ;(selectElement as HTMLSelectElement).value = ''
    })
    applyLocationFiltersForMap(mapId, {})
  })
}

const updateLocationCountForMap = (mapId: string) => {
  const mapLocationsContainer = document.querySelector(
    getMapSelector(mapId, CF_LOCATIONS_LIST_CONTAINER),
  )
  if (!mapLocationsContainer) return

  const allCheckboxes = mapLocationsContainer.querySelectorAll('.usa-checkbox')
  const visibleCount = Array.from(allCheckboxes).filter(
    (cb) => (cb as HTMLElement).style.display !== 'none',
  ).length

  const countText = document.querySelector(
    getMapSelector(mapId, CF_LOCATION_COUNT),
  )
  if (countText) {
    countText.textContent = `Displaying ${visibleCount} of ${allCheckboxes.length} locations`
  }
}

const getFilterValuesForMap = (mapId: string): {[key: string]: string} => {
  const activeFilters: {[key: string]: string} = {}
  const filterSelects = getFilterSelectsForMap(mapId)

  filterSelects.forEach((selectElement) => {
    const select = selectElement as HTMLSelectElement
    if (select.value && select.value !== '') {
      activeFilters[select.name] = select.value
    }
  })
  return activeFilters
}
