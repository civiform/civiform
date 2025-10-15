// Javascript handling for the trusted intermediary page

import {addEventListenerToElements} from './util'

export function init() {
  addEventListenerToElements('#cf-ti-clear-search', 'click', clearSearchForm)
}

/** Clear the client search form when the clear link is clicked. */
function clearSearchForm() {
  const inputIds = [
    'name-query',
    'date-of-birth-day',
    'date-of-birth-month',
    'date-of-birth-year',
  ]
  for (const inputId of inputIds) {
    ;(document.getElementById(inputId) as HTMLInputElement).value = ''
  }
}
