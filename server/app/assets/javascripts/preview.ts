/** The preview controller is responsible for updating question preview text in the question builder. */
class PreviewController {
  private static readonly QUESTION_TEXT_INPUT_ID = 'question-text-textarea'
  private static readonly QUESTION_HELP_TEXT_INPUT_ID =
    'question-help-text-textarea'
  private static readonly QUESTION_ENUMERATOR_INPUT_ID =
    'question-enumerator-select'
  private static readonly QUESTION_ENTITY_TYPE_INPUT_ID =
    'enumerator-question-entity-type-input'
  private static readonly QUESTION_ADD_OPTION_ID = 'add-new-option'
  private static readonly QUESTION_SETTINGS_ID = 'question-settings'

  private static readonly QUESTION_TEXT_CLASS = '.cf-applicant-question-text'
  private static readonly QUESTION_HELP_TEXT_CLASS =
    '.cf-applicant-question-help-text'
  private static readonly REPEATED_QUESTION_INFORMATION_ID =
    '#repeated-question-information'
  private static readonly QUESTION_ENTITY_TYPE_BUTTON_ID =
    '#enumerator-field-add-button'
  private static readonly QUESTION_ENTITY_NAME_INPUT_CLASS =
    '.cf-entity-name-input'
  private static readonly QUESTION_ENTITY_DELETE_BUTTON_CLASS =
    '.cf-enumerator-delete-button'

  private static readonly DEFAULT_QUESTION_TEXT = 'Sample question text'
  private static readonly DEFAULT_ENTITY_TYPE = 'Sample repeated entity type'

  // This regex is used to match $this and $this.parent (etc) strings so we can
  // highlight them in the question preview.
  private static readonly THIS_REGEX = /(\$this(?:\.parent)*)/g

  private static accordionClasses = [
    'cf-accordion',
    'bg-white',
    'my-4',
    'p-4',
    'rounded-lg',
    'shadow-md',
    'border',
    'border-gray-300',
  ]
  private static accordionContentClasses = [
    'cf-accordion-content',
    'h-0',
    'overflow-hidden',
  ]
  private static accordionHeaderClasses = ['cf-accordion-header', 'relative']
  private static accordionTitleClasses = ['text-xl', 'font-light']

  private static accordionContent = '>'
  private static accordionHeader = '### '
  private static bulletedItem = '* '

  constructor() {
    const textInput = document.getElementById(
      PreviewController.QUESTION_TEXT_INPUT_ID,
    ) as HTMLInputElement | null
    if (textInput) {
      textInput.addEventListener(
        'input',
        (ev) => {
          PreviewController.updateFromNewQuestionText(textInput.value)
        },
        false,
      )
      PreviewController.updateFromNewQuestionText(textInput.value)
    }
    const helpTextInput = document.getElementById(
      PreviewController.QUESTION_HELP_TEXT_INPUT_ID,
    ) as HTMLInputElement | null
    if (helpTextInput) {
      helpTextInput.addEventListener(
        'input',
        (ev) => {
          PreviewController.updateFromNewQuestionHelpText(helpTextInput.value)
        },
        false,
      )
      PreviewController.updateFromNewQuestionHelpText(helpTextInput.value)
    }
    const enumeratorSelector = document.getElementById(
      PreviewController.QUESTION_ENUMERATOR_INPUT_ID,
    ) as HTMLInputElement | null
    if (enumeratorSelector) {
      enumeratorSelector.addEventListener(
        'input',
        (ev) => {
          PreviewController.updateFromNewEnumeratorSelector(
            enumeratorSelector.value,
          )
        },
        false,
      )
      PreviewController.updateFromNewEnumeratorSelector(
        enumeratorSelector.value,
      )
    }
    const entityTypeInput = document.getElementById(
      PreviewController.QUESTION_ENTITY_TYPE_INPUT_ID,
    ) as HTMLInputElement | null
    if (entityTypeInput) {
      entityTypeInput.addEventListener(
        'input',
        (ev) => {
          PreviewController.updateFromNewEntityType(entityTypeInput.value)
        },
        false,
      )
      PreviewController.updateFromNewEntityType(entityTypeInput.value)
    }

    // TODO(clouser): Currently using the add button as a proxy for a multi-option
    // question type. Might want to use a class instead so that we can do all of
    // the querying on the top-level DOM element.
    const addOptionButton = document.getElementById(
      PreviewController.QUESTION_ADD_OPTION_ID,
    ) as HTMLButtonElement | null
    if (addOptionButton) {
      const questionSettingsSection = document.getElementById(
        PreviewController.QUESTION_SETTINGS_ID,
      ) as HTMLElement | null
      if (questionSettingsSection) {
        PreviewController.addOptionObservers(questionSettingsSection)
      }
    }
  }

