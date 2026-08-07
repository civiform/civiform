/**
 * Client-side behavior for the admin multi-option question editor (checkbox,
 * dropdown, radio): adding, removing, and reordering answer options.
 *
 * Shared by the legacy J2HTML page (wired in {@link module:main}) and the
 * Thymeleaf page (wired in pages/admin/question_edit_page). Both DOMs expose the
 * same contract: a `#multi-option-container` holding `.cf-multi-option-question-option`
 * rows, an `#add-new-option` button, and a `<template id="multi-option-question-answer-template">`.
 */
export class MultiOptionQuestion {
  private readonly containerId: string
  private readonly templateId: string
  private readonly addButtonId: string
  private readonly wiredOptions = new WeakSet<Element>()

  constructor(
    containerId = 'multi-option-container',
    templateId = 'multi-option-question-answer-template',
    addButtonId = 'add-new-option',
  ) {
    this.containerId = containerId
    this.templateId = templateId
    this.addButtonId = addButtonId
  }

  /** Kick everything off. Call once after the DOM is ready. */
  init() {
    const container = document.getElementById(this.containerId)
    if (container == null) return

    if (container.dataset.multiOptionInitialized != null) {
      return
    }

    container.dataset.multiOptionInitialized = 'true'

    // Wire server-rendered options already in the DOM.
    container
      .querySelectorAll('.cf-multi-option-question-option')
      .forEach((el) => this.wireUpOptionButtons(el))

    // Wire options added later (clones from the add button).
    const observer = new MutationObserver((mutations) => {
      for (const mutation of mutations) {
        mutation.addedNodes.forEach((node) => {
          if (
            node instanceof HTMLElement &&
            node.classList.contains('cf-multi-option-question-option')
          ) {
            this.wireUpOptionButtons(node)
          }
        })
      }
    })
    observer.observe(container, {childList: true})

    // Wire the "Add answer option" button.
    document
      .getElementById(this.addButtonId)
      ?.addEventListener('click', this.addNewOption)
  }

  /** Clone the template and append it. The observer wires the buttons. */
  private addNewOption = () => {
    const template = document.getElementById(
      this.templateId,
    ) as HTMLTemplateElement | null
    if (template == null) return

    const newField = template.content.firstElementChild?.cloneNode(true) as
      | HTMLDivElement
      | undefined
    if (newField == null) return

    document.getElementById(this.containerId)?.appendChild(newField)
  }

  private wireUpOptionButtons(optionEl: Element) {
    if (this.wiredOptions.has(optionEl)) return
    this.wiredOptions.add(optionEl)

    optionEl
      .querySelector('.multi-option-question-field-remove-button')
      ?.addEventListener('click', this.removeInput)

    optionEl
      .querySelector('.multi-option-question-field-move-up-button')
      ?.addEventListener('click', this.moveUp)

    optionEl
      .querySelector('.multi-option-question-field-move-down-button')
      ?.addEventListener('click', this.moveDown)
  }

  private removeInput = (event: Event) => {
    const option = this.getOption(event)
    option?.remove()
  }

  private moveUp = (event: Event) => {
    event.preventDefault()
    const option = this.getOption(event)
    if (option == null) return

    const parent = option.parentElement
    if (parent == null) return

    const options = this.getEditableOptions()
    const index = options.indexOf(option)
    if (index > 0) {
      parent.insertBefore(option, options[index - 1])
    }
  }

  private moveDown = (event: Event) => {
    event.preventDefault()
    const option = this.getOption(event)
    if (option == null) return

    const parent = option.parentElement
    if (parent == null) return

    const options = this.getEditableOptions()
    const index = options.indexOf(option)
    if (index < options.length - 1) {
      parent.insertBefore(options[index + 1], option)
    }
  }

  /** Resolve the enclosing option element from a button click. */
  private getOption(event: Event): HTMLElement | null {
    return (event.currentTarget as Element).closest(
      '.cf-multi-option-question-option',
    )
  }

  private getEditableOptions(): HTMLElement[] {
    return Array.from(
      document.querySelectorAll<HTMLElement>(
        'div.cf-multi-option-question-option-editable:not(.hidden)',
      ),
    )
  }
}
