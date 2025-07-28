interface Window {
  app: {
    data: {
      maps: {
        [id: string]: object // Maps map IDs to GeoJSON strings
      }
    }
  }
}

declare const window: Window
