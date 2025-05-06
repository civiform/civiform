// Javascript handling for maps
// This file requires that main.ts is also added to the page.

import L from 'leaflet';

export function init() {
  console.log('init maps')
  const providers = [
    {
      name: 'Little Explorers Preschool',
      address: '123 Maple St, Seattle, WA 98101',
      latitude: 47.6101,
      longitude: -122.3421,
    },
    {
      name: 'Bright Beginnings Academy',
      address: '456 Pine St, Seattle, WA 98101',
      latitude: 47.6119,
      longitude: -122.335,
    },
    {
      name: 'Rainier Kids Center',
      address: '789 Rainier Ave S, Seattle, WA 98144',
      latitude: 47.5902,
      longitude: -122.308,
    },
    {
      name: 'Greenwood Daycare',
      address: '101 Greenwood Ave N, Seattle, WA 98103',
      latitude: 47.6941,
      longitude: -122.355,
    },
    {
      name: 'Capitol Hill Child Care',
      address: '202 Broadway E, Seattle, WA 98102',
      latitude: 47.6215,
      longitude: -122.3208,
    },
  ]
  const map = L.map('map').setView([47.6062, -122.3321], 12) // Seattle coords
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map)
  providers.forEach((provider) => {
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
            addLocationToMyList(provider.name)
          })
      })
  })
}

export function addLocationToMyList(providerName: string) {
  alert(`Added ${providerName} your list!`)
}
