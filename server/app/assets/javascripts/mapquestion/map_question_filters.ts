import {
  ExpressionSpecification,
  FilterSpecification,
  Map as MapLibreMap,
} from 'maplibre-gl'
import {Feature, Geometry, GeoJsonProperties} from 'geojson'
import {
  CF_APPLY_FILTERS_BUTTON,
  CF_RESET_FILTERS_BUTTON,
  CF_LOCATION_COUNT,
  LOCATIONS_LAYER,
  MapData,
  mapQuerySelector,
  CF_LOCATIONS_LIST_CONTAINER,
  CF_LOCATION_CHECKBOX,
  CF_LOCATION_HIDDEN,
  DATA_FEATURE_ID,
  DATA_FILTER_KEY,
  DATA_MAP_ID,
} from './map_util'

export const initFilters = (
  mapId: string,
  mapElement: MapLibreMap,
  mapData: MapData,
): void => {
  const featureMap = new Map<string, Feature>()
  mapData.geoJson.features.forEach((feature) => {
    if (feature.id) {
      featureMap.set(feature.id.toString(), feature)
    }
  })

  mapQuerySelector(mapId, CF_APPLY_FILTERS_BUTTON)?.addEventListener(
    'click',
    () => applyLocationFilters(mapId, mapElement, featureMap, mapData),
  )

  mapQuerySelector(mapId, CF_RESET_FILTERS_BUTTON)?.addEventListener(
    'click',
    () => {
      queryMapSelectOptions(mapId).forEach((selectOption) => {
        const selectOptionElement = (selectOption as HTMLSelectElement) || null
        if (!selectOptionElement) return
        selectOptionElement.value = ''
      })
      applyLocationFilters(mapId, mapElement, featureMap, mapData, true)
    },
  )
}

const applyLocationFilters = (
  mapId: string,
  map: MapLibreMap,
  featureMap: Map<string, Feature>,
  mapData: MapData,
  reset?: boolean,
): void => {
  const filters = reset ? {} : getFilters(mapId)

  map.setFilter(LOCATIONS_LAYER, buildMapFilterExpression(filters))

  const locationCheckboxContainers = queryLocationCheckboxes(mapId)

  locationCheckboxContainers.forEach((container) => {
    const containerElement = (container as HTMLElement) || null
    if (!containerElement) return

    const featureId = containerElement.getAttribute(DATA_FEATURE_ID)
    if (!featureId) return

    const matchingFeature = featureMap.get(featureId)
    const matchesFilter = featureMatchesFilters(matchingFeature, filters)

    if (matchesFilter) {
      containerElement.classList.remove(CF_LOCATION_HIDDEN)
    } else {
      containerElement.classList.add(CF_LOCATION_HIDDEN)
    }
  })

  updateLocationCountForMap(mapId, mapData)
}

const updateLocationCountForMap = (mapId: string, mapData: MapData): void => {
  const locationCheckboxes = queryLocationCheckboxes(mapId)
  const visibleCount = Array.from(locationCheckboxes).filter((checkbox) => {
    const checkboxElement = (checkbox as HTMLElement) || null
    return (
      checkboxElement && !checkboxElement.classList.contains(CF_LOCATION_HIDDEN)
    )
  }).length

  const countText = mapQuerySelector(
    mapId,
    CF_LOCATION_COUNT,
  ) as HTMLElement | null
  if (countText) {
    const messageTemplate = mapData.messages.locationsCount
    // Get the internalized string and replace {0} and {1} with the counts
    // {0} is the visible count, {1} is the total count
    // TODO: implement a more robust solution for string internationalization in typescript
    countText.textContent = messageTemplate
      .replace('{0}', visibleCount.toString())
      .replace('{1}', locationCheckboxes.length.toString())
  }
}

const featureMatchesFilters = (
  feature: Feature<Geometry, GeoJsonProperties> | undefined,
  filters: {[key: string]: string},
): boolean => {
  // if no properties or no filters, consider it a match
  if (!feature?.properties || Object.keys(filters).length === 0) return true
  return Object.entries(filters).every(
    ([key, value]) => feature.properties && feature.properties[key] === value,
  )
}

const getFilters = (mapId: string): {[key: string]: string} => {
  const activeFilters: {[key: string]: string} = {}
  const filterSelectOptions = queryMapSelectOptions(mapId)

  filterSelectOptions.forEach((selectElement) => {
    const select = (selectElement as HTMLSelectElement) || null
    if (!select) return
    // if a value is selected, add to active filters
    if (select.value && select.value !== '') {
      const filterKey = select.getAttribute(DATA_FILTER_KEY)
      if (filterKey) {
        activeFilters[filterKey] = select.value
      }
    }
  })
  return activeFilters
}

const buildMapFilterExpression = (filters: {
  [key: string]: string
}): FilterSpecification | null => {
  const filterCount = Object.keys(filters).length
  if (filterCount === 0) return null

  if (filterCount === 1) {
    const [key, value] = Object.entries(filters)[0]
    return ['==', ['get', key], value] as FilterSpecification
  }

  const specifications: ExpressionSpecification[] = []
  Object.entries(filters).forEach(([key, value]) => {
    specifications.push(['==', ['get', key], value])
  })
  return ['all', ...specifications] as ExpressionSpecification
}

const queryMapSelectOptions = (mapId: string): NodeListOf<Element> => {
  return document.querySelectorAll(`select[${DATA_MAP_ID}="${mapId}"]`)
}

const queryLocationCheckboxes = (mapId: string) => {
  const locationsListContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (!locationsListContainer) return []

  return locationsListContainer.querySelectorAll(`.${CF_LOCATION_CHECKBOX}`)
}
