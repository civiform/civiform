import {addEventListenerToElements, assertNotNull} from './util'

export function init() {
  console.log('initting program image js')
  const descriptionForm = document.getElementById('image-description-form')
  if (descriptionForm) {
    console.log('doing description form')
    descriptionForm.addEventListener('input', changeSubmitDescriptionState)
  }

  const imageForm = document.getElementById('image-file-upload-form')
  if (imageForm) {
    imageForm.addEventListener('input', changeSubmitImageState)
  }
}

function changeSubmitDescriptionState() {
  const descriptionForm = assertNotNull(
    document.getElementById('image-description-form'),
  )
  const descriptionInput = assertNotNull(
    descriptionForm.querySelector<HTMLInputElement>('#image-description-input'),
  )

  const submitButton = assertNotNull(
    document.getElementById('image-description-submit-button'),
  )

  if (descriptionInput.value !== descriptionInput.defaultValue) {
    submitButton.removeAttribute('disabled')
  } else {
    submitButton.setAttribute('disabled', '')
  }
}

function changeSubmitImageState() {
  const imageForm = assertNotNull(
    document.getElementById('image-file-upload-form'),
  )
  const imageInput = assertNotNull(
    imageForm.querySelector<HTMLInputElement>('#image-file-upload-input'),
  )

  const submitButton = assertNotNull(
    document.getElementById('image-file-upload-submit-button'),
  )

  if (imageInput.value !== '') {
    submitButton.removeAttribute('disabled')
  } else {
    submitButton.setAttribute('disabled', '')
  }
}
