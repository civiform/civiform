import {assertNotNull} from './util'
import Cleave from 'cleave.js'
import 'cleave.js/dist/addons/cleave-phone.ca.js'
import 'cleave.js/dist/addons/cleave-phone.us.js'


export function init() {
  const element = document.getElementById('cf-question-text')
  const countryElement = document.getElementById('cf-country-selector')
  if(element && countryElement) {
     new Cleave(element, {
      phone: true,
      phoneRegionCode: (countryElement as HTMLInputElement).value
    });
  }
}
