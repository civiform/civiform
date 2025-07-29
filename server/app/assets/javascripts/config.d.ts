interface Window {
  app: {
    data: {
      maps: {
        [id: string]: object // Maps map IDs to GeoJSON objects
      }
    }
  }
}

declare const window: Window
