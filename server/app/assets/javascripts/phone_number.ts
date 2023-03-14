import {assertNotNull} from './util'
import Cleave from 'cleave.js'
import 'cleave.js/dist/addons/cleave-phone.ca.js'
import 'cleave.js/dist/addons/cleave-phone.us.js'


export function init() {
  const element = document.getElementById('cf-question-text')
  //const countryElement = document.getElementById('cf-country-selector')
  if(element ){//&& countryElement) {
    new Cleave(element, {phone:true,phoneRegionCode:'US'}
      )
   /*  new Cleave(element, {
       numericOnly: true,
       blocks: [0,1,0,3, 0, 3, 4],
       delimiters: ["+"," ","(", ")", " ", "-"],
     });
     /* phone: true,
      //phoneRegionCode: (countryElement as HTMLInputElement).value,
       blocks:[0,3,3,4],
       delimiters: ['(',')','-']
       //prefix:'+1 '
    });*/
  }
}
