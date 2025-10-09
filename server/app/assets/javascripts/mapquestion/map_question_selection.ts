import {
  CF_LOCATIONS_LIST_CONTAINER,
  CF_LOCATION_CHECKBOX,
  CF_SELECTED_LOCATIONS_CONTAINER,
  CF_NO_SELECTIONS_MESSAGE,
  CF_FILTER_HIDDEN,
  CF_PAGINATION_HIDDEN,
  mapQuerySelector,
  DATA_FEATURE_ID_ATTR,
  DATA_MAP_ID_ATTR,
  CF_LOCATION_CHECKBOX_INPUT,
  CF_SELECTED_LOCATION_MESSAGE,
  MapMessages,
  localizeString,
} from './map_util'

export const initLocationSelection = (
  mapId: string,
  messages: MapMessages,
): void => {
  // Initial update so the previously saved locations get displayed as selected
  updateSelectedLocations(mapId, messages)
}

export const updateSelectedLocations = (
  mapId: string,
  messages: MapMessages,
): void => {
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  )

  if (!mapLocationsContainer) {
    return
  }

  const selectedCheckboxes = Array.from(
    mapLocationsContainer.querySelectorAll(`.${CF_LOCATION_CHECKBOX}`),
  ).filter((checkbox) => {
    const input = checkbox.querySelector(
      'input[type="checkbox"]',
    ) as HTMLInputElement
    return input && input.checked
  })

  const selectedLocationsContainer = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATIONS_CONTAINER,
  ) as HTMLElement | null

  const noSelectionsTemplate = mapQuerySelector(
    mapId,
    CF_NO_SELECTIONS_MESSAGE,
  ) as HTMLTemplateElement | null

  if (!selectedLocationsContainer || !noSelectionsTemplate) {
    return
  }
  if (selectedCheckboxes.length === 0) {
    selectedLocationsContainer.textContent = ''
    const noSelectionsElement = noSelectionsTemplate.content.cloneNode(true)
    selectedLocationsContainer.appendChild(noSelectionsElement)
  } else {
    selectedLocationsContainer.textContent = ''
    selectedCheckboxes.forEach((originalCheckbox) => {
      const selectedLocation = originalCheckbox.cloneNode(true) as HTMLElement
      selectedLocation.classList.remove(CF_FILTER_HIDDEN)
      selectedLocation.classList.remove(CF_PAGINATION_HIDDEN)

      const input = selectedLocation.querySelector(
        'input[type="checkbox"]',
      ) as HTMLInputElement
      const label = selectedLocation.querySelector('label') as HTMLLabelElement
      // Remove the name so they don't get included in form submission
      input.name = ''

      if (input && label) {
        const originalId = input.id
        const selectedId = `selected-${originalId}`
        const featureId = originalCheckbox.getAttribute(DATA_FEATURE_ID_ATTR)

        input.id = selectedId
        label.htmlFor = selectedId
        input.setAttribute(DATA_MAP_ID_ATTR, mapId)

        if (featureId) {
          input.setAttribute(DATA_FEATURE_ID_ATTR, featureId)
        }
      }

      selectedLocationsContainer.appendChild(selectedLocation)
    })
  }
  updateSelectionCountForMap(mapId, messages)
}

const updateSelectionCountForMap = (
  mapId: string,
  messages: MapMessages,
): void => {
  const selectedLocationsListContainer = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATIONS_CONTAINER,
  ) as HTMLElement | null

  if (selectedLocationsListContainer == null) {
    return
  }

  const count =
    selectedLocationsListContainer.querySelectorAll(`.usa-checkbox`).length

  const countText = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATION_MESSAGE,
  ) as HTMLElement | null

  const maxLocationSelections = window.app?.data?.maxLocationSelections
  if (countText && maxLocationSelections) {
    countText.textContent = localizeString(messages.locationsSelectedCount, [
      count.toString(),
      maxLocationSelections.toString(),
    ])
  }
}

export const selectLocationsFromMap = (
  featureId: string,
  mapId: string,
  messages: MapMessages,
  isSelected: boolean,
): void => {
  const locationsListContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (!locationsListContainer) return

  const targetCheckbox = locationsListContainer.querySelector(
    `[${DATA_FEATURE_ID_ATTR}="${featureId}"]`,
  )
  if (targetCheckbox) {
    const checkboxInputElement = targetCheckbox.querySelector(
      `.${CF_LOCATION_CHECKBOX_INPUT}`,
    ) as HTMLInputElement
    if (checkboxInputElement) {
      checkboxInputElement.checked = isSelected
      updateSelectedLocations(mapId, messages)
    }
  }
}
