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

/** Hide warning message and throw an indicator in local storage to not show. */
function dismissWarning() {
  const warningDiv = document.getElementById("warning-message");
  if (warningDiv) {
    warningDiv.classList.add("hidden");
  }
  localStorage.setItem("hideWarning", "true");
}

/** In the admin question form - add a new option input for each new question answer option. */
function addNewQuestionAnswerOptionForm(event: Event) {
  // Prevent the default event, which is to submit the containing form.
  event.preventDefault();

  // Copy the answer template and remove ID and hidden properties.
  const newField = document.getElementById("multi-option-question-answer-template").cloneNode(true) as HTMLElement;
  newField.classList.remove("hidden");
  newField.removeAttribute("id");

  // Register the click event handler for the remove button.
  newField.lastElementChild.addEventListener("click", removeQuestionOption);

  // Find the add option button and insert the new option input field before it.
  const button = document.getElementById("add-new-option");
  document.getElementById("question-settings").insertBefore(newField, button);
}

/** In the admin question form - remove an answer option input for multi-option questions. */
function removeQuestionOption(event: Event) {
  // Prevent button default, which is to submit the form.
  event.preventDefault();

  // Get the parent div, which contains the input field and remove button, and remove it.
  const optionDiv = (event.target as Element).parentNode;
  optionDiv.parentNode.removeChild(optionDiv);
}

function init() {
  attachDropdown("create-question-button");

  /* REMOVE BEFORE FLIGHT - Demo only. */
  maybeShowWarning();

  // Configure the button on the admin question form to add more answer options
  const questionOptionButton = document.getElementById("add-new-option");
  if (questionOptionButton) {
    questionOptionButton.addEventListener("click", addNewQuestionAnswerOptionForm);
  }
}
init();
