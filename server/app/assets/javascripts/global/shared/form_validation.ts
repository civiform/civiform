import {assertNotNull} from '@/util'

type FormControl = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement

type CheckableInput = HTMLInputElement & {type: 'checkbox' | 'radio'}

enum FormType {
  STATIC = 'static',
  DYNAMIC = 'dynamic',
}

/**
 * Provides client-side form validation using the Constraint Validation API
 *
 * Forms opt in via the `data-form-type` attribute, which can be "static"
 * or "dynamic". Dynamic forms use a MutationObserver to configure
 * controls added to the DOM after initial page load.
 */
export class FormValidation {
  private observers: Map<HTMLFormElement, MutationObserver> = new Map()

  /**
   * Discovers all forms with the `data-form-type` attribute and configures
   * validation for each.
   */
  init() {
    document
      .querySelectorAll<HTMLFormElement>('form[data-form-type]')
      .forEach((el) => this.configureForm(el))
  }

  /**
   * Configures a form by disabling native browser validation, attaching
   * event listeners to existing controls, setting up submit handling,
   * and optionally observing the DOM for dynamically added controls.
   */
  private configureForm(form: HTMLFormElement) {
    // Disable native browser validation UI
    form.noValidate = true

    // Configure existing controls
    form
      .querySelectorAll<FormControl>('input,textarea,select')
      .forEach((el) => this.configureControl(el))

    // Validate all controls on submit
    form.addEventListener('submit', this.onSubmit)

    // Set up MutationObserver for dynamic forms
    if (form.dataset.formType === FormType.DYNAMIC) {
      const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          mutation.addedNodes.forEach((node) => {
            if (this.isFormControl(node)) {
              this.configureControl(node)
            }

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

  /**
   * Validate the form on submit, if invalid focuses on the first invalid control
   */
  private onSubmit = (ev: SubmitEvent) => {
    const form = ev.target as HTMLFormElement
    let isValid = true

    form
      .querySelectorAll<FormControl>('input,textarea,select')
      .forEach((el) => {
        if (this.isCheckableInput(el) && !this.validateFieldset(el)) {
          isValid = false
        } else if (!this.isCheckableInput(el) && !this.validateControl(el)) {
          isValid = false
        }
      })

    if (!isValid) {
      ev.preventDefault()
      form
        .querySelector('.usa-form-group--error')
        ?.querySelector<FormControl>('input,textarea,select')
        ?.focus()
    }
  }

  /**
   * Check that the element is an expected type
   */
  private isFormControl(node: Node): node is FormControl {
    return (
      node instanceof HTMLInputElement ||
      node instanceof HTMLSelectElement ||
      node instanceof HTMLTextAreaElement
    )
  }

  /**
   * Check that the element is either a checkbox or radio button.
   */
  private isCheckableInput(
    formControl: FormControl,
  ): formControl is CheckableInput {
    return (
      formControl instanceof HTMLInputElement &&
      ['checkbox', 'radio'].includes(formControl.type)
    )
  }

  /**
   * Bind the control to the type appropriate change event
   */
  private configureControl(formControl: FormControl) {
    const eventType = this.isCheckableInput(formControl) ? 'change' : 'blur'
    formControl.addEventListener(eventType, this.validate)
  }

  /**
   * Callback trigger by a change in the control
   */
  private validate = (ev: Event) => {
    const target = ev.target as FormControl

    if (this.isCheckableInput(target)) {
      this.validateFieldset(target)
    } else {
      this.validateControl(target)
    }
  }

  /**
   * Validates a control (excluding checkbox/radio buttons) and configures
   * appropriately for if there is an error or not.
   *
   * @returns true if the control is valid, false otherwise.
   */
  private validateControl(formControl: FormControl): boolean {
    if (!formControl.willValidate) {
      return true
    }

    const formGroup = assertNotNull(formControl.closest('.usa-form-group'))
    const errorElement = assertNotNull(
      formGroup.querySelector<HTMLSpanElement>('.usa-error-message'),
    )
    const helpTextId =
      formGroup.querySelector<HTMLDivElement>('.usa-hint')?.id ?? ''

    if (formControl.validity.valid) {
      formGroup.classList.remove('usa-form-group--error')
      errorElement.textContent = ''
      errorElement.style.display = 'none'
      this.setAriaValid(formControl, helpTextId)
      return true
    } else {
      formGroup.classList.add('usa-form-group--error')
      errorElement.textContent = this.getErrorMessage(formControl)
      errorElement.style.display = 'block'
      this.setAriaInvalid(formControl, errorElement.id, helpTextId)
      return false
    }
  }

  /**
   * Validates a checkbox/radio button group and configures
   * appropriately for if there is an error or not.
   *
   * @returns true if the fieldset is valid, false otherwise.
   */
  private validateFieldset(triggerElement: HTMLInputElement): boolean {
    if (!triggerElement.willValidate) {
      return true
    }

    const fieldset = assertNotNull(triggerElement.closest('fieldset'))
    const inputs = fieldset.querySelectorAll<HTMLInputElement>(
      'input[type="checkbox"], input[type="radio"]',
    )
    const formGroup = assertNotNull(fieldset.closest('.usa-form-group'))

    const hasRequired = Array.from(inputs).some((input) => input.required)

    if (!hasRequired) {
      return true
    }

    const errorElement = assertNotNull(
      formGroup.querySelector<HTMLSpanElement>('.usa-error-message'),
    )
    const helpTextId =
      formGroup.querySelector<HTMLDivElement>('.usa-hint')?.id ?? ''
    const hasChecked = Array.from(inputs).some((input) => input.checked)

    if (hasChecked) {
      formGroup.classList.remove('usa-form-group--error')
      errorElement.textContent = ''
      errorElement.style.display = 'none'
      inputs.forEach((input) => this.setAriaValid(input, helpTextId))
      return true
    } else {
      formGroup.classList.add('usa-form-group--error')
      errorElement.textContent =
        fieldset.dataset.requiredMessage || 'This field is required'
      errorElement.style.display = 'block'
      inputs.forEach((input) =>
        this.setAriaInvalid(input, errorElement.id, helpTextId),
      )
      return false
    }
  }

  /**
   * Sets aria attributes on a form control to indicate a valid state.
   * Removes `aria-invalid` and restores `aria-describedby` to reference
   * only the help text, if present.
   */
  private setAriaValid(element: FormControl, helpTextId: string) {
    element.removeAttribute('aria-invalid')
    if (helpTextId === '') {
      element.removeAttribute('aria-describedby')
    } else {
      element.setAttribute('aria-describedby', helpTextId)
    }
  }

  /**
   * Sets aria attributes on a form control to indicate an invalid state.
   * Adds `aria-invalid` and sets `aria-describedby` to reference both the
   * error message and help text elements.
   */
  private setAriaInvalid(
    element: FormControl,
    errorElementId: string,
    helpTextId: string,
  ) {
    element.setAttribute('aria-invalid', 'true')
    element.setAttribute(
      'aria-describedby',
      `${errorElementId} ${helpTextId}`.trim(),
    )
  }

  /**
   * Returns the custom error message for a form control based on its validity state. If
   * the data-* attribute isn't found falls back to a context specific default, followed
   * by the browser default text.
   *
   * The data-* attributes should be typically be present as they will contain
   * our custom localized strings.
   */
  private getErrorMessage(field: FormControl): string {
    const validity = field.validity

    if (validity.valueMissing) {
      return field.dataset.requiredMessage || 'This field is required'
    } else if (validity.patternMismatch) {
      return field.dataset.patternMessage || 'Invalid format'
    } else if (validity.typeMismatch && field.type === 'email') {
      return field.dataset.emailMessage || 'Please enter a valid email'
    } else if (validity.typeMismatch && field.type === 'url') {
      return field.dataset.urlMessage || 'Please enter a valid url'
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

    // Final fallback to browser default text
    return field.validationMessage
  }
}