  private static updateOptionsList(parent: HTMLElement) {
    const configuredOptions = Array.from(
      parent.querySelectorAll('.cf-multi-option-question-option input'),
    ).map((el) => {
      return (el as HTMLInputElement).value
    })
    if (configuredOptions.length == 0) {
      configuredOptions.push('The default option text.')
    }

    // Reset the option list in the preview.
    // Keep track of the first element so new options can be cloned from it.
    const allOptions = Array.from(
      document.querySelectorAll('#sample-question .cf-multi-option'),
    )
    const firstOption = allOptions[0]
    const firstOptionParent = firstOption.parentElement!
    allOptions.forEach((option => {
      option.remove()
    }))

    for (const configuredOption of configuredOptions) {
      const newOptionContainer = firstOption.cloneNode(true) as HTMLElement
      // Set the underlying value.
      const optionText = newOptionContainer.classList.contains('cf-multi-option-text')
      ? newOptionContainer : newOptionContainer.querySelector(
        '.cf-multi-option-text',
      )! as HTMLElement
      optionText.innerText = configuredOption
      firstOptionParent.appendChild(newOptionContainer)
    }
  }

  private static addOptionObservers(questionSettingsSection: HTMLElement) {
    const mutationObserver = new MutationObserver(
      (records: MutationRecord[]) => {
        for (const record of records) {
          if (record.removedNodes.length > 0) {
            PreviewController.updateOptionsList(questionSettingsSection)
          }
          for (const newNode of Array.from(record.addedNodes)) {
            const newInputs = Array.from(
              (<Element>newNode).querySelectorAll('input'),
            )
            newInputs.forEach((newInput) => {
              newInput.addEventListener('input', () => {
                PreviewController.updateOptionsList(questionSettingsSection)
              })
            })
            if (newInputs.length > 0) {
              // Explicitly call this so that an option is added for the new entry.
              PreviewController.updateOptionsList(questionSettingsSection)
            }
          }
        }
      },
    )

    mutationObserver.observe(questionSettingsSection, {
      childList: true,
      subtree: true,
      characterDataOldValue: true,
    })
  }

  private static updateFromNewQuestionText(text: string) {
    text = text || PreviewController.DEFAULT_QUESTION_TEXT
    const questionType = document.querySelector('.cf-question-type')
    const useAdvancedFormatting =
      questionType && questionType.textContent === 'STATIC'
    if (useAdvancedFormatting) {
      const contentElement = PreviewController.formatText(text, true)
      contentElement.classList.add('text-sm')
      contentElement.classList.add('font-normal')
      contentElement.classList.add('pr-16')

      const contentParent = document.querySelector(
        PreviewController.QUESTION_TEXT_CLASS,
      ) as Element
      if (contentParent) {
        contentParent.innerHTML = ''
        contentParent.appendChild(contentElement)
      }
    } else {
      PreviewController.setTextAndHighlightEnumeratorReferences(
        PreviewController.QUESTION_TEXT_CLASS,
        text,
      )
    }
  }

  private static updateFromNewQuestionHelpText(helpText: string) {
    PreviewController.setTextAndHighlightEnumeratorReferences(
      PreviewController.QUESTION_HELP_TEXT_CLASS,
      helpText,
    )
  }

  private static updateFromNewEnumeratorSelector(
    enumeratorSelectorValue: string,
  ) {
    const repeatedQuestionInformation = document.querySelector(
      PreviewController.REPEATED_QUESTION_INFORMATION_ID,
    )
    repeatedQuestionInformation.classList.toggle(
      'hidden',
      enumeratorSelectorValue === '',
    )
  }

  private static updateFromNewEntityType(entityType: string) {
    entityType = entityType || PreviewController.DEFAULT_ENTITY_TYPE
    PreviewController.setAllMatchingElements(
      PreviewController.QUESTION_ENTITY_NAME_INPUT_CLASS + ' label',
      entityType + ' name',
    )
    PreviewController.setTextContent(
      PreviewController.QUESTION_ENTITY_TYPE_BUTTON_ID,
      'Add ' + entityType,
    )
    PreviewController.setAllMatchingElements(
      PreviewController.QUESTION_ENTITY_DELETE_BUTTON_CLASS,
      'Remove ' + entityType,
    )
  }

