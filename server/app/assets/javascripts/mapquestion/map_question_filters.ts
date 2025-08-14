import {FilterSpecification, Map as MapLibreMap} from 'maplibre-gl'
import {FeatureCollection, Feature, Geometry, GeoJsonProperties} from 'geojson'
import {
  CF_APPLY_FILTERS_BUTTON,
  CF_RESET_FILTERS_BUTTON,
  CF_LOCATION_COUNT,
  CF_LOCATIONS_LIST_CONTAINER,
  MapData,
  queryMapSelectOptions,
  mapQuerySelector,
} from './map_util'

export const initFilters = (
  mapId: string,
  mapElement: MapLibreMap,
  mapData: MapData,
): void => {
  mapQuerySelector(mapId, CF_APPLY_FILTERS_BUTTON)?.addEventListener(
    'click',
    () => applyLocationFilters(mapId, mapElement, mapData),
  )

  mapQuerySelector(mapId, CF_RESET_FILTERS_BUTTON)?.addEventListener(
    'click',
    () => {
      queryMapSelectOptions(mapId).forEach((selectOption) => {
        const selectOptionElement = selectOption as HTMLSelectElement
        selectOptionElement.value = ''
      })
      applyLocationFilters(mapId, mapElement, mapData, true)
    },
  )
}

const applyLocationFilters = (
  mapId: string,
  map: MapLibreMap,
  mapData: MapData,
  reset?: boolean,
): void => {
  const filters = reset ? {} : getFilterValues(mapId)

  map.setFilter('locations-layer', buildMapFilterExpression(filters))

  // Apply to checkboxes within this map's container
  const features: FeatureCollection['features'] =
    mapData?.geoJson?.features || ([] as FeatureCollection['features'])
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (!mapLocationsContainer) return

  const checkboxContainers =
    mapLocationsContainer.querySelectorAll('.usa-checkbox')

  checkboxContainers.forEach((container, index) => {
    const containerElement = container as HTMLElement
    containerElement.style.display = featureMatchesFilters(
      features[index],
      filters,
    )
      ? 'block'
      : 'none'
  })

  updateLocationCountForMap(mapId)
}

const updateLocationCountForMap = (mapId: string): void => {
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (!mapLocationsContainer) return

  const allCheckboxes = mapLocationsContainer.querySelectorAll('.usa-checkbox')
  const visibleCount = Array.from(allCheckboxes).filter(
    (cb) => (cb as HTMLElement).style.display !== 'none',
  ).length

  const countText = mapQuerySelector(
    mapId,
    CF_LOCATION_COUNT,
  ) as HTMLElement | null
  if (countText) {
    countText.textContent = `Displaying ${visibleCount} of ${allCheckboxes.length} locations`
  }
}

const featureMatchesFilters = (
  feature: Feature<Geometry, GeoJsonProperties> | undefined,
  filters: {[key: string]: string},
): boolean => {
  if (!feature?.properties || Object.keys(filters).length === 0) return true
  return Object.entries(filters).every(
    ([key, value]) => feature.properties && feature.properties[key] === value,
  )
}

const getFilterValues = (mapId: string): {[key: string]: string} => {
  const activeFilters: {[key: string]: string} = {}
  const filterSelects = queryMapSelectOptions(mapId)

  filterSelects.forEach((selectElement) => {
    const select = selectElement as HTMLSelectElement
    if (select.value && select.value !== '') {
      activeFilters[select.name] = select.value
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

  const conditions: FilterSpecification[] = [['all']]
  Object.entries(filters).forEach(([key, value]) => {
    conditions.push(['==', ['get', key], value])
  })
  return conditions as FilterSpecification
}
