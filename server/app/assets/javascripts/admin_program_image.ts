import {assertNotNull} from './util'
import {isFileTooLarge} from './file_upload_util'

/** Scripts for controlling the admin program image upload page. */
class AdminProgramImage {
  // These values should be kept in sync with views/admin/programs/ProgramImageView.java.
  private static IMAGE_DESCRIPTION_FORM_ID = 'image-description-form'
  private static IMAGE_FILE_UPLOAD_FORM_ID = 'image-file-upload-form'
  // This should be kept in sync with views/fileupload/FileUploadViewStrategy#createFileTooLargeError.
  private static FILE_TOO_LARGE_ID = 'cf-fileupload-too-large-error'

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
      imageForm.addEventListener('input', AdminProgramImage.onImageFileChanged)
    }
  }

  static onImageFileChanged() {
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
    const fileTooLargeError = assertNotNull(
      document.getElementById(AdminProgramImage.FILE_TOO_LARGE_ID),
    ) as HTMLElement

    if (imageInput.value == '') {
      // Prevent submission and hide the too-large error if no file is uploaded
      submitButton.setAttribute('disabled', '')
      fileTooLargeError.hidden = true
      return
    }

    const fileTooLarge = isFileTooLarge(imageInput)
    if (fileTooLarge) {
      // Prevent submission and show the too-large error if the file was too large
      submitButton.setAttribute('disabled', '')
      fileTooLargeError.hidden = false
    } else {
      // Allow submission and hide the too-large error if the file is small enough
      submitButton.removeAttribute('disabled')
      fileTooLargeError.hidden = true
    }
  }
}

export function init() {
  AdminProgramImage.attachEventListenersToDescriptionForm()
  AdminProgramImage.attachEventListenersToImageForm()
}