  /**
   * Sets the child nodes of the selected div as text or span nodes.
   * This will highlight text matching PreviewController.THIS_REGEX
   * in span nodes.
   *
   * This will only work when the selected div is only supposed to contain
   * text and has no other child nodes.
   *
   * @param {string} selector The query selector used to find the preview div
   * @param {string} text The text to parse for $this and $this.parent (etc) strings.
   */
  private static setTextAndHighlightEnumeratorReferences(
    selector: string,
    text: string,
  ) {
    const previewDiv = document.querySelector(selector)
    const pieces = text.split(PreviewController.THIS_REGEX)

    previewDiv.innerHTML = ''
    pieces.forEach((piece) => {
      if (piece.match(PreviewController.THIS_REGEX)) {
        const thisSpan = document.createElement('span')
        thisSpan.classList.add('bg-yellow-300')
        thisSpan.textContent = piece
        previewDiv.appendChild(thisSpan)
      } else {
        previewDiv.appendChild(document.createTextNode(piece))
      }
    })
  }

  private static setTextContent(selector: string, text: string) {
    const previewDiv = document.querySelector(selector)
    if (previewDiv) {
      previewDiv.textContent = text
    }
  }

  private static setAllMatchingElements(selector: string, text: string) {
    const matchingElements = document.querySelectorAll(selector)
    Array.from(matchingElements).forEach(function (matchingElement) {
      ;(<HTMLElement>matchingElement).textContent = text
    })
  }

  private static formatText(
    text: string,
    preserveEmptyLines: boolean,
  ): Element {
    const ret = document.createElement('div')
    const lines = text.split('\n')
    for (let i = 0; i < lines.length; i++) {
      const currentLine = lines[i].trim()
      if (currentLine.startsWith(this.accordionHeader)) {
        const title = currentLine.substring(4)
        let content = ''
        let next = i + 1
        while (
          next < lines.length &&
          lines[next].startsWith(this.accordionContent)
        ) {
          content += lines[next].substring(1) + '\n'
          next++
        }
        i = next - 1
        ret.appendChild(PreviewController.buildAccordion(title, content))
      } else if (currentLine.startsWith(this.bulletedItem)) {
        const listItems = [currentLine.substring(2).trim()]
        let next = i + 1
        while (
          next < lines.length &&
          lines[next].startsWith(this.bulletedItem)
        ) {
          listItems.push(lines[next].substring(2).trim())
          next++
        }
        i = next - 1
        ret.appendChild(PreviewController.buildList(listItems))
      } else if (currentLine.length > 0) {
        const content = document.createElement('div')
        content.textContent = currentLine
        ret.appendChild(content)
      } else if (preserveEmptyLines) {
        const emptyLine = document.createElement('div')
        emptyLine.classList.add('h-6')
        ret.appendChild(emptyLine)
      }
    }
    return ret
  }

  private static buildAccordion(title: string, content: string): Element {
    const childContent = PreviewController.formatText(
      content,
      /* preserveEmptyLines = */ true,
    )
    const accordion = document.createElement('div')
    this.accordionClasses.forEach((accordionClass) =>
      accordion.classList.add(accordionClass),
    )

    const accordionHeader = document.createElement('div')
    accordionHeader.addEventListener('click', (event: Event) => {
      const parentAccordion = (event.target as Element).closest('.cf-accordion')
      if (parentAccordion) {
        parentAccordion.classList.toggle('cf-accordion-visible')
      }
    })
    this.accordionHeaderClasses.forEach((headerClass) =>
      accordionHeader.classList.add(headerClass),
    )

    const accordionTitle = document.createElement('div')
    this.accordionTitleClasses.forEach((titleClass) =>
      accordionHeader.classList.add(titleClass),
    )
    accordionTitle.textContent = title
    accordionHeader.appendChild(accordionTitle)

    const accordionButton = document.createElement('div')
    accordionHeader.appendChild(accordionButton)

    accordion.appendChild(accordionHeader)

    this.accordionContentClasses.forEach((contentClass) =>
      childContent.classList.add(contentClass),
    )
    accordion.appendChild(childContent)
    return accordion
  }

  private static buildList(items: string[]): Element {
    const listTag = document.createElement('ul')
    listTag.classList.add('list-disc')
    listTag.classList.add('mx-8')

    items.forEach((item) => {
      const listItem = document.createElement('li')
      listItem.textContent = item
      listTag.appendChild(listItem)
    })
    return listTag
  }
}

// eslint-disable-next-line no-unused-vars
const previewController = new PreviewController()
