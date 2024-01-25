import {assertNotNull} from './util'

class AdminProgramImage {
  private static IMAGE_DESCRIPTION_FORM_ID = 'image-description-form'
  private static IMAGE_FILE_UPLOAD_FORM_ID = 'image-file-upload-form'

  static attachEventListenersToDescriptionForm() {
    const descriptionForm = document.getElementById(
      AdminProgramImage.IMAGE_DESCRIPTION_FORM_ID,
    )
    if (descriptionForm) {
      descriptionForm.addEventListener(
        'input',
        AdminProgramImage.changeSubmitDescriptionState,
      )
    }
  }

  static changeSubmitDescriptionState() {
    const descriptionForm = assertNotNull(
      document.getElementById(AdminProgramImage.IMAGE_DESCRIPTION_FORM_ID),
    )
    const descriptionInput = assertNotNull(
      descriptionForm.querySelector<HTMLInputElement>(
        'input[name="summaryImageDescription"]',
      ),
    )
    const submitButton = assertNotNull(
      document.querySelector(
        'button[form=' +
          AdminProgramImage.IMAGE_DESCRIPTION_FORM_ID +
          '][type="submit"]',
      ),
    )

    if (descriptionInput.value !== descriptionInput.defaultValue) {
      submitButton.removeAttribute('disabled')
    } else {
      submitButton.setAttribute('disabled', '')
    }
  }

  static attachEventListenersToImageForm() {
    const imageForm = document.getElementById(
      AdminProgramImage.IMAGE_FILE_UPLOAD_FORM_ID,
    )
    if (imageForm) {
      imageForm.addEventListener(
        'input',
        AdminProgramImage.changeSubmitImageState,
      )
    }
  }

  static changeSubmitImageState() {
    const imageForm = assertNotNull(
      document.getElementById(AdminProgramImage.IMAGE_FILE_UPLOAD_FORM_ID),
    )
    const imageInput = assertNotNull(
      imageForm.querySelector<HTMLInputElement>('input[type=file]'),
    )
    const submitButton = assertNotNull(
      document.querySelector(
        'button[form=' +
          AdminProgramImage.IMAGE_FILE_UPLOAD_FORM_ID +
          '][type="submit"]',
      ),
    )

    if (imageInput.value !== '') {
      submitButton.removeAttribute('disabled')
    } else {
      submitButton.setAttribute('disabled', '')
    }
  }
}

export function init() {
  AdminProgramImage.attachEventListenersToDescriptionForm()
  AdminProgramImage.attachEventListenersToImageForm()
}
