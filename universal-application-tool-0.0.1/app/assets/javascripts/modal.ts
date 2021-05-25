/**
 * We're trying to keep the JS pretty mimimal for CiviForm, so we're only using it
 * where it's necessary to improve the user experience.
 *
 * Appropriate uses include:
 *  - Visual transitions that aren't possible with just CSS.
 *  - Rare instances in which we need to update a page without refreshing.
 *  - TBD
 */

class ModalController {

  /** Find the modals, and add on-click listeners on their respective buttons to toggle them. */
  attachModalListeners() {
    const modalContainer = document.querySelector('#modal-container');
    // Find and connect each modal to its button
    const modals = Array.from(document.querySelectorAll('.cf-modal'));
    modals.forEach(modal => {
      const modalButton = document.querySelector(`#${modal.id}-button`);
      modalButton.addEventListener("click", function() {
        modalContainer.classList.toggle('hidden');
        modal.classList.toggle('hidden');
      });

      const modalClose = document.querySelector(`#${modal.id}-close`);
      modalClose.addEventListener("click", function() {
        modalContainer.classList.toggle('hidden');
        modal.classList.toggle('hidden');
      });
    });
  }

  constructor() {
    this.attachModalListeners();
  }
}

let modalController = new ModalController();