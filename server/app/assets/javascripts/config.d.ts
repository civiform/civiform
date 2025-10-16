interface Window {
  app: {
    scripts: {
      AdminProgramApiBridge: AdminProgramApiBridge
    }
    data: {
      maxLocationSelections: number
      messages: object
      iconUrls: {
        locationIcon: string
        selectedLocationIcon: string
      }
      maps: {
        [id: string]: object // Maps map IDs to GeoJSON objects
      }
      bridge: {
        question_scalars: {
          [key: string]: Array<{value: string; display: string}>
        }
      }
      questionType: QuestionType
    }
  }
}

declare const window: Window
