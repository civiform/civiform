interface Window {
  app: {
    scripts: {
      AdminProgramApiBridge: AdminProgramApiBridge
      AdminPredicateEdit: AdminPredicateEdit
    }
    data: {
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
      predicate: {
        operator_scalars: {
          [key: string]: Array<string>
        }
      }
    }
  }
}

declare const window: Window
