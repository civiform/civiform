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
  POPUP_LAYER,
  MapData,
  mapQuerySelector,
  CF_FILTER_HIDDEN,
  DATA_FEATURE_ID,
  DATA_FILTER_KEY,
  DATA_MAP_ID,
  queryLocationCheckboxes,
  updateLocationCountForMap,
} from './map_util'
import {resetPagination} from './map_question_pagination'

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
    () => {
      applyLocationFilters(mapId, mapElement, featureMap)
      locationFocus(mapId)
    },
  )

  mapQuerySelector(mapId, CF_RESET_FILTERS_BUTTON)?.addEventListener(
    'click',
    () => {
      queryMapSelectOptions(mapId).forEach((selectOption) => {
        const selectOptionElement = (selectOption as HTMLSelectElement) || null
        if (!selectOptionElement) return
        selectOptionElement.value = ''
      })
      applyLocationFilters(mapId, mapElement, featureMap, true)
      locationFocus(mapId)
    },
  )
}

const locationFocus = (mapId: string): void => {
  const locationCount = mapQuerySelector(
    mapId,
    CF_LOCATION_COUNT,
  ) as HTMLElement
  locationCount.focus()
}

const applyLocationFilters = (
  mapId: string,
  map: MapLibreMap,
  featureMap: Map<string, Feature>,
  reset?: boolean,
): void => {
  const filters = reset ? {} : getFilters(mapId)

  map.setFilter(LOCATIONS_LAYER, buildMapFilterExpression(filters))

  const locationCheckboxContainers = queryLocationCheckboxes(mapId)

  const popupContent = mapQuerySelector(mapId, POPUP_LAYER) as HTMLElement
  let openPopupFeatureId = null
  if (popupContent) {
    openPopupFeatureId = popupContent.getAttribute(DATA_FEATURE_ID)
  }

  let visibleCount = 0
  locationCheckboxContainers.forEach((container) => {
    const containerElement = container || null
    if (!containerElement) return

    const featureId = containerElement.getAttribute(DATA_FEATURE_ID)
    if (!featureId) return

    const matchingFeature = featureMap.get(featureId)
    const matchesFilter = featureMatchesFilters(matchingFeature, filters)

    if (matchesFilter) {
      containerElement.classList.remove(CF_FILTER_HIDDEN)
      visibleCount++
    } else {
      containerElement.classList.add(CF_FILTER_HIDDEN)
      if (featureId == openPopupFeatureId) {
        const popup = popupContent.parentElement?.parentElement
        if (popup) popup.remove()
      }
    }
  })

  updateLocationCountForMap(mapId, visibleCount)
  resetPagination(mapId)
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
}): FilterSpecification | undefined => {
  const filterCount = Object.keys(filters).length
  if (filterCount === 0) return undefined

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
