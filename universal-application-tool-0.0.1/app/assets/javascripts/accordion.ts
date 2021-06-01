
/** This class is a simple controller for accordion elements. */
class AccordionController {
  static accordionSelector = '.cf-accordion';
  static accordionButtonSelector = '.cf-accordion-button';
  static accordionVisibleClass = 'cf-accordion-visible';

  constructor() {
    this.init();
  }

  private init() {
    const buttons =
      Array.from(document.querySelectorAll(AccordionController.accordionButtonSelector));
    buttons.forEach(
      (button) => {
        button.addEventListener('click', this.toggleAccordion);
      }
    );
  }

  toggleAccordion(event: Event) {
    const target = event.target as Element;
    const parentAccordion = target.closest(AccordionController.accordionSelector);
    if (parentAccordion) {
      parentAccordion.classList.toggle(AccordionController.accordionVisibleClass);
    }
  }
}

new AccordionController();