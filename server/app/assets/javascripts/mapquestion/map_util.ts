import {FeatureCollection} from 'geojson'

export interface MapData {
  geoJson: FeatureCollection
  settings: {
    [key: string]: string
  }
}

export const CF_LOCATIONS_LIST_CONTAINER = 'cf-locations-list'
export const CF_APPLY_FILTERS_BUTTON = 'cf-apply-filters-button'
export const CF_RESET_FILTERS_BUTTON = 'cf-reset-filters-button'
export const CF_LOCATION_COUNT = 'cf-location-count'
export const CF_NO_SELECTIONS_MESSAGE = 'cf-no-selections-message'
export const CF_SELECTED_LOCATIONS_LIST = 'cf-selected-locations-list'
export const CF_SELECT_LOCATION_BUTTON = 'cf-select-location-button'
export const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE =
  'cf-selected-location-checkbox-template'
export const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_INPUT =
  '.cf-selected-location-checkbox-template-input'
export const CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LABEL =
  '.cf-selected-location-checkbox-template-label'
export const CF_LOCATION_CHECKBOX = '.cf-location-checkbox'
export const CF_LOCATION_CHECKBOX_INPUT = '.cf-location-checkbox-input'
export const DATA_FEATURE_ID_ATTR = 'data-feature-id'
export const CF_POPUP_CONTENT_TEMPLATE = 'cf-popup-content-template'

// Query elements within a specific map container
export const mapQuerySelector = (
  mapId: string,
  className: string,
): Element | null => {
  return document.querySelector(`[data-map-id="${mapId}"].${className}`)
}

export const queryMapSelectOptions = (mapId: string): NodeListOf<Element> => {
  return document.querySelectorAll(`select[data-map-id="${mapId}"]`)
}

export const queryLocationCheckboxes = (mapId: string) => {
  const locationsListContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (!locationsListContainer) return []

  return locationsListContainer.querySelectorAll(CF_LOCATION_CHECKBOX)
}
