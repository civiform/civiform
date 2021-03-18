/** 
 * We're trying to keep the JS pretty mimimal for CiviForm, so we're only using it 
 * where it's necessary to improve the user experience.
 *
 * Appropriate uses include:
 *  - Visual transitions that aren't possible with just CSS.
 *  - Rare instances in which we need to update a page without refreshing.
 *  - TBD
 */

function attachDropdown(elementId) {
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

function toggleElementVisibility(id) {
    const element = document.getElementById(id);
    element.classList.toggle("hidden");
}

function maybeHideElement(e, id, parentId) {
    const parent = document.getElementById(parentId);
    if (parent && !parent.contains(e.target)) {     
        document.getElementById(id).classList.add("hidden");
    }
}

function init() {
    attachDropdown("create-question-button");
}
init();
