import {attachRedirectToPageListeners} from '@/main'
import {HtmxRequest} from '@/htmx_request'

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
  duplicateQuestionHandlingPrefix = 'duplicateQuestionHandling-'

  constructor() {
    // If we aren't on the import page, do nothing
    if (!document.getElementById('admin-import-header')) {
      return
    }
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
      this.duplicateQuestionHandlingPrefix,
    )

    toplevelRadio.addEventListener('change', (event) => {
      this.setAllQuestionRadiosToSpecifiedValue(
        duplicateQuestionRadios,
        (<HTMLInputElement>event.target)?.value,
      )
    })

    duplicateQuestionRadios.forEach((radio) => {
      radio.addEventListener('change', (event) =>
        this.onDuplicateQuestionRadioChange(event, toplevelRadio),
      )
    })
  }

  onDuplicateQuestionRadioChange(
    event: Event,
    toplevelRadio: HTMLFieldSetElement,
  ) {
    this.setRadioToValue(toplevelRadio, 'DECIDE_FOR_EACH')
    this.setRepeatedQuestionHandling(event)
  }

  setRepeatedQuestionHandling(event: Event) {
    const target = <HTMLInputElement>event.target
    const currentAdminName = target?.name.slice(
      this.duplicateQuestionHandlingPrefix.length,
    )
    const repeatedQs = document.querySelectorAll(
      '[data-enumerator="' + currentAdminName + '"]',
    )
    const selection = (<HTMLInputElement>event.target)?.value
    // If an enumerator is using/overwriting an existing question,
    // then its repeated questions can do whatever they want.
    // If it's creating a duplicate, then its repeated Qs must as well.
    if (repeatedQs.length > 0) {
      const parentFieldset = target?.closest('fieldset') as HTMLFieldSetElement
      const warnings = parentFieldset.getElementsByClassName(
        'repeated-disabled-warning',
      )
      const duplicate = selection === 'CREATE_DUPLICATE'
      for (let i = 0; i < (warnings?.length || 0); i++) {
        warnings[i].toggleAttribute('hidden', !duplicate)
      }

      repeatedQs.forEach((repeatedQ) => {
        const radios = repeatedQ.getElementsByTagName('fieldset')
        if (radios.length > 0) {
          const radio = <HTMLFieldSetElement>(
            repeatedQ.getElementsByTagName('fieldset').item(0)
          )
          if (duplicate) {
            this.disableRadioValuesExcept(radio, 'CREATE_DUPLICATE')
          } else {
            this.enableAllRadioValues(radio)
          }
        }
      })
    }
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

  disableRadioValuesExcept(radio: HTMLFieldSetElement, value: string) {
    Array.from(radio.getElementsByTagName('input')).forEach((radioInput) => {
      const shouldDisableInput = radioInput.value !== value
      radioInput.disabled = shouldDisableInput
    })
    this.hideRepeatedWarning(radio, false)
    this.setRadioToValue(radio, value)
  }

  enableAllRadioValues(radio: HTMLFieldSetElement) {
    Array.from(radio.getElementsByTagName('input')).forEach((radioInput) => {
      radioInput.disabled = false
    })
    this.hideRepeatedWarning(radio, true)
  }

  hideRepeatedWarning(parentElement: HTMLElement, hide: boolean) {
    const warnings = parentElement.getElementsByClassName(
      'repeated-disabled-warning',
    )
    for (let i = 0; i < (warnings?.length || 0); i++) {
      warnings[i].toggleAttribute('hidden', hide)
    }
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
