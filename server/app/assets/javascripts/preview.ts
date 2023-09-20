/** The preview controller is responsible for updating question preview text in the question builder. */
import {assertNotNull} from './util'
import {AccordionController as Accordion} from './accordion'
import MarkdownIt = require('markdown-it')

class PreviewController {
  private static readonly QUESTION_TEXT_INPUT_ID = 'question-text-textarea'
  private static readonly QUESTION_HELP_TEXT_INPUT_ID =
    'question-help-text-textarea'
  private static readonly QUESTION_ENUMERATOR_INPUT_ID =
    'question-enumerator-select'
  private static readonly QUESTION_ENTITY_TYPE_INPUT_ID =
    'enumerator-question-entity-type-input'
  private static readonly QUESTION_SETTINGS_ID = 'question-settings'
  private static readonly QUESTION_ENTITY_TYPE_BUTTON_ID =
    'enumerator-field-add-button'
  private static readonly REPEATED_QUESTION_INFORMATION_ID =
    'repeated-question-information'
  private static readonly SAMPLE_QUESTION_ID = 'sample-question'

  private static readonly QUESTION_TEXT_SELECTOR = '.cf-applicant-question-text'
  private static readonly QUESTION_HELP_TEXT_SELECTOR =
    '.cf-applicant-question-help-text'
  private static readonly QUESTION_ENTITY_NAME_INPUT_SELECTOR =
    '.cf-entity-name-input'
  private static readonly QUESTION_ENTITY_DELETE_BUTTON_SELECTOR =
    '.cf-enumerator-delete-button'
  private static readonly QUESTION_MULTI_OPTION_SELECTOR =
    '.cf-multi-option-question-option'
  private static readonly QUESTION_MULTI_OPTION_INPUT_FIELD_SELECTOR =
    '.cf-multi-option-input'
  private static readonly QUESTION_MULTI_OPTION_VALUE_CLASS =
    'cf-multi-option-value'

  // These are defined in {@link ApplicantQuestionRendererFactory}.
  private static readonly DEFAULT_QUESTION_TEXT = 'Sample question text'
  private static readonly DEFAULT_ENTITY_TYPE = 'Sample repeated entity type'
  private static readonly DEFAULT_OPTION_TEXT = 'Sample question option'

  // This is defined in {@link QuestionType}.
  private static readonly STATIC_QUESTION_TEXT = 'Static Text'

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
    'h-auto',
    'mt-2',
    'mb-1',
  ]
  private static accordionHeaderClasses = [
    'cf-accordion-header',
    'flex',
    'justify-between',
    'relative',
    'w-full',
    'bg-white',
    'text-black',
    'px-0',
    'py-0',
    'border-0',
    'hover:bg-gray-100',
  ]
  private static accordionTitleClasses = ['text-xl', 'font-light']

  private static accordionContent = '>'
  private static accordionHeader = '### '
  private static bulletedItem = '* '
  private static headerIndicator = '#'

  constructor() {
    const textInput = document.getElementById(
      PreviewController.QUESTION_TEXT_INPUT_ID,
    ) as HTMLInputElement | null
    if (textInput) {
      textInput.addEventListener(
        'input',
        () => {
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
        () => {
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
        () => {
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
        () => {
          PreviewController.updateFromNewEntityType(entityTypeInput.value)
        },
        false,
      )
      PreviewController.updateFromNewEntityType(entityTypeInput.value)
    }

    const questionSettings = document.getElementById(
      PreviewController.QUESTION_SETTINGS_ID,
    )
    const questionPreviewContainer = document.getElementById(
      PreviewController.SAMPLE_QUESTION_ID,
    )
    if (questionSettings && questionPreviewContainer) {
      PreviewController.addOptionObservers({
        questionSettings,
        questionPreviewContainer,
      })
    }
  }

  private static addOptionObservers({
    questionSettings,
    questionPreviewContainer,
  }: {
    questionSettings: HTMLElement
    questionPreviewContainer: HTMLElement
  }) {
    const firstPreviewOption = questionPreviewContainer.querySelector(
      PreviewController.QUESTION_MULTI_OPTION_SELECTOR,
    )
    if (!firstPreviewOption) {
      return
    }
    // In some cases, the element containing options is distinct from the question settings
    // element. (e.g. <select><option>option1</option><option>option2</option></select>).
    const previewQuestionOptionContainer = firstPreviewOption.parentElement
    if (!previewQuestionOptionContainer) {
      return
    }
    // The option shown at page load is stored for easier cloning when updating based on the
    // configured questions.
    const previewOptionTemplate = firstPreviewOption.cloneNode(
      true,
    ) as HTMLElement
    const syncOptionsToPreview = () => {
      PreviewController.updateOptionsList({
        questionSettings,
        previewOptionTemplate,
        previewQuestionOptionContainer,
      })
    }
    const mutationObserver = new MutationObserver(
      (records: MutationRecord[]) => {
        syncOptionsToPreview()
        for (const record of records) {
          for (const newNode of Array.from(record.addedNodes)) {
            const newInputs = Array.from(
              (<Element>newNode).querySelectorAll('input'),
            )
            newInputs.forEach((newInput) => {
              newInput.addEventListener('input', syncOptionsToPreview)
            })
          }
        }
      },
    )

    mutationObserver.observe(questionSettings, {
      childList: true,
      subtree: true,
      characterDataOldValue: true,
    })
    syncOptionsToPreview()
  }

  private static updateOptionsList({
    questionSettings,
    previewQuestionOptionContainer,
    previewOptionTemplate,
  }: {
    questionSettings: HTMLElement
    previewQuestionOptionContainer: HTMLElement
    previewOptionTemplate: HTMLElement
  }) {
    const configuredOptions = Array.from(
      questionSettings.querySelectorAll(
        `${PreviewController.QUESTION_MULTI_OPTION_SELECTOR} ${PreviewController.QUESTION_MULTI_OPTION_INPUT_FIELD_SELECTOR} input`,
      ),
    ).map((el) => {
      return (el as HTMLInputElement).value
    })
    if (configuredOptions.length === 0) {
      configuredOptions.push(PreviewController.DEFAULT_OPTION_TEXT)
    }

    // Reset the option list in the preview.
    Array.from(
      previewQuestionOptionContainer.querySelectorAll(
        PreviewController.QUESTION_MULTI_OPTION_SELECTOR,
      ),
    ).forEach((previewOption) => {
      previewOption.remove()
    })

    for (const configuredOption of configuredOptions) {
      const newPreviewOption = previewOptionTemplate.cloneNode(
        true,
      ) as HTMLElement
      // Set the underlying value. In some cases, the container element for an option is the same
      // as the element containing the element value (e.g. <option>Value</option>).
      const optionText = newPreviewOption.classList.contains(
        PreviewController.QUESTION_MULTI_OPTION_VALUE_CLASS,
      )
        ? newPreviewOption
        : assertNotNull(
            newPreviewOption.querySelector<HTMLElement>(
              `.${PreviewController.QUESTION_MULTI_OPTION_VALUE_CLASS}`,
            ),
          )
      optionText.innerText = configuredOption
      previewQuestionOptionContainer.appendChild(newPreviewOption)
    }
  }

  private static updateFromNewQuestionText(text: string) {
    text = text || PreviewController.DEFAULT_QUESTION_TEXT
    const questionType = document.querySelector('.cf-question-type')
    const useAdvancedFormatting =
      questionType &&
      questionType.textContent === PreviewController.STATIC_QUESTION_TEXT
    if (useAdvancedFormatting) {
      const contentElement = PreviewController.formatText(text, true)
      contentElement.classList.add('text-sm')
      contentElement.classList.add('font-normal')
      contentElement.classList.add('pr-16')

      const contentParent = document.querySelector(
        PreviewController.QUESTION_TEXT_SELECTOR,
      )
      if (contentParent) {
        contentParent.innerHTML = ''
        contentParent.appendChild(contentElement)
      }
    } else {
      PreviewController.setTextAndHighlightEnumeratorReferences(
        PreviewController.QUESTION_TEXT_SELECTOR,
        text,
      )
    }
    // Add the ability for the accordion to expand and collapse on click
    const accordionHeaderElement = document.querySelector(
      '.cf-accordion-header',
    )
    if (accordionHeaderElement) {
      new Accordion(accordionHeaderElement as HTMLElement)
    }
  }

  private static updateFromNewQuestionHelpText(helpText: string) {
    PreviewController.setTextAndHighlightEnumeratorReferences(
      PreviewController.QUESTION_HELP_TEXT_SELECTOR,
      helpText,
    )
  }

  private static updateFromNewEnumeratorSelector(
    enumeratorSelectorValue: string,
  ) {
    const repeatedQuestionInformation = assertNotNull(
      document.getElementById(
        PreviewController.REPEATED_QUESTION_INFORMATION_ID,
      ),
    )
    repeatedQuestionInformation.classList.toggle(
      'hidden',
      enumeratorSelectorValue === '',
    )
  }

  private static updateFromNewEntityType(entityType: string) {
    entityType = entityType || PreviewController.DEFAULT_ENTITY_TYPE
    PreviewController.setAllMatchingElements(
      PreviewController.QUESTION_ENTITY_NAME_INPUT_SELECTOR + ' label',
      entityType + ' name',
    )
    PreviewController.setTextContent(
      `#${PreviewController.QUESTION_ENTITY_TYPE_BUTTON_ID}`,
      'Add ' + entityType,
    )
    PreviewController.setAllMatchingElements(
      PreviewController.QUESTION_ENTITY_DELETE_BUTTON_SELECTOR,
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
    const previewDiv = assertNotNull(document.querySelector(selector))
    const pieces = text.split(PreviewController.THIS_REGEX)

    previewDiv.innerHTML = ''
    pieces.forEach((piece) => {
      if (piece.match(PreviewController.THIS_REGEX)) {
        const thisSpan = document.createElement('span')
        thisSpan.classList.add('bg-amber-300')
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
    Array.from(matchingElements).forEach(function (matchingElement, index) {
      ;(<HTMLElement>matchingElement).textContent =
        text + ' #' + (index + 1).toString()
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
        ret.appendChild(PreviewController.parseMarkdown(currentLine))
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

    const accordionHeader = document.createElement('button')
    accordionHeader.setAttribute('aria-controls', 'cf-accordion-content')
    accordionHeader.setAttribute('aria-expanded', 'false')
    accordionHeader.setAttribute('type', 'button')

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

    childContent.setAttribute('id', 'cf-accordion-content')
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
      listItem.appendChild(PreviewController.parseMarkdown(item))
      listTag.appendChild(listItem)
    })
    return listTag
  }

  private static parseMarkdown(currentLine: string): Element {
    const md = new MarkdownIt()
    const parser = new DOMParser()
    let html
    if (currentLine[0] && currentLine[0] === this.headerIndicator) {
      html = parser.parseFromString(md.render(currentLine), 'text/html')
    } else {
      html = parser.parseFromString(md.renderInline(currentLine), 'text/html')
    }
    return html.body
  }
}

export function init() {
  new PreviewController()
}
