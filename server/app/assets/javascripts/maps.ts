// Javascript handling for maps
// This file requires that main.ts is also added to the page.

import L from 'leaflet'
import {assertNotNull} from './util'

let selectedProviderNames: Array<string> = []

export function init() {
  selectedProviderNames = []
  const providers = JSON.parse(
    document.getElementById('all-providers')?.getAttribute('value') as string,
  )
  const map = L.map('map').setView([47.6062, -122.3321], 12) // Seattle coords
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map)
  providers.forEach((provider: any) => {
    const popup = L.DomUtil.create('div', 'infoWindow')
    popup.innerHTML = `<strong>${provider.name}</strong><br>${provider.address}<br><a class="usa-button-outline" href="www.google.com" target="_blank">See more</a><br><button id="add-to-my-list" class="add-to-my-list usa-button">Add to my list</button>`
    L.marker([provider.latitude, provider.longitude])
      .addTo(map)
      .bindPopup(popup)
      .on('popupopen', (a) => {
        const popUp = a.target.getPopup()
        popUp
          .getElement()
          .querySelector('#add-to-my-list')
          .addEventListener('click', () => {
            addLocationToMyList(provider.name, provider.address)
          })
      })
  })
}

export function addLocationToMyList(providerName: string, providerAddress: string) {
  if (selectedProviderNames.includes(providerName)) {
    alert(`${providerName} has already been added!`)
  } else {
    // Copy the selected provider template
    const newField = assertNotNull(
      document.getElementById('selected-provider-template'),
    ).cloneNode(true) as HTMLElement
    newField.classList.remove('hidden')
    newField.removeAttribute('id')
    newField.innerHTML=`<td>${providerName}</td><td>${providerAddress}</td>`

    // Add to the end of selected-providers table.
    const selectedProviders = assertNotNull(
      document.getElementById('selected-providers'),
    )

    selectedProviders.appendChild(newField)
    selectedProviderNames.push(providerName)
  }
}
