import {
  CF_LOCATIONS_LIST_CONTAINER,
  CF_NO_SELECTIONS_MESSAGE,
  CF_SELECTED_LOCATIONS_LIST,
  CF_SELECT_LOCATION_BUTTON,
  CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE,
  CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_INPUT,
  CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LABEL,
  mapQuerySelector,
  queryLocationCheckboxes,
  DATA_FEATURE_ID_ATTR,
  CF_LOCATION_CHECKBOX_INPUT,
} from './map_util'

export const initLocationSelection = (mapId: string): void => {
  const locationCheckboxes = queryLocationCheckboxes(mapId)

  locationCheckboxes.forEach((locationCheckbox) => {
    // Add event listener to each checkbox to update selected locations on change event
    locationCheckbox.addEventListener('change', () =>
      updateSelectedLocations(mapId),
    )
  })

  document.addEventListener('click', (e) => {
    // Add event listener to select location buttons in map popups to update selected locations on click event
    const target = e.target as HTMLElement
    if (target.classList.contains(CF_SELECT_LOCATION_BUTTON)) {
      const featureId = target.getAttribute(DATA_FEATURE_ID_ATTR)
      if (featureId) {
        selectLocationsFromMap(featureId, mapId)
        // Close any open popups on click of select location button
        // TODO: Change color of button when selected? But not close popup? Talk to UX.
        const popups = document.querySelectorAll('.maplibregl-popup')
        popups.forEach((popup) => popup.remove())
      }
    }
  })
}

const updateSelectedLocations = (mapId: string): void => {
  const mapLocationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  )

  if (!mapLocationsContainer) return

  const selectedCheckboxes = mapLocationsContainer.querySelectorAll(
    'input[type="checkbox"]:checked',
  )
  const noSelectionsMessage = mapQuerySelector(
    mapId,
    CF_NO_SELECTIONS_MESSAGE,
  ) as HTMLElement | null
  const selectedLocationsList = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATIONS_LIST,
  ) as HTMLElement | null

  if (!noSelectionsMessage || !selectedLocationsList) return

  if (selectedCheckboxes.length === 0) {
    // Show "no selections" message
    noSelectionsMessage.classList.remove('display-none')
    selectedLocationsList.classList.add('display-none')
  } else {
    // Hide "no selections" message and show selected locations
    noSelectionsMessage.classList.add('display-none')
    selectedLocationsList.classList.remove('display-none')

    // Clear and rebuild the selected locations list
    selectedLocationsList.innerHTML = ''

    selectedCheckboxes.forEach((checkbox) => {
      const locationLabel = mapLocationsContainer.querySelector(
        `label[for="${checkbox.id}"]`,
      )
      if (locationLabel) {
        const selectedItem = createSelectedLocationCheckboxFromTemplate(
          checkbox,
          locationLabel,
          mapId,
        )
        if (selectedItem) {
          selectedLocationsList.appendChild(selectedItem)
        }
      }
    })
  }
}

const selectLocationsFromMap = (featureId: string, mapId: string): void => {
  const checkboxes = queryLocationCheckboxes(mapId)

  // Find the checkbox with the matching feature ID within this map's container
  checkboxes.forEach((checkbox) => {
    const checkboxValue = checkbox.getAttribute(DATA_FEATURE_ID_ATTR)
    if (checkboxValue === featureId) {
      const checkboxInputElement = checkbox.querySelector(
        CF_LOCATION_CHECKBOX_INPUT,
      ) as HTMLInputElement
      checkboxInputElement.checked = true
      checkbox.dispatchEvent(new Event('change'))
    }
  })
}

const copyFieldContent = (
  fieldName: string,
  locationLabel: Element,
  selectedLocation: HTMLElement,
): void => {
  const sourceSelector = `.cf-location-checkbox-${fieldName}`
  const templateSelector = `.cf-selected-location-checkbox-template-${fieldName}`

  const sourceElement = locationLabel.querySelector(sourceSelector)
  const templateElement = selectedLocation.querySelector(
    templateSelector,
  ) as HTMLElement

  if (sourceElement && templateElement) {
    // For links, copy href attribute
    if (fieldName === 'link') {
      const sourceLink = sourceElement as HTMLAnchorElement
      const templateLink = templateElement as HTMLAnchorElement
      templateLink.href = sourceLink.href
    } else {
      templateElement.textContent = sourceElement.textContent
    }
  } else if (templateElement) {
    templateElement.style.display = 'none'
  }
}

const createSelectedLocationCheckboxFromTemplate = (
  originalCheckbox: Element,
  locationLabel: Element,
  mapId: string,
): HTMLElement | undefined => {
  const selectedLocationTemplate = mapQuerySelector(
    mapId,
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE,
  )
  if (!selectedLocationTemplate) {
    return
  }

  const selectedLocation = selectedLocationTemplate.cloneNode(
    true,
  ) as HTMLElement
  selectedLocation.classList.remove(
    'hidden',
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE,
  )

  // Set up checkbox and label IDs
  const selectedCheckbox = selectedLocation.querySelector(
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_INPUT,
  ) as HTMLInputElement
  const selectedLabel = selectedLocation.querySelector(
    CF_SELECTED_LOCATION_CHECKBOX_TEMPLATE_LABEL,
  ) as HTMLLabelElement
  const selectedCheckboxId = `selected-${originalCheckbox.id}`
  selectedCheckbox.id = selectedCheckboxId
  selectedLabel.htmlFor = selectedCheckboxId

  // Copy content from source checkbox to selected checkbox template
  copyFieldContent('name', locationLabel, selectedLocation)
  copyFieldContent('address', locationLabel, selectedLocation)
  copyFieldContent('link', locationLabel, selectedLocation)

  // Add event listener for unchecking original checkbox
  selectedCheckbox.addEventListener('change', () => {
    const originalElement = document.getElementById(
      originalCheckbox.id,
    ) as HTMLInputElement
    if (originalElement) {
      originalElement.checked = false
      updateSelectedLocations(mapId)
    }
  })

  return selectedLocation
}
