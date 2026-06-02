function init(): void {
  const submitForm = () =>
    document.querySelector<HTMLFormElement>('#selectionForm')?.submit()

  document.getElementById('programSlug')?.addEventListener('change', submitForm)
  document.getElementById('stage')?.addEventListener('change', submitForm)
}

if (document.readyState === 'loading') {
  // DOM is NOT ready yet: the event hasn't fired, so we can
  // safely add a listener for DOMContentLoaded
  document.addEventListener('DOMContentLoaded', init)
} else {
  // readyState is 'interactive' or 'complete': DOM is ALREADY ready,
  // DOMContentLoaded has fired (or won't help us), so just run init now
  init()
}
