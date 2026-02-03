import {assertNotNull} from '@/util'

type FormControl = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement

type CheckableInput = HTMLInputElement & {type: 'checkbox' | 'radio'}

enum FormType {
  STATIC = 'static',
  DYNAMIC = 'dynamic',
}

// type Result<TResult, TError> = { success: true, value: TResult } | { success: false, error: TError }

export class FormValidation {
  private observers: Map<HTMLFormElement, MutationObserver> = new Map()

  constructor() {
    document
      .querySelectorAll<HTMLFormElement>('form[data-form-type]')
      .forEach((el) => this.configureForm(el))
  }

  private configureForm(form: HTMLFormElement) {
    // Configure existing controls
    form
      .querySelectorAll<FormControl>('input,textarea,select')
      .forEach((el) => this.configureControl(el))

    // Set up MutationObserver for dynamic forms
    if (form.dataset.formType === FormType.DYNAMIC) {
      const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          mutation.addedNodes.forEach((node) => {
            // Check if the added node itself is a form control
            if (this.isFormControl(node)) {
              this.configureControl(node)
            }

            // Check if the added node contains form controls
            if (node instanceof HTMLElement) {
              node
                .querySelectorAll<FormControl>('input,textarea,select')
                .forEach((el) => this.configureControl(el))
            }
          })
        })
      })

      observer.observe(form, {
        childList: true,
        subtree: true,
      })

      this.observers.set(form, observer)
    }
  }

  // private isFormControl(node: Node): node is FormControl {
  //   return [HTMLInputElement, HTMLSelectElement, HTMLTextAreaElement].some(
  //     (type) => node instanceof type,
  //   )
  // }

  private isFormControl(node: Node): node is FormControl {
    return (
      node instanceof HTMLInputElement ||
      node instanceof HTMLSelectElement ||
      node instanceof HTMLTextAreaElement
    )
  }

  private isCheckableInput(
    formControl: FormControl,
  ): formControl is CheckableInput {
    return (
      formControl instanceof HTMLInputElement &&
      ['checkbox', 'radio'].includes(formControl.type)
    )
  }

  private configureControl(formControl: FormControl) {
    const eventType = this.isCheckableInput(formControl) ? 'change' : 'blur'
    formControl.addEventListener(eventType, this.validate)
  }

  private validate = (ev: Event) => {
    const target = ev.target as FormControl

    // For radio buttons and checkboxes in a fieldset, validate the fieldset
    if (this.isCheckableInput(target)) {
      this.validateFieldset(target)
    } else {
      this.validateControl(target)
    }
  }

  private validateControl(formControl: FormControl) {
    // Only validate if the control has validation constraints
    if (!formControl.willValidate) {
      return
    }

    // Find the nearest usa-form-group element
    const formGroup = assertNotNull(formControl.closest('.usa-form-group'))
    const errorElement =
      formGroup.querySelector<HTMLSpanElement>('.usa-error-message')

    if (!errorElement) {
      console.warn('Missing errorElement span')
      return
    }

    if (formControl.validity.valid) {
      formGroup.classList.remove('usa-form-group--error')
      errorElement.textContent = ''
      errorElement.style.display = 'none'
    } else {
      formGroup.classList.add('usa-form-group--error')
      errorElement.textContent = this.getErrorMessage(formControl)
      errorElement.style.display = 'block'
    }
  }

  private validateFieldset(triggerElement: HTMLInputElement) {
    if (!triggerElement.willValidate) {
      return
    }

    const fieldset = triggerElement.closest('fieldset')!

    const inputs = fieldset.querySelectorAll<HTMLInputElement>(
      'input[type="checkbox"], input[type="radio"]',
    )
    const formGroup = assertNotNull(fieldset.closest('.usa-form-group'))

    // const hasRequired2 = [...inputs].some((input) => input.required)

    // Check if any checkbox/radio in the fieldset is required
    const hasRequired = Array.from(inputs).some((input) => input.required)

    if (!hasRequired) {
      return
    }

    const errorElement = assertNotNull(
      formGroup.querySelector<HTMLSpanElement>('.usa-error-message'),
    )

    if (!errorElement) {
      console.warn('Missing errorElement span')
      return
    }

    // Check if at least one is checked
    const hasChecked = Array.from(inputs).some((input) => input.checked)

    if (hasChecked) {
      formGroup.classList.remove('usa-form-group--error')
      errorElement.textContent = ''
      errorElement.style.display = 'none'
    } else {
      formGroup.classList.add('usa-form-group--error')
      errorElement.textContent =
        fieldset.dataset.requiredMessage || 'Please select at least one option.'
      errorElement.style.display = 'block'
    }
  }

  private getErrorMessage(field: FormControl): string {
    const validity = field.validity

    // Check each validation state and use corresponding data attribute
    if (validity.valueMissing) {
      return field.dataset.requiredMessage || 'This field is required'
    } else if (validity.patternMismatch) {
      return field.dataset.patternMessage || 'Invalid format'
    } else if (validity.typeMismatch && field.type === 'email') {
      return field.dataset.emailMessage || 'Please enter a valid email'
    } else if (validity.tooLong && 'maxLength' in field) {
      return (
        field.dataset.maxlengthMessage ||
        `Maximum ${field.maxLength} characters allowed`
      )
    } else if (validity.tooShort && 'minLength' in field) {
      return (
        field.dataset.minlengthMessage ||
        `Minimum ${field.minLength} characters required`
      )
    } else if (validity.rangeOverflow && 'max' in field) {
      return field.dataset.maxMessage || `Value must be ${field.max} or less`
    } else if (validity.rangeUnderflow && 'min' in field) {
      return field.dataset.minMessage || `Value must be ${field.min} or more`
    }

    return 'ERROR'
  }
}
