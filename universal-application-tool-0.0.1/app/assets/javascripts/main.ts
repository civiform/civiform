/** 
 * We're trying to keep the JS pretty mimimal for CiviForm, so we're only using it 
 * where it's necessary to improve the user experience.
 *
 * Appropriate uses include:
 *  - Visual transitions that aren't possible with just CSS.
 *  - Rare instances in which we need to update a page without refreshing.
 *  - TBD
 */

function attachDropdown(elementId: string) {
  const dropdownId = elementId + "-dropdown";
  const element = document.getElementById(elementId);
  const dropdown = document.getElementById(dropdownId);
  if (dropdown && element) {
    // Attach onclick event to element to toggle dropdown visibility.
    element.addEventListener("click", () => toggleElementVisibility(dropdownId));

    // Attach onblur event to page to hide dropdown if it wasn't the clicked element.
    document.addEventListener("click", (e) => maybeHideElement(e, dropdownId, elementId));
  }
}

function toggleElementVisibility(id: string) {
  const element = document.getElementById(id);
  if (element) {
    element.classList.toggle("hidden");
  }
}

function maybeHideElement(e: Event, id: string, parentId: string) {
  if (e.target instanceof Element) {
    const parent = document.getElementById(parentId);
    if (parent && !parent.contains(e.target)) {
      const elementToHide = document.getElementById(id);
      if (elementToHide) {
        elementToHide.classList.add("hidden");
      }
    }
  }
}

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
    toastMessage.classList.remove('opacity-0');
    if (toastMessage.hasAttribute('duration')) {
      const showDuration = toastMessage.getAttribute('duration');
      setTimeout(function() {
        toastMessage.classList.add('opacity-0');
      }, showDuration);
    }
  }
}

/** Hide warning message and throw an indicator in local storage to not show. */
function dismissWarning() {
  const warningDiv = document.getElementById("warning-message");
  if (warningDiv) {
    warningDiv.classList.add("hidden");
  }
  localStorage.setItem("hideWarning", "true");
}

function init() {
  attachDropdown("create-question-button");
  maybeShowToast();

  /* REMOVE BEFORE FLIGHT - Demo only. */
  maybeShowWarning();
}
init();
