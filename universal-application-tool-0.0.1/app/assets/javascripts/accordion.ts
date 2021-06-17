
/** This class is a simple controller for accordion elements. */
class AccordionController {
  static accordionSelector = '.cf-accordion';
  static accordionButtonSelector = '.cf-accordion-button';
  static accordionHeaderSelector = '.cf-accordion-header';
  static accordionVisibleClass = 'cf-accordion-visible';

  /* These  */
  private static accordionClasses = [
    'cf-accordion', 'bg-white', 'my-4', 'p-4', 'rounded-lg', 'shadow-md', 'border', 'border-gray-300'
  ];
  private static accordionContentClasses = ['cf-accordion-content', 'h-0', 'overflow-hidden'];
  private static accordionHeaderClasses = ['cf-accordion-header', 'relative'];
  private static accordionTitleClasses = ['text-xl', 'font-light'];
  // private static accordionSvgPath = 'M19 9l-7 7-7-7';
  
  constructor() {
    this.init();
  }

  private init() {
    const items =
      Array.from(document.querySelectorAll(AccordionController.accordionHeaderSelector));

    items.forEach(
      (item) => {
        item.addEventListener('click', AccordionController.toggleAccordion);
      }
    );
  }

  static build(title: string, accordionContent: Element) {
    let accordion = document.createElement('div');

    let accordionHeader = document.createElement('div');
    accordionHeader.addEventListener('click', AccordionController.toggleAccordion);
    this.accordionHeaderClasses.forEach(headerClass => accordionHeader.classList.add(headerClass));

    let accordionTitle = document.createElement('div');
    this.accordionTitleClasses.forEach(titleClass => accordionHeader.classList.add(titleClass));
    accordionTitle.textContent = title;
    accordionHeader.appendChild(accordionTitle);

    let accordionButton = document.createElement('div');
    accordionHeader.appendChild(accordionButton);

    accordion.appendChild(accordionHeader);

    this.accordionContentClasses.forEach(contentClass => accordionContent.classList.add(contentClass));
    accordion.appendChild(accordionContent);
    return accordion;
  }

  static toggleAccordion(event: Event) {
    const target = event.target as Element;
    const parentAccordion = target.closest(AccordionController.accordionSelector);
    if (parentAccordion) {
      parentAccordion.classList.toggle(AccordionController.accordionVisibleClass);
    }
  }
}

new AccordionController();