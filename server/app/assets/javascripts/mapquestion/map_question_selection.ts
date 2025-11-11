import {
  CF_FILTER_HIDDEN,
  CF_LOCATION_CHECKBOX_INPUT,
  CF_LOCATIONS_LIST_CONTAINER,
  CF_MAP_QUESTION_ALERT_HIDDEN,
  CF_MAP_QUESTION_TAG_ALERT,
  CF_NO_SELECTIONS_MESSAGE,
  CF_SELECTIONS_MESSAGE,
  CF_PAGINATION_HIDDEN,
  CF_SELECTED_LOCATION_MESSAGE,
  CF_SELECTED_LOCATIONS_CONTAINER,
  DATA_FEATURE_ID,
  DATA_MAP_ID,
  getMessages,
  hasReachedMaxSelections,
  localizeString,
  MapData,
  mapQuerySelector,
  queryLocationCheckboxes,
  selectionCounts,
  CF_MAX_LOCATION_STATUS,
} from './map_util'

export const initLocationSelection = (mapId: string): void => {
  // Initial update so the previously saved locations get displayed as selected
  updateSelectedLocations(mapId)
  updateAlertVisibility(mapId)
}

export const updateSelectedLocations = (mapId: string): void => {
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  )

  if (!mapLocationsContainer) {
    return
  }

  const locationCheckboxes = queryLocationCheckboxes(mapId)

  const selectedCheckboxes = Array.from(locationCheckboxes).filter(
    (checkbox) => {
      const input = checkbox.querySelector(
        'input[type="checkbox"]',
      ) as HTMLInputElement
      return input && input.checked
    },
  )

  const selectedLocationsContainer = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATIONS_CONTAINER,
  ) as HTMLElement | null

  const noSelectionsTemplate = mapQuerySelector(
    mapId,
    CF_NO_SELECTIONS_MESSAGE,
  ) as HTMLTemplateElement | null

  const selectionsTemplate = mapQuerySelector(
    mapId,
    CF_SELECTIONS_MESSAGE,
  ) as HTMLTemplateElement | null

  if (
    !selectedLocationsContainer ||
    !noSelectionsTemplate ||
    !selectionsTemplate
  ) {
    return
  }
  if (selectedCheckboxes.length === 0) {
    selectedLocationsContainer.textContent = ''
    const noSelectionsElement = noSelectionsTemplate.content.cloneNode(true)
    selectedLocationsContainer.appendChild(noSelectionsElement)
  } else {
    selectedLocationsContainer.textContent = ''
    const selectionsElement = selectionsTemplate.content.cloneNode(true)
    selectedLocationsContainer.appendChild(selectionsElement)
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
        const featureId = originalCheckbox.getAttribute(DATA_FEATURE_ID)

        input.id = selectedId
        label.htmlFor = selectedId
        input.setAttribute(DATA_MAP_ID, mapId)

        if (featureId) {
          input.setAttribute(DATA_FEATURE_ID, featureId)
        }
      }

      selectedLocationsContainer.appendChild(selectedLocation)
    })
  }
  updateSelectionCountForMap(mapId)
  updateAlertVisibility(mapId)

  // Disable unchecked checkboxes if max selections reached
  const atMaxSelections = hasReachedMaxSelections(mapId)

  locationCheckboxes.forEach((checkbox) => {
    const input = checkbox.querySelector(
      'input[type="checkbox"]',
    ) as HTMLInputElement
    if (input) {
      // Only disable unchecked checkboxes when max is reached
      if (atMaxSelections && !input.checked) {
        input.disabled = true
      } else {
        input.disabled = false
      }
    }
  })

  if (atMaxSelections) {
    const mapData = window.app?.data?.maps?.[mapId] as MapData
    const maxLocationSelections = mapData.settings.maxLocationSelections
    const maxLocationStatus = mapQuerySelector(mapId, CF_MAX_LOCATION_STATUS)

    if (maxLocationStatus && maxLocationSelections) {
      maxLocationStatus.textContent = localizeString(
        getMessages().maxLocationsSelectedSr,
        [maxLocationSelections],
      )
      // Clear the text after announcement to prevent navigation to it
      setTimeout(() => {
        maxLocationStatus.textContent = ''
      }, 5000)
    }
  }
}

const updateSelectionCountForMap = (mapId: string): void => {
  const selectedLocationsListContainer = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATIONS_CONTAINER,
  ) as HTMLElement | null

  if (selectedLocationsListContainer == null) {
    return
  }

  const count =
    selectedLocationsListContainer.querySelectorAll(`.usa-checkbox`).length

  selectionCounts.set(mapId, count)

  const countText = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATION_MESSAGE,
  ) as HTMLElement | null

  const mapData = window.app?.data?.maps?.[mapId] as MapData
  const maxLocationSelections = mapData.settings.maxLocationSelections
  if (countText && maxLocationSelections) {
    countText.textContent = localizeString(
      getMessages().locationsSelectedCount,
      [count.toString(), maxLocationSelections.toString()],
    )
  }
}

export const selectLocationsFromMap = (
  featureId: string,
  mapId: string,
  isSelected: boolean,
): void => {
  const locationsListContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (!locationsListContainer) return

  const targetCheckbox = locationsListContainer.querySelector(
    `[${DATA_FEATURE_ID}="${featureId}"]`,
  )
  if (targetCheckbox) {
    const checkboxInputElement = targetCheckbox.querySelector(
      `.${CF_LOCATION_CHECKBOX_INPUT}`,
    ) as HTMLInputElement
    if (checkboxInputElement) {
      checkboxInputElement.checked = isSelected
      updateSelectedLocations(mapId)
    }
  }
}

const updateAlertVisibility = (mapId: string): void => {
  const mapData = window.app?.data?.maps?.[mapId] as MapData
  const alert = mapQuerySelector(mapId, CF_MAP_QUESTION_TAG_ALERT)

  if (!mapData || !alert) return

  const {tagGeoJsonKey, tagGeoJsonValue} = mapData.settings

  // If no tag is configured, keep alert hidden
  if (!tagGeoJsonKey || !tagGeoJsonValue) {
    alert.classList.add(CF_MAP_QUESTION_ALERT_HIDDEN)
    return
  }

  const locationCheckboxes = queryLocationCheckboxes(mapId)

  let hasTaggedLocation: boolean = false
  for (const checkbox of Array.from(locationCheckboxes)) {
    const input = checkbox.querySelector(
      'input[type="checkbox"]',
    ) as HTMLInputElement

    if (input && input.checked) {
      const featureId = checkbox.getAttribute(DATA_FEATURE_ID)
      if (featureId) {
        const feature = mapData.geoJson.features.find(
          (feature) => String(feature.id) === featureId,
        )
        if (feature && feature.properties) {
          const propertyValue = feature.properties[tagGeoJsonKey] as string
          if (propertyValue === tagGeoJsonValue) {
            hasTaggedLocation = true
            break
          }
        }
      }
    }
  }

  if (hasTaggedLocation) {
    alert.classList.remove(CF_MAP_QUESTION_ALERT_HIDDEN)
  } else {
    alert.classList.add(CF_MAP_QUESTION_ALERT_HIDDEN)
  }
}
