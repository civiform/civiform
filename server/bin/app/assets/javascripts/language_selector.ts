// Javascript handling for the applicant language selector
import {addEventListenerToElements} from './util'

function onLanguageChange(event: Event) {
  const languageSelector = event.target as HTMLSelectElement
  languageSelector.form!.submit()
}

export function init() {
  addEventListenerToElements('#select-language', 'change', onLanguageChange)
}
