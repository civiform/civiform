import {FilterSpecification, Map as MapLibreMap} from 'maplibre-gl'
import {FeatureCollection, Feature, Geometry, GeoJsonProperties} from 'geojson'
import {
  CF_APPLY_FILTERS_BUTTON,
  CF_RESET_FILTERS_BUTTON,
  CF_LOCATION_COUNT,
  MapData,
  queryMapSelectOptions,
  mapQuerySelector,
  queryLocationCheckboxes
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
  const filters = reset ? {} : getFilters(mapId)

  map.setFilter('locations-layer', buildMapFilterExpression(filters))

  const features: FeatureCollection['features'] =
    mapData?.geoJson?.features || ([] as FeatureCollection['features'])

  const locationCheckboxContainers = queryLocationCheckboxes(mapId)

  locationCheckboxContainers.forEach((container, index) => {
    const containerElement = container as HTMLElement
    // Assuming order of features matches order of location checkboxes
    // TODO: Improve this by linking via location ID
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
  const locationCheckboxes = queryLocationCheckboxes(mapId)
  const visibleCount = Array.from(locationCheckboxes).filter((checkbox) => {
    const checkboxElement = checkbox as HTMLElement
    return checkboxElement.style.display !== 'none'
  }).length

  const countText = mapQuerySelector(
    mapId,
    CF_LOCATION_COUNT,
  ) as HTMLElement | null
  if (countText) {
    countText.textContent = `Displaying ${visibleCount} of ${locationCheckboxes.length} locations`
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
    const select = selectElement as HTMLSelectElement
    // if a value is selected, add to active filters
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

  const specifications: FilterSpecification[] = [['all']]
  Object.entries(filters).forEach(([key, value]) => {
    specifications.push(['==', ['get', key], value])
  })
  return specifications as FilterSpecification
}
