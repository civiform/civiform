import {describe, it, expect, beforeEach, vi} from 'vitest'
import {FormValidation} from '@/global/shared/form_validation'

/**
 * Initializes a FormValidation instance. This is not done in the beforeEach
 * because it needs to happen after the form exists.
 */
function initFormValidation() {
  const formValidation = new FormValidation()
  formValidation.init()
}

/**
 * Creates a form group formatted for the expected uswds structure
 */
function createFormGroup(
  inputHtml: string,
  options?: {helpTextId?: string},
): string {
  const helpText = options?.helpTextId
    ? `<div class="usa-hint" id="${options.helpTextId}">Help text</div>`
    : ''

  return `
    <div class="usa-form-group">
      <label class="usa-label">Label</label>
      ${helpText}
      <span class="usa-error-message" id="error-${Math.random().toString(36).slice(2)}" style="display:none"></span>
      ${inputHtml}
    </div>
  `
}

/**
 * Creates a fieldset formatted for the expected uswds structure
 */
function createFieldsetGroup(
  type: 'checkbox' | 'radio',
  options: {required?: boolean; requiredMessage?: string; count?: number},
): string {
  const count = options.count ?? 3
  const inputs = Array.from({length: count}, (_, i) => {
    return `<input type="${type}" name="group" value="${i}" ${options.required ? 'required' : ''} />`
  }).join('\n')

  const dataAttr = options.requiredMessage
    ? `data-required-message="${options.requiredMessage}"`
    : ''

  return `
    <div class="usa-form-group">
      <fieldset ${dataAttr}>
        <legend>Pick one</legend>
        <span class="usa-error-message" id="fieldset-error-${Math.random().toString(36).slice(2)}" style="display:none"></span>
        ${inputs}
      </fieldset>
    </div>
  `
}

/**
 * Build a form
 */
function createForm(
  innerHTML: string,
  formType: 'static' | 'dynamic' = 'static',
): HTMLFormElement {
  const form = document.createElement('form')
  form.setAttribute('data-form-type', formType)

  form.innerHTML = innerHTML
  document.body.appendChild(form)
  return form
}

/** Triggers a blur event on an element. */
function blur(el: HTMLElement) {
  el.dispatchEvent(new Event('blur', {bubbles: true}))
}

/** Triggers a change event on an element. */
function change(el: HTMLElement) {
  el.dispatchEvent(new Event('change', {bubbles: true}))
}

/** Triggers a submit event on a form and returns the event. */
function submit(form: HTMLFormElement): SubmitEvent {
  const ev = new SubmitEvent('submit', {bubbles: true, cancelable: true})
  form.dispatchEvent(ev)
  return ev
}

/** Returns the form group and error element for a given control. */
function getFormGroupElements(control: Element) {
  const formGroup = control.closest('.usa-form-group')!
  const errorEl =
    formGroup.querySelector<HTMLSpanElement>('.usa-error-message')!
  return {formGroup, errorEl}
}

