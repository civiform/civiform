import {LngLatLike, Map, MapMouseEvent, Popup} from 'maplibre-gl'
import {FeatureCollection, Point} from 'geojson'

export const init = () => {
  const geoJsonDataObject = window.app?.data?.maps || {}

  Object.entries(geoJsonDataObject).forEach(([mapId, geoJsonData]) => {
    renderMap(mapId, geoJsonData as FeatureCollection)
  })
}

const renderMap = (mapId: string, geoJsonData: FeatureCollection) => {
  console.log(`Rendering map with ID: ${mapId}`)

  if (geoJsonData) {
    console.log(`GeoJSON Data for ${mapId}:`, geoJsonData)
  }

  const map = new Map({
    container: mapId,
    style: {
      version: 8,
      sources: {
        osm: {
          type: 'raster',
          tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
          tileSize: 256,
          attribution: '© OpenStreetMap contributors',
        },
      },
      layers: [{id: 'osm', type: 'raster', source: 'osm'}],
    },
    // TODO(#11095): Allow configurable center point
    center: [-122.3321, 47.6062],
    zoom: 8,
  })

  map.on('load', async () => {
    map.addSource(`${mapId}-locations`, {
      type: 'geojson',
      data: geoJsonData,
    })
    map.addLayer({
      id: `${mapId}-locations-layer`,
      type: 'circle',
      source: `${mapId}-locations`,
      paint: {
        'circle-color': '#CC38FF'
      },
    })

    map.on('click', `${mapId}-locations-layer`, (e: any) => {
      const coordinates = e.features[0].geometry.coordinates.slice();
      const name = e.features[0].properties.name;
      new Popup({closeButton: false})
        .setLngLat(coordinates)
        .setHTML(name)
        .addTo(map);
    })

    // Change the cursor to a pointer when the mouse is over the places layer.
    map.on('mouseenter', `${mapId}-locations-layer`, () => {
        map.getCanvas().style.cursor = 'pointer';
    });

    // Change it back to a pointer when it leaves.
    map.on('mouseleave', `${mapId}-locations-layer`, () => {
        map.getCanvas().style.cursor = '';
    });

  })

}
