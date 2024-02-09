/**
 * Input formatting for PhoneQuestion question type
 * This file requires third party dependency- cleave.js for formatting the input as the user types.
 * Documentation for it can be found here: https://github.com/nosir/cleave.js/
 */

import Cleave from 'cleave.js'

function formatPhone(element: HTMLElement) {
  new Cleave(element, {
    // Only accept digits as input
    numericOnly: true,
    // An Array value indicates the groups to format the input value. It will insert delimiters in between these groups.
    // it will take the index from the last block. ex.0 -> insert '(' at 0th spot, 3 -> insert ')' after 3 characters from 0th block
    blocks: [0, 3, 3, 4],
    // the delimiters at the specified blocks
    delimiters: ['(', ') ', '-'],
  })
}

export function init() {
  const element = document.getElementById('cf-phone-number')
  const countryElement = document.getElementById('cf-phone-country-code')
  if (element && countryElement) {
    formatPhone(element)
  }
  const phoneElement = document.getElementById('edit-phone-number-input')
  if (phoneElement) {
    formatPhone(phoneElement)
  }
}
