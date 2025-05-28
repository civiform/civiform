import {attachRedirectToPageListeners} from './main'
import {HtmxRequest} from './htmx_request'

/**
 * Dynamic behavior for AdminImportView.
 *
 * Some HTMX swap listeners ensure that the program import form (and its contents,
 * such as the stringified program JSON) work correctly.
 *
 * Listeners are also attached to the duplicate-handling radio groups, to support
 * dynamic interaction between the top-level radio group and each individual
 * question's radio group.
 *
 * For example, if a certain selection is made at the top-level, all individual radio
 * groups will be set to that selection. Conversely, if an individual question
 * selection is then made, the top-level radio group will reflect that the user is
 * choosing how to handle each question individually.
 */
class AdminImportView {
  constructor() {
    this.addRedirectListeners()
    this.addStringifyJsonListener()
  }

  /**
   * Adds listeners to all duplicate-handling radio groups to support dynamic
   * interactions between the top-level radio and each question's individual question.
   */
  addDuplicateHandlingListeners() {
    const toplevelRadio = document
      .getElementsByName('toplevelDuplicateQuestionHandling')
      .item(0) as HTMLFieldSetElement

    const duplicateQuestionRadios = document.getElementsByName(
      'duplicateQuestionHandling-',
    )

    toplevelRadio.addEventListener('change', (event) => {
      this.setAllQuestionRadiosToSpecifiedValue(
        duplicateQuestionRadios,
        (<HTMLInputElement>event.target)?.value,
      )
    })

    duplicateQuestionRadios.forEach((radio) => {
      radio.addEventListener('change', () =>
        this.setRadioToValue(toplevelRadio, 'DECIDE_FOR_EACH'),
      )
    })
  }

  setAllQuestionRadiosToSpecifiedValue(
    questionRadios: NodeList,
    value: string,
  ) {
    Array.from(questionRadios).forEach((radio) => {
      this.setRadioToValue(<HTMLFieldSetElement>radio, value)
    })
  }

  setRadioToValue(radio: HTMLFieldSetElement, value: string) {
    Array.from(radio.getElementsByTagName('input')).forEach((radioInput) => {
      if (radioInput.value === value) {
        radioInput.checked = true
      }
    })
  }

  addRedirectListeners() {
    document.addEventListener('htmx:afterSwap', () => {
      attachRedirectToPageListeners()
      this.addDuplicateHandlingListeners()
    })
  }

  addStringifyJsonListener() {
    document.body.addEventListener('htmx:configRequest', function (evt) {
      const customEvent = evt as CustomEvent<HtmxRequest>
      const formData = customEvent.detail.formData
      const programJson = formData.get('programJson') as string
      const trimmedProgramJson = JSON.stringify(JSON.parse(programJson))
      customEvent.detail.formData.set('programJson', trimmedProgramJson)
    })
  }
}

export function init() {
  new AdminImportView()
}
