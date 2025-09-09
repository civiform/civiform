interface Window {
  app: {
    data: {
      maxLocationSelections: number
      messages: Object
      maps: {
        [id: string]: object // Maps map IDs to GeoJSON objects
      }
    }
  }
}

declare const window: Window
