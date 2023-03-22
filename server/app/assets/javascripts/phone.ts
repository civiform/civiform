import {assertNotNull} from './util'
import Cleave from 'cleave.js'


export function init() {
  const element = document.getElementById('cf-phone-number')
  const countryElement = document.getElementById('cf-phone-country-code')
  if(element && countryElement) {
    new Cleave(element, {
      numericOnly: true,
      blocks: [0, 3, 3, 4],
      delimiters: ['(', ') ', '-']
    });
  }
}
