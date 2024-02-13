import {addEventListenerToElements, assertNotNull} from './util'

const UPLOAD_ATTR = 'data-upload-text'

export function init() {
  // Don't add extra logic if we don't have a block form with a
  // file upload question.
  const blockForm = document.getElementById('cf-block-form')
  if (!blockForm) {
    return
  }
  const fileUploadQuestion = blockForm.querySelector('.cf-question-fileupload')
  if (!fileUploadQuestion) {
    // If there's no file upload question on the page, don't add extra logic.
    return
  }

  blockForm.addEventListener('submit', (event) => {
    // Prevent submission of a file upload form if no file has been
    // selected. Note: For optional file uploads, a distinct skip button
    // is shown.
    if (!validateFileUploadQuestion(blockForm)) {
      event.preventDefault()
      return false
    }
    return true
  })

    addEventListenerToElements(
      '.file-upload-action-button',
      'click',
      (e: Event) => {
        console.log('click')
        // TODO: Flag guard stuff (don't apply attributes if flag off?)
        const redirectWithFile = assertNotNull((e.currentTarget as HTMLElement).dataset
          .redirectWithFile)
        const redirectWithoutFile = assertNotNull((e.currentTarget as HTMLElement).dataset
          .redirectWithoutFile)
        console.log('redirectWith=' + redirectWithFile)
        console.log('redirectWi/o=' + redirectWithoutFile)

        const fileInput = assertNotNull(
          blockForm.querySelector<HTMLInputElement>('input[type=file]'),
        )

        if (fileInput.value != '') {
          console.log('has file so changing redirect')
          assertNotNull(
            blockForm.querySelector<HTMLInputElement>(
              'input[name="success_action_redirect"]',
            )
          ).value = redirectWithFile
          // We want the submit to fire so don't call #stopPropagation?
          return true
        } else {
          window.location.href = redirectWithoutFile
          e.stopPropagation()
        }
      },
    )

      // Prevent submission of a file upload form if no file has been
      // selected. Note: For optional file uploads, a distinct skip button
      // is shown.
    blockForm.addEventListener('submit', (event) => {
      console.log('submit. id=' + (event.target! as Element).id)
      console.log(
        'submitter=' + ((event as SubmitEvent).submitter as Element).id,
      )

      const successActionRedirect = assertNotNull(
        blockForm.querySelector<HTMLInputElement>(
          'input[name="success_action_redirect"]',
        ),
      ).value
      console.log('redirect input = ' + successActionRedirect)
      if (
        ((event as SubmitEvent).submitter as Element).id === 'review-button-id'
      ) {
        const applicantRequestedAction = 'REVIEW_PAGE'
        const newSuccessActionRedirect =
          successActionRedirect.substring(
            0,
            successActionRedirect.lastIndexOf('/') + 1,
          ) + applicantRequestedAction
        console.log('new redirect=' + newSuccessActionRedirect)
        assertNotNull(
          blockForm.querySelector<HTMLInputElement>(
            'input[name="success_action_redirect"]',
          ),
        ).value = newSuccessActionRedirect
        //return true
      }

      if (!validateFileUploadQuestions(blockForm)) {
        event.preventDefault()
        return false
      }
      return true
    })

      const uploadedDivs = blockForm.querySelectorAll(`[${UPLOAD_ATTR}]`)
      if (uploadedDivs.length) {
        const uploadedDiv = uploadedDivs[0]
        const uploadText = assertNotNull(uploadedDiv.getAttribute(UPLOAD_ATTR))

        blockForm.addEventListener('change', (event) => {
          const files = (event.target! as HTMLInputElement).files
          const file = assertNotNull(files)[0]
          uploadedDiv.innerHTML = uploadText.replace('{0}', file.name)
          validateFileUploadQuestion(blockForm)
          })
          }
  }
}

/**
 * Validates the file upload question, showing an error if no file has been uploaded
 * and hiding the error otherwise.
 *
 * @returns true if a file was uploaded and false otherwise.
 */
function validateFileUploadQuestion(blockForm: Element): boolean {
  // Note: Currently, a file upload question must be on a screen by itself with no
  // other questions (file upload or otherwise). This method implementation assumes
  // that there is a single question on the page. If we later allow file upload
  // questions to be with other questions, we'll need to update this method.
  const fileInput = assertNotNull(
    blockForm.querySelector<HTMLInputElement>('input[type=file]'),
  )
  const isFileUploaded = fileInput.value != ''

  const errorDiv = blockForm.querySelector('.cf-fileupload-error')
  if (!errorDiv) {
    return isFileUploaded
  }

  if (isFileUploaded) {
    hideFileSelectionError(errorDiv, fileInput)
  } else {
    showFileSelectionError(errorDiv, fileInput)
  }
  return isFileUploaded
}

/**
 * Shows a "Please select a file" error. Used when no file was uploaded
 * but the user wants to continue to the next page.
 */
function showFileSelectionError(
  errorDiv: Element,
  fileInput: HTMLInputElement,
) {
  errorDiv.classList.remove('hidden')
  // Add ariaLive label so error is announced to screen reader.
  errorDiv.ariaLive = 'polite'
  fileInput.setAttribute('aria-invalid', 'true')

  const errorId = errorDiv.getAttribute('id')
  if (!errorId) {
    return
  }

  const ariaDescribedBy = fileInput.getAttribute('aria-describedby') ?? ''
  if (!ariaDescribedBy.includes(errorId)) {
    fileInput.setAttribute('aria-describedby', `${errorId} ${ariaDescribedBy}`)
  }
}

/** Hides the "Please select a file" error. */
function hideFileSelectionError(
  errorDiv: Element,
  fileInput: HTMLInputElement,
) {
  errorDiv.classList.add('hidden')
  fileInput.removeAttribute('aria-invalid')

  const errorId = errorDiv.getAttribute('id')
  if (!errorId) {
    return
  }

  const ariaDescribedBy = fileInput.getAttribute('aria-describedby') ?? ''
  if (ariaDescribedBy.includes(errorId)) {
    const ariaDescribedByWithoutError = ariaDescribedBy.replace(errorId, '')
    fileInput.setAttribute('aria-describedby', ariaDescribedByWithoutError)
  }
}
