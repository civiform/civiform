/** This class is a simple controller for accordion elements. */
export class AccordionController {
  buttonNode: HTMLElement
  controlledNode: HTMLElement | null

  /*
   *   This content is licensed according to the W3C Software License at
   *   https://www.w3.org/Consortium/Legal/2015/copyright-software-and-document
   *
   *   Desc:   Disclosure button widget that implements ARIA Authoring Best Practices
   */
  constructor(buttonNode: HTMLElement) {
    this.buttonNode = buttonNode
    this.controlledNode = null
    const id = this.buttonNode.getAttribute('aria-controls')

    if (id) {
      this.controlledNode = document.getElementById(id)
    }

    this.buttonNode.setAttribute('aria-expanded', 'false')
    this.hideContent()

    this.buttonNode.addEventListener('click', this.onClick.bind(this))
    this.buttonNode.addEventListener('focus', this.onFocus.bind(this))
    this.buttonNode.addEventListener('blur', this.onBlur.bind(this))
  }

  showContent() {
    if (this.controlledNode) {
      this.controlledNode.style.display = 'block'
    }
  }

  hideContent() {
    if (this.controlledNode) {
      this.controlledNode.style.display = 'none'
    }
  }

  toggleExpand() {
    if (this.buttonNode.getAttribute('aria-expanded') === 'true') {
      this.buttonNode.setAttribute('aria-expanded', 'false')
      this.hideContent()
    } else {
      this.buttonNode.setAttribute('aria-expanded', 'true')
      this.showContent()
    }
  }

  /* EVENT HANDLERS */

  onClick() {
    this.toggleExpand()
  }

  onFocus() {
    this.buttonNode.classList.add('focus')
  }

  onBlur() {
    this.buttonNode.classList.remove('focus')
  }
}

export function init() {
  const disclosureButtons: NodeListOf<Element> = document.querySelectorAll(
    'button[aria-expanded][aria-controls]',
  )

  for (let i = 0; i < disclosureButtons.length; i++) {
    new AccordionController(disclosureButtons[i] as HTMLElement)
  }
}
