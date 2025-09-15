interface Window {
  app: {
    data: {
      maxLocationSelections: number
      messages: object
      maps: {
        [id: string]: object // Maps map IDs to GeoJSON objects
      }
    }
  }
}

declare const window: Window
