

/** Show the warning message if it hasn't been dismissed by the user. */
function maybeShowWarning() {
    if (!localStorage.getItem("hideWarning")) {
      const warningDiv = document.getElementById("warning-message");
      if (warningDiv) {
        warningDiv.classList.remove("hidden");
      }
      const warningDismissButton = document.getElementById("warning-message-dismiss");
      if (warningDismissButton) {
        warningDismissButton.addEventListener("click", dismissWarning);
      }
    }
  }
  
   /** If a toast message is present, make it visible for the specified duration. */
  function maybeShowToast() {
    const toastMessage = document.querySelector('.cf-toast');
    if (toastMessage) {
      // const id = toastMessage.getAttribute('id');
      toastMessage.classList.remove('opacity-0');
      if (toastMessage.hasAttribute('duration')) {
        const showDuration = toastMessage.getAttribute('duration');
        const toastId = "toastId";
        setTimeout(dismissToast, showDuration, toastId, /* dismissClicked = */ false);
      }
      const dismissButton = document.querySelector(".cf-message-dismiss");
      if (dismissButton) {
        dismissButton.addEventListener("click", dismissClicked);
      }
    }
  }
  
  /** Hide warning message and throw an indicator in local storage to not show. */
  function dismissClicked(event: Event) {
    const target = event.target;
    const toastId = 'rwrwer';
    dismissToast(toastId, /* dismissClicked = */ true);
  }
  
  // If toastMessage has a storageId and was dismissed by the user (not a timeout)
  // then we add the id to localStorage so that it isn't displayed again.
  function dismissToast(toastId: string, dismissClicked: boolean) {
    const toastMessage = document.getElementById(toastId);
    if (toastMessage) { 
      if (dismissClicked && toastMessage.hasAttribute('storageId')) {
        const storageId = toastMessage.getAttribute('storageId');
        localStorage.setItem(storageId, "true");
      }
  
      // Dismiss the toast with the given id.
      toastMessage.classList.add('opacity-0');
      cleanupToast(toastId);
    }
  }
  
  /** Used for attaching pseudo-random ids to dom elements. */
  function createPseudoRandomUuid() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
  
  function cleanupToast(toastId: string) {  
    const toastMessage = document.getElementById(toastId);
    if (toastMessage) { 
      // Remove toast message.
      toastMessage.remove();
    }
    // Hide toast container if there are no toast messages active.
    const toastContainer = document.getElementById('toastContainer');
    if (toastContainer?.children.length == 0) { 
      toastContainer.classList.add('hidden');
    }
  }