interface Window {
  app: {
    data: {
      maps: {
        [id: string]: string // Maps map IDs to GeoJSON strings
      }
    }
  }
}

declare const window: Window
