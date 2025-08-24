interface Window {
  app: {
    data: {
      maxLocationSelections: Number
      maps: {
        [id: string]: object // Maps map IDs to GeoJSON objects
      }
    }
  }
}

declare const window: Window
