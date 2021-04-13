
/** 
 * This class controls the visibility of toast messages.
 * 
 * Functionality includes:
 *  - showing and hiding toast messages and the toast container.
 *  - dismiss a toast messages based on a user action or after a specified timeout.
 *  - permanently dismiss toast messags (using localStorage)
 */
class Toast {
  containerId = 'toast-container';
  messageClass = '.cf-toast';

  constructor() {
    this.maybeShowToasts();
  }

  /** If a toast message is present, make it visible for the specified duration. */
  maybeShowToasts() {
    const toastContainer = document.getElementById(this.containerId);
    const toastMessages = Array.from(document.querySelectorAll(this.messageClass));
    if (toastContainer === null) {
      return;
    }

    toastMessages.forEach((toastMessage) => {
      const toastId = toastMessage.getAttribute('id');
      const toastIgnored = 
        toastMessage.getAttribute('ignorable') && localStorage.getItem(toastId + '-dismissed');
      
      if (!toastIgnored) {
        toastContainer!.classList.remove('hidden');
        toastMessage.classList.remove('opacity-0');

        if (toastMessage.hasAttribute('duration')) {
          const showDuration = toastMessage.getAttribute('duration');
          setTimeout(this.dismissToast, showDuration, toastId, /* dismissClicked = */ false);
        }

        const dismissButton = document.getElementById(toastId + '-dismiss');
        if (dismissButton) {
          dismissButton.addEventListener("click", this.dismissClicked);
        }
      } else {
        toastMessage.remove();
      }
    });
  }
  
  /** Hide warning message and throw an indicator in local storage to not show. */
  dismissClicked(event: Event) {
    const target = event.target as Element;
    const toast = target.closest(this.messageClass);
    if (toast && toast.hasAttribute('id')) {
      const toastId = toast.getAttribute('id')!;
      this.dismissToast(toastId, /* dismissClicked = */ true);
    }
  }
  
  /** If toastMessage has a storageId and was dismissed by the user (not a timeout) 
   * then we add the id to localStorage so that it isn't displayed again.
   */
  dismissToast(toastId: string, dismissClicked: boolean) {
    const toastMessage = document.getElementById(toastId);
    if (toastMessage) { 
      if (dismissClicked && toastMessage.getAttribute('ignorable')) {
        localStorage.setItem(toastId + '-dismissed', "true");
      }
  
      // Dismiss the toast with the given id.
      toastMessage.classList.add('opacity-0');
      this.cleanupToast(toastId);
    }
  }
  
  /** Removes a toast from the DOM and hides the toast container if it is now empty. */
  cleanupToast(toastId: string) {  
    const toastMessage = document.getElementById(toastId);
    if (toastMessage) { 
      // Remove toast message.
      toastMessage.remove();
    }

    /** Hide toast container if there are no toast messages active. */
    const toastContainer = document.getElementById(this.containerId);
    if (toastContainer && toastContainer.children.length == 0) { 
      toastContainer.classList.add('hidden');
    }
  }
}

new Toast();
