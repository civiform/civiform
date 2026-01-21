import {atom, computed, ReadableAtom, WritableAtom} from 'nanostores'

enum ProgramType {
  DEFAULT = 'default',
  EXTERNAL = 'external',
  COMMON_INTAKE_FORM = 'common_intake_form',
}

export class AdminProgramEditStore {
  public programType: WritableAtom<ProgramType>
  public enableProgramDescription: ReadableAtom<boolean>

  constructor() {
    this.programType = atom(ProgramType.DEFAULT)
    this.enableProgramDescription = computed(
      this.programType,
      (programType) => {
        return ![ProgramType.COMMON_INTAKE_FORM, ProgramType.EXTERNAL].includes(
          programType,
        )
      },
    )
  }
}

export class AdminProgramEditFormController {
  private store: AdminProgramEditStore
  private radioProgramOptionDefault: HTMLInputElement
  private radioProgramOptionExternal: HTMLInputElement
  private radioProgramOptionCommonIntake: HTMLInputElement

  constructor(store: AdminProgramEditStore) {
    this.store = store
    this.radioProgramOptionDefault = document.getElementById(
      'default-program-option',
    ) as HTMLInputElement
    this.radioProgramOptionExternal = document.getElementById(
      'external-program-option',
    ) as HTMLInputElement
    this.radioProgramOptionCommonIntake = document.getElementById(
      'common-intake-program-option',
    ) as HTMLInputElement

    this.bindUiEvents()
    this.bindStoreEvents()
  }

  private bindUiEvents() {
    this.radioProgramOptionDefault.addEventListener('change', () => {
      if (this.radioProgramOptionDefault.checked) {
        this.store.programType.set(ProgramType.DEFAULT)
      }
    })

    this.radioProgramOptionExternal.addEventListener('change', () => {
      if (this.radioProgramOptionExternal.checked) {
        this.store.programType.set(ProgramType.EXTERNAL)
      }
    })

    this.radioProgramOptionCommonIntake.addEventListener('change', () => {
      if (this.radioProgramOptionCommonIntake.checked) {
        this.store.programType.set(ProgramType.COMMON_INTAKE_FORM)
      }
    })
  }

  private bindStoreEvents() {
    this.store.programType.subscribe((programOption) => {
      console.log(programOption)
    })

    this.store.enableProgramDescription.subscribe((enabled) => {
      const element = document.getElementById(
        'program-description-textarea',
      ) as HTMLInputElement
      element.disabled = !enabled

      document
        .querySelectorAll<HTMLInputElement>(
          'input[id^="apply-step"],textarea[id^="apply-step"]',
        )
        .forEach((el) => {
          el.disabled = !enabled
        })
    })
  }

  private showFormGroup(el: HTMLElement, enabled: boolean) {
    const formGroup = el.closest('.usa-form-group') as HTMLElement

    if (formGroup) {
      formGroup.style.display = enabled ? '' : 'none'
    }
  }

  /*
  id: program-display-name-input, type: text,
  id: program-display-short-description-textarea, type: textarea,
  id: program-description-textarea, type: textarea,

  id: program-eligibility-gating, type: radio,
  id: program-eligibility-not-gating, type: radio,

  id: checkbox-category-Childcare, type: checkbox,
  id: checkbox-category-Economic, type: checkbox,
  id: checkbox-category-Education, type: checkbox,
  id: checkbox-category-Employment, type: checkbox,
  id: checkbox-category-Food, type: checkbox,
  id: checkbox-category-General, type: checkbox,
  id: checkbox-category-Healthcare, type: checkbox,
  id: checkbox-category-Housing, type: checkbox,
  id: checkbox-category-Internet, type: checkbox,
  id: checkbox-category-Military, type: checkbox,
  id: checkbox-category-Training, type: checkbox,
  id: checkbox-category-Transportation, type: checkbox,
  id: checkbox-category-Utilities, type: checkbox,
  id: checkbox-category-Veteran, type: checkbox,

  id: program-display-mode-public, type: radio,
  id: program-display-mode-hidden, type: radio,
  id: program-display-mode-ti-only, type: radio,
  id: program-display-mode-select-ti-only, type: radio,
  id: program-display-mode-disabled, type: radio,

  id: program-external-link-input, type: text,
  id: notification-preferences-email, type: checkbox,
  id: program-display-description-textarea, type: textarea,

  id: apply-step-1-title, type: text,
  id: apply-step-1-description, type: textarea,
  id: apply-step-2-title, type: text,
  id: apply-step-2-description, type: textarea,
  id: apply-step-3-title, type: text,
  id: apply-step-3-description, type: textarea,
  id: apply-step-4-title, type: text,
  id: apply-step-4-description, type: textarea,
  id: apply-step-5-title, type: text,
  id: apply-step-5-description, type: textarea,
  id: apply-step-6-title, type: text,
  id: apply-step-6-description, type: textarea,

  id: program-confirmation-message-textarea, type: textarea
  */
}
