import {assertNotNull} from './util'
import Cleave from 'cleave.js';
import ''


export function init() {
  const element = document.getElementById('cf-question-text')
  if(element) {
     new Cleave(element, {
      phone: true,
      phoneRegionCode: 'US'
    });
  }
}
