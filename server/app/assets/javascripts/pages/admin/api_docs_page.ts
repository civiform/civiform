function init(): void {
  const submitForm = () =>
    document.querySelector<HTMLFormElement>('#selectionForm')?.submit()

  document.getElementById('programSlug')?.addEventListener('change', submitForm)
  document.getElementById('stage')?.addEventListener('change', submitForm)
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init)
} else {
  init()
}
