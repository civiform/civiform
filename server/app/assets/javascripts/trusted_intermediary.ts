/** The question bank controller is responsible for manipulating the question bank. */
import {sortElementsByDataAttributes} from './sort_selector'

class TrustedIntermediaryController {
  static readonly SORT_SELECT_ID = 'ti-list'

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
      '.cf-ti-sublist',
      '.cf-ti-element',
    )
  }
}

export function init() {
  new TrustedIntermediaryController()
}
