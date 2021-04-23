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

/** In the admin question form - add a new option input for each new question answer option. */
function addNewQuestionAnswerOptionForm(event: Event) {
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
  // Get the parent div, which contains the input field and remove button, and remove it.
  const optionDiv = (event.target as Element).parentNode;
  optionDiv.parentNode.removeChild(optionDiv);
}

/** In the repeater form - add a new input field for a repeated entity. */
function addNewRepeaterField(event: Event) {
  // Copy the first repeater field
  const newField = document.querySelector(".repeater-field").cloneNode(true) as HTMLElement;

  // Remove the text
  const newTextField = newField.querySelector("[type=text]") as HTMLInputElement;
  newTextField.value = null;

  const repeaterFields = document.getElementById("repeater-fields");
  const index = repeaterFields.querySelectorAll(".repeater-field").length;

  // Set new checkbox value to the index and uncheck it
  const newCheckbox = newField.querySelector("[type=checkbox]") as HTMLInputElement;
  newCheckbox.setAttribute("value", index.toString());
  newCheckbox.checked = false;

  repeaterFields.appendChild(newField);
}

function init() {
  attachDropdown("create-question-button");

  // Configure the button on the admin question form to add more answer options
  const questionOptionButton = document.getElementById("add-new-option");
  if (questionOptionButton) {
    questionOptionButton.addEventListener("click", addNewQuestionAnswerOptionForm);
  }

  // Configure the button on the repeater question form to add more repeater field options
  const repeaterOptionButton = document.getElementById("repeater-field-add-button");
  if (repeaterOptionButton) {
    repeaterOptionButton.addEventListener("click", addNewRepeaterField);
  }
}
init();
