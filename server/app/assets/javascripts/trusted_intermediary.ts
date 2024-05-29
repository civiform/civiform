/** The question bank controller is responsible for manipulating the question bank. */
import {assertNotNull} from './util'
import {sortSelectorElements} from './sort_selector'

class TrustedIntermediaryController {
  static readonly SORT_SELECT_ID = 'ti-list-sort'

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
    sortSelectorElements(
      TrustedIntermediaryController.SORT_SELECT_ID,
      '.cf-ti-list-element',
      '.cf-sortable-elements',
    )
  }
}

export function init() {
  new TrustedIntermediaryController()
}
