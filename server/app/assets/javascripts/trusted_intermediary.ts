// Javascript handling for the trusted intermediary page

import {addEventListenerToElements} from './util'

export function init() {
  addEventListenerToElements('#cf-ti-clear-search', 'click', clearSearchForm)
}

/** Clear the client search form when the clear link is clicked. */
function clearSearchForm() {
  const inputIds = [
    'name-query',
    'date_of_birth_day',
    'date_of_birth_month',
    'date_of_birth_year',
  ]
  for (const inputId of inputIds) {
    ;(document.getElementById(inputId) as HTMLInputElement).value = ''
  }
}
