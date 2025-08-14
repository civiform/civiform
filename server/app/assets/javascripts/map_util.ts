import {
  FilterSpecification,
  LngLatLike,
  Map as MapLibreMap,
  MapGeoJSONFeature,
  Popup,
} from 'maplibre-gl'

import {FeatureCollection, GeoJsonProperties} from 'geojson'

export interface MapData {
  geoJson: FeatureCollection
  settings: {
    [key: string]: string
  }
}

export const CF_LOCATIONS_LIST_CONTAINER = 'cf-locations-list'
export const CF_APPLY_FILTERS_BTN = 'cf-apply-filters-btn'
export const CF_RESET_FILTERS_BTN = 'cf-reset-filters-btn'
export const CF_LOCATION_COUNT = 'cf-location-count'
export const CF_NO_SELECTIONS_MESSAGE = 'cf-no-selections-message'
export const CF_SELECTED_LOCATIONS_LIST = 'cf-selected-locations-list'
export const CF_SELECT_LOCATION_BUTTON = 'cf-select-location-btn'

export const getMapSelector = (mapId: string, className: string) =>
  `[data-map-id="${mapId}"].${className}`
export const getFilterSelectsForMap = (mapId: string) => {
  return document.querySelectorAll(`select[data-map-id="${mapId}"]`)
}

export const createPopupContent = (
  settings: MapData['settings'],
  properties: GeoJsonProperties,
): string | undefined => {
  if (!properties) return

  let popupContent = ''

  const name = properties[settings['nameGeoJsonKey']]
  if (name) {
    popupContent += `<div class="text-bold font-serif-sm padding-bottom-2">${name}</div>`
  }

  const address = properties[settings['addressGeoJsonKey']]
  if (address) {
    popupContent += `<div class="font-sans-sm">${address}</div>`
  }

  const detailsUrl = properties[settings['detailsUrlGeoJsonKey']]
  if (detailsUrl) {
    popupContent += `<a class="font-sans-sm usa-link usa-link--external" href="${detailsUrl}" target="_blank">View more details</a>`
  }

  if (name) {
    const selectButton = `<button 
        type="button"
        class="usa-button usa-button--secondary margin-top-1 ${CF_SELECT_LOCATION_BUTTON}" 
        data-location-name="${name}">
        Select location
      </button>`
    popupContent += selectButton
  }

  return popupContent
}

export const buildMapFilterExpression = (filters: {
  [key: string]: string
}): FilterSpecification | null => {
  const filterCount = Object.keys(filters).length
  if (filterCount === 0) return null

  if (filterCount === 1) {
    const [key, value] = Object.entries(filters)[0]
    return ['==', ['get', key], value] as any
  }

  const conditions = ['all'] as any[]
  Object.entries(filters).forEach(([key, value]) => {
    conditions.push(['==', ['get', key], value])
  })
  return conditions as FilterSpecification
}
