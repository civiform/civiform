import {Modal} from '@/components/shared/modal'
import {MultiOptionQuestion} from '@/multi_option_question'
import {assertNotNull} from '@/util'

class QuestionEditPage {
  static init(): void {
    // MultiOptionQuestion events
    new MultiOptionQuestion().init()

    QuestionEditPage.initUnsetUniversalConfirmation()
  }

  /**
   * Asks for confirmation before saving when the universal toggle goes from
   * on to off.
   */
  private static initUnsetUniversalConfirmation(): void {
    // The universal toggle is only rendered for question types with settings.
    const universalToggle = document.getElementById(
      'universal-toggle-input',
    ) as HTMLInputElement | null
    if (universalToggle === null) {
      return
    }

    // The unset-confirmation modal only exists on the edit page; the
    // new-question page renders the universal toggle without it.
    const confirmModal = document.getElementById(
      'confirm-unset-universal-modal-container',
    ) as Modal | null

    if (confirmModal === null) {
      return
    }

    const form = assertNotNull(
      document.getElementById('full-edit-form'),
    ) as HTMLFormElement
    const updateButton = assertNotNull(document.getElementById('updateButton'))

    const initiallyUniversal = universalToggle.checked

    updateButton.addEventListener('click', (e) => {
      if (initiallyUniversal && !universalToggle.checked) {
        e.preventDefault()
        confirmModal.open()
      }
    })

    // The modal lives outside the form and DOMPurify strips the form=""
    // attribute when wc-modal rebuilds its content, so the confirm button
    // can't submit natively.
    confirmModal.addEventListener('modal:action-selected', (e) => {
      if (e.detail.action === 'ok') {
        form.requestSubmit()
      }
    })
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', QuestionEditPage.init)
} else {
  QuestionEditPage.init()
}