describe('FormValidation', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  describe('initialization', () => {
    it('sets noValidate on configured forms', () => {
      const form = createForm(createFormGroup('<input type="text" />'))
      initFormValidation()
      expect(form.noValidate).toBe(true)
    })

    it('ignores forms without data-form-type', () => {
      const form = document.createElement('form')
      form.innerHTML = createFormGroup('<input type="text" required />')
      document.body.appendChild(form)

      initFormValidation()

      // noValidate should not have been set
      expect(form.noValidate).toBe(false)
    })
  })

  describe('text input validation', () => {
    it('shows error on blur when required field is empty', () => {
      const form = createForm(createFormGroup('<input type="text" required />'))
      initFormValidation()

      const input = form.querySelector('input')!
      blur(input)

      const {formGroup, errorEl} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(true)
      expect(errorEl.textContent).toBe('This field is required')
      expect(errorEl.style.display).toBe('block')
    })

    it('clears error on blur when required field has a value', () => {
      const form = createForm(createFormGroup('<input type="text" required />'))
      initFormValidation()

      const input = form.querySelector('input')!

      // First trigger error
      blur(input)
      expect(
        input
          .closest('.usa-form-group')!
          .classList.contains('usa-form-group--error'),
      ).toBe(true)

      // Then fill in value and blur again
      input.value = 'hello'
      blur(input)

      const {formGroup, errorEl} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(false)
      expect(errorEl.textContent).toBe('')
      expect(errorEl.style.display).toBe('none')
    })

    it('does not validate inputs without constraints', () => {
      const form = createForm(createFormGroup('<input type="text" />'))
      initFormValidation()

      const input = form.querySelector('input')!
      blur(input)

      const {formGroup} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(false)
    })
  })

  describe('custom error messages', () => {
    it('uses data-required-message when set', () => {
      const form = createForm(
        createFormGroup(
          '<input type="text" required data-required-message="Very required" />',
        ),
      )
      initFormValidation()

      const input = form.querySelector('input')!
      blur(input)

      const {errorEl} = getFormGroupElements(input)
      expect(errorEl.textContent).toBe('Very required')
    })

    it('uses data-pattern-message for pattern mismatch', () => {
      const form = createForm(
        createFormGroup(
          '<input type="text" pattern="[0-9]+" value="abc" data-pattern-message="Numbers only" />',
        ),
      )
      initFormValidation()

      const input = form.querySelector('input')!
      blur(input)

      const {errorEl} = getFormGroupElements(input)
      expect(errorEl.textContent).toBe('Numbers only')
    })

    it('uses default message when no data attribute is set', () => {
      const form = createForm(createFormGroup('<input type="text" required />'))
      initFormValidation()

      const input = form.querySelector('input')!
      blur(input)

      const {errorEl} = getFormGroupElements(input)
      expect(errorEl.textContent).toBe('This field is required')
    })
  })

  describe('select validation', () => {
    it('validates required select on blur', () => {
      const form = createForm(
        createFormGroup(`
          <select required>
            <option value="">Choose</option>
            <option value="a">A</option>
          </select>
        `),
      )
      initFormValidation()

      const select = form.querySelector('select')!
      blur(select)

      const {formGroup} = getFormGroupElements(select)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(true)
    })
  })

  describe('textarea validation', () => {
    it('validates required textarea on blur', () => {
      const form = createForm(createFormGroup('<textarea required></textarea>'))
      initFormValidation()

      const textarea = form.querySelector('textarea')!
      blur(textarea)

      const {formGroup} = getFormGroupElements(textarea)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(true)
    })
  })

  describe('fieldset validation', () => {
    it('shows error when no required checkbox is checked', () => {
      const form = createForm(createFieldsetGroup('checkbox', {required: true}))
      initFormValidation()

      const input = form.querySelector('input')!
      change(input)

      const {formGroup} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(true)
    })

    it('clears error when a required checkbox is checked', () => {
      const form = createForm(createFieldsetGroup('checkbox', {required: true}))
      initFormValidation()

      const input = form.querySelector('input')!

      // Trigger error
      change(input)
      expect(
        input
          .closest('.usa-form-group')!
          .classList.contains('usa-form-group--error'),
      ).toBe(true)

      // Check and re-validate
      input.checked = true
      change(input)

      const {formGroup} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(false)
    })

    it('shows error when no required radio is selected', () => {
      const form = createForm(createFieldsetGroup('radio', {required: true}))
      initFormValidation()

      const input = form.querySelector('input')!
      change(input)

      const {formGroup} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(true)
    })

    it('clears error when a required radio is selected', () => {
      const form = createForm(createFieldsetGroup('radio', {required: true}))
      initFormValidation()

      const input = form.querySelector('input')!

      change(input)
      expect(
        input
          .closest('.usa-form-group')!
          .classList.contains('usa-form-group--error'),
      ).toBe(true)

      input.checked = true
      change(input)

      const {formGroup} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(false)
    })

    it('uses custom data-required-message on fieldset', () => {
      const form = createForm(
        createFieldsetGroup('checkbox', {
          required: true,
          requiredMessage: 'Pick at least one',
        }),
      )
      initFormValidation()

      const input = form.querySelector('input')!
      change(input)

      const {errorEl} = getFormGroupElements(input)
      expect(errorEl.textContent).toBe('Pick at least one')
    })

    it('does not show error for non-required fieldsets', () => {
      const form = createForm(
        createFieldsetGroup('checkbox', {required: false}),
      )
      initFormValidation()

      const input = form.querySelector('input')!
      change(input)

      const {formGroup} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(false)
    })
  })

  describe('aria attributes', () => {
    it('sets aria-invalid on invalid control', () => {
      const form = createForm(createFormGroup('<input type="text" required />'))
      initFormValidation()

      const input = form.querySelector('input')!
      blur(input)

      expect(input.getAttribute('aria-invalid')).toBe('true')
    })

    it('removes aria-invalid when control becomes valid', () => {
      const form = createForm(createFormGroup('<input type="text" required />'))
      initFormValidation()

      const input = form.querySelector('input')!
      blur(input)
      expect(input.getAttribute('aria-invalid')).toBe('true')

      input.value = 'hello'
      blur(input)
      expect(input.hasAttribute('aria-invalid')).toBe(false)
    })

    it('sets aria-describedby referencing error and help text', () => {
      const form = createForm(
        createFormGroup('<input type="text" required />', {
          helpTextId: 'hint-1',
        }),
      )
      initFormValidation()

      const input = form.querySelector('input')!
      blur(input)

      const {errorEl} = getFormGroupElements(input)
      expect(input.getAttribute('aria-describedby')).toBe(
        `${errorEl.id} hint-1`,
      )
    })

    it('sets aria-describedby to only help text when valid', () => {
      const form = createForm(
        createFormGroup('<input type="text" required />', {
          helpTextId: 'hint-1',
        }),
      )
      initFormValidation()

      const input = form.querySelector('input')!
      input.value = 'hello'
      blur(input)

      expect(input.getAttribute('aria-describedby')).toBe('hint-1')
    })

    it('removes aria-describedby when valid and no help text', () => {
      const form = createForm(createFormGroup('<input type="text" required />'))
      initFormValidation()

      const input = form.querySelector('input')!
      input.value = 'hello'
      blur(input)

      expect(input.hasAttribute('aria-describedby')).toBe(false)
    })

    it('sets aria-invalid on all inputs in an invalid fieldset', () => {
      const form = createForm(
        createFieldsetGroup('checkbox', {required: true, count: 3}),
      )
      initFormValidation()

      const inputs = form.querySelectorAll('input')
      change(inputs[0])

      inputs.forEach((input) => {
        expect(input.getAttribute('aria-invalid')).toBe('true')
      })
    })

    it('removes aria-invalid from all inputs when fieldset becomes valid', () => {
      const form = createForm(
        createFieldsetGroup('checkbox', {required: true, count: 3}),
      )
      initFormValidation()

      const inputs = form.querySelectorAll('input')

      // Trigger error
      change(inputs[0])
      inputs.forEach((input) => {
        expect(input.getAttribute('aria-invalid')).toBe('true')
      })

      // Fix
      inputs[1].checked = true
      change(inputs[1])
      inputs.forEach((input) => {
        expect(input.hasAttribute('aria-invalid')).toBe(false)
      })
    })
  })

  describe('submit validation', () => {
    it('prevents submission when a required field is empty', () => {
      const form = createForm(createFormGroup('<input type="text" required />'))
      initFormValidation()

      const ev = submit(form)
      expect(ev.defaultPrevented).toBe(true)
    })

    it('allows submission when all fields are valid', () => {
      const form = createForm(
        createFormGroup('<input type="text" required value="hello" />'),
      )
      initFormValidation()

      const ev = submit(form)
      expect(ev.defaultPrevented).toBe(false)
    })

    it('prevents submission when required checkbox group has none checked', () => {
      const form = createForm(createFieldsetGroup('checkbox', {required: true}))
      initFormValidation()

      const ev = submit(form)
      expect(ev.defaultPrevented).toBe(true)
    })

    it('allows submission when required checkbox group has one checked', () => {
      const form = createForm(createFieldsetGroup('checkbox', {required: true}))
      initFormValidation()

      form.querySelector('input')!.checked = true

      const ev = submit(form)
      expect(ev.defaultPrevented).toBe(false)
    })

    it('shows errors on all invalid fields on submit', () => {
      const html =
        createFormGroup('<input type="text" required />') +
        createFormGroup('<input type="email" required />')

      const form = createForm(html)
      initFormValidation()

      submit(form)

      form.querySelectorAll('.usa-form-group').forEach((fg) => {
        expect(fg.classList.contains('usa-form-group--error')).toBe(true)
      })
    })

    it('focuses the first invalid control on submit', () => {
      const html =
        createFormGroup('<input id="first" type="text" required />') +
        createFormGroup('<input id="second" type="text" required />')

      const form = createForm(html)
      initFormValidation()

      const focusSpy = vi.spyOn(
        form.querySelector<HTMLInputElement>('#first')!,
        'focus',
      )

      submit(form)

      expect(focusSpy).toHaveBeenCalled()
    })
  })

  describe('dynamic forms', () => {
    it('configures controls added after initialization', async () => {
      const form = createForm('', 'dynamic')
      initFormValidation()

      const div = document.createElement('div')
      div.innerHTML = createFormGroup('<input type="text" required />')
      form.appendChild(div)

      // Wait for MutationObserver to fire. Yes, the `0` here looks weird, but
      // it is enough to allow the MutationObserver to trigger. For more details
      // look into Javascript Microtasks vs Macrotasks.
      await new Promise((r) => setTimeout(r, 0))

      const input = form.querySelector('input')!
      blur(input)

      const {formGroup} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(true)
    })

    it('does not observe static forms for mutations', () => {
      const form = createForm('', 'static')
      initFormValidation()

      const div = document.createElement('div')
      div.innerHTML = createFormGroup('<input type="text" required />')
      form.appendChild(div)

      const input = form.querySelector('input')!
      blur(input)

      // Should not have been configured, so no error class added
      const {formGroup} = getFormGroupElements(input)
      expect(formGroup.classList.contains('usa-form-group--error')).toBe(false)
    })
  })
})
