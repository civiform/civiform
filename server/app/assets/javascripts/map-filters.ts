import {FilterSpecification, GeoJSONFeature} from 'maplibre-gl'
import {
  CF_APPLY_FILTERS_BTN,
  CF_RESET_FILTERS_BTN,
  CF_LOCATION_COUNT,
  CF_LOCATIONS_LIST_CONTAINER,
  MapData,
  getFilterSelectsForMap,
  mapQuerySelector,
} from './map-util'

export const initFilters = (mapId: string, mapInstances: Map<string, any>) => {
  const applyBtn = mapQuerySelector(mapId, CF_APPLY_FILTERS_BTN)
  const resetBtn = mapQuerySelector(mapId, CF_RESET_FILTERS_BTN)

  applyBtn?.addEventListener('click', () =>
    applyLocationFiltersForMap(mapId, mapInstances),
  )

  resetBtn?.addEventListener('click', () => {
    const filterSelects = getFilterSelectsForMap(mapId)
    filterSelects.forEach((selectElement) => {
      ;(selectElement as HTMLSelectElement).value = ''
    })
    applyLocationFiltersForMap(mapId, mapInstances, {})
  })
}

export const applyLocationFiltersForMap = (
  mapId: string,
  mapInstances: Map<string, any>,
  filterOverride?: {[key: string]: string},
) => {
  const filters = filterOverride ?? getFilterValues(mapId)

  // Apply to specific map
  const map = mapInstances.get(mapId)
  if (map) {
    map.setFilter('locations-layer', buildMapFilterExpression(filters))
  }

  // Apply to checkboxes within this map's container
  const mapData = window.app?.data?.maps?.[mapId] as MapData
  const features = mapData?.geoJson?.features || []
  const mapLocationsContainer = mapQuerySelector(mapId, CF_LOCATIONS_LIST_CONTAINER)
  if (!mapLocationsContainer) return

  const checkboxContainers =
    mapLocationsContainer.querySelectorAll('.usa-checkbox')

  checkboxContainers.forEach((container, index) => {
    ;(container as HTMLElement).style.display = featureMatchesFilters(
      features[index] as GeoJSONFeature,
      filters,
    )
      ? 'block'
      : 'none'
  })

  updateLocationCountForMap(mapId)
}

export const updateLocationCountForMap = (mapId: string) => {
  const mapLocationsContainer = mapQuerySelector(mapId, CF_LOCATIONS_LIST_CONTAINER)
  if (!mapLocationsContainer) return

  const allCheckboxes = mapLocationsContainer.querySelectorAll('.usa-checkbox')
  const visibleCount = Array.from(allCheckboxes).filter(
    (cb) => (cb as HTMLElement).style.display !== 'none',
  ).length

  const countText = mapQuerySelector(mapId, CF_LOCATION_COUNT)
  if (countText) {
    countText.textContent = `Displaying ${visibleCount} of ${allCheckboxes.length} locations`
  }
}

export const featureMatchesFilters = (
  feature: GeoJSONFeature,
  filters: {[key: string]: string},
): boolean => {
  if (!feature?.properties || Object.keys(filters).length === 0) return true
  return Object.entries(filters).every(
    ([key, value]) => feature.properties[key] === value,
  )
}

export const getFilterValues = (mapId: string): {[key: string]: string} => {
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

export const buildMapFilterExpression = (filters: {
  [key: string]: string
}): FilterSpecification | null => {
  const filterCount = Object.keys(filters).length
  if (filterCount === 0) return null

  if (filterCount === 1) {
    const [key, value] = Object.entries(filters)[0]
    return ['==', ['get', key], value] as FilterSpecification
  }

  const conditions: FilterSpecification[] = [['all']]
  Object.entries(filters).forEach(([key, value]) => {
    conditions.push(['==', ['get', key], value])
  })
  return conditions as FilterSpecification
}
