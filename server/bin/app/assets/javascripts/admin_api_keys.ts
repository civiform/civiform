// Javascript handling for the admin API keys page (ApiKeyIndexView)
import {addEventListenerToElements} from './util'

function onRetireKeyFormSubmit(event: Event) {
  const keyName = (event.target as HTMLFormElement).dataset.apiKeyName
  const message =
    'Retiring the API key is permanent and will prevent anyone from being able to call the API with the key. Are you sure you want to retire ' +
    keyName +
    '?'
  if (!confirm(message)) {
    event.preventDefault()
  }
}

export function init() {
  addEventListenerToElements(
    '.retire-key-form',
    'submit',
    onRetireKeyFormSubmit,
  )
}
