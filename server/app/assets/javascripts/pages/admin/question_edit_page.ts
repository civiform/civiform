import {MultiOptionQuestion} from '@/multi_option_question'

class QuestionEditPage {
  static init(): void {
    console.log('question_edit_page:init')
    // const universalToggle = assertNotNull(document.getElementById('isUniversal'))
    // const updateButton = assertNotNull(document.getElementById('updateButton'))
    // const confirmModal = assertNotNull(document.getElementById('confirm-unset-universal-modal-container')) as Modal

    // const initialUniversalToggleState = universalToggle.checked;
    //
    // updateButton.addEventListener('click', (e) => {
    //   const currentUniversalToggleState = universalToggle.checked;
    //   if (!currentUniversalToggleState && currentUniversalToggleState !== initialUniversalToggleState) {
    //     e.preventDefault()
    //     confirmModal.open()
    //   }
    // })
    //
    // confirmModal.addEventListener('modal:action-selected', (e) => {
    //   if (e.detail.action !== 'ok') {
    //     e.preventDefault()
    //   }
    // })

    // MultiOptionQuestion events
    new MultiOptionQuestion().init()
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', QuestionEditPage.init)
} else {
  QuestionEditPage.init()
}
