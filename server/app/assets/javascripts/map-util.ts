import {FeatureCollection} from 'geojson'

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

export const mapQuerySelector = (mapId: string, className: string) => {
  return document.querySelector(`[data-map-id="${mapId}"].${className}`)
}

export const getFilterSelectsForMap = (mapId: string) => {
  return document.querySelectorAll(`select[data-map-id="${mapId}"]`)
}
