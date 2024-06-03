/** The question bank controller is responsible for manipulating the trusted intermediary group list. */
import {sortElementsByDataAttributes} from './sort_selector'

class TrustedIntermediaryController {
  // Keep in sync with TrustedIntermediaryGrouplistView.java
  static readonly SORT_SELECT_ID = 'cf-ti-list'
  static readonly SORT_SELECT_SUBLIST_CLASS = '.cf-ti-sublist'
  static readonly SORT_SELECT_ELEMENT_CLASS = '.cf-ti-element'

  constructor() {
    const tiListSort = document.getElementById(
      TrustedIntermediaryController.SORT_SELECT_ID,
    ) as HTMLSelectElement

    if (tiListSort) {
      tiListSort.addEventListener(
        'change',
        TrustedIntermediaryController.sortList,
      )
    }
  }
  private static sortList() {
    sortElementsByDataAttributes(
      TrustedIntermediaryController.SORT_SELECT_ID,
      TrustedIntermediaryController.SORT_SELECT_SUBLIST_CLASS,
      TrustedIntermediaryController.SORT_SELECT_ELEMENT_CLASS,
    )
  }
}

export function init() {
  new TrustedIntermediaryController()
}
