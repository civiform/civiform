/**
 * The question bank controller is responsible for manipulating the question bank.
 * The question bank contains questions for all blocks. Question bank is
 * normally hidden. It is shown upon admin clickin "Add question" button.
 *
 * When shown the question bank filters question to match the selected block
 * based on block id from the `data-block-id` attribute of the button that
 * triggered it.
 *
 * Additionally the question bank can read current block from url `sqb` url
 * param which is used when user refreshes the page or comes back to the page
 * using back button or redirect expecting that question bank is open.
 */
class QuestionBankController {
  static readonly FILTER_ID = 'question-bank-filter'
  static readonly QUESTION_CLASS = 'cf-question-bank-element'
  static readonly OPEN_QUESTION_BANK_BUTTON = 'cf-open-question-bank-button'
  static readonly CLOSE_QUESTION_BANK_BUTTON = 'cf-close-question-bank-button'
  static readonly QUESTION_BANK_CONTAINER = 'cf-question-bank-container'
  static readonly QUESTION_BANK_HIDDEN = 'cf-question-bank-hidden'
  static readonly CURRENT_BLOCK_ATTRIBUTE = 'data-block-id'

  // Url param containing id of currently selected block. If url contains this
  // param it means that question bank is open.
  static readonly BANK_SHOWN_URL_PARAM = 'sqb'

  constructor() {
    const questionBankFilter = document.getElementById(
      QuestionBankController.FILTER_ID,
    )
    if (questionBankFilter) {
      questionBankFilter.addEventListener(
        'input',
        QuestionBankController.filterQuestions,
      )
      QuestionBankController.filterQuestions()
    }
    QuestionBankController.initToggleQuestionBankButtons()
  }

  private static initToggleQuestionBankButtons() {
    // To animate question bank we need to use a hack. Question bank is initially
    // hidden using tailwinds `hidden` class: `display: none`. When user clicks
    // "add question" button we want to display questyion bank using quick animation.
    // But CSS transitions don't work if they are triggered simultaneously with display:
    // https://stackoverflow.com/questions/3331353/transitions-on-the-css-display-property
    // To workaround we use 2 classes: `hidden` and `cf-question-bank-hidden`.
    //
    // When showing question we first remove `hidden` and wait for browser to apply it by
    // using requestAnimationFrame() and then remove `cf-question-bank-hidden` class.
    //
    // When hiding we first add `cf-question-bank-hidden` to trigger transition and once
    // it's over add `hidden` class.
    //
    // CSS is fun!
    const questionBankContainer = document.getElementById(
      QuestionBankController.QUESTION_BANK_CONTAINER,
    )

    const openQuestionBankElements = Array.from(
      document.getElementsByClassName(
        QuestionBankController.OPEN_QUESTION_BANK_BUTTON,
      ),
    )
    for (const element of openQuestionBankElements) {
      element.addEventListener('click', (event: Event) => {
        QuestionBankController.showQuestionBank(questionBankContainer, event)
      })
    }
    const closeQuestionBankElements = Array.from(
      document.getElementsByClassName(
        QuestionBankController.CLOSE_QUESTION_BANK_BUTTON,
      ),
    )
    for (const element of closeQuestionBankElements) {
      element.addEventListener('click', () => {
        QuestionBankController.hideQuestionBank(questionBankContainer)
      })
    }
  }

  private static showQuestionBank(container: HTMLElement, event: Event) {
    // Update url to indciate that question bank is in open state and also
    // specify which
    const blockId = (event.currentTarget as HTMLElement).getAttribute(
      QuestionBankController.CURRENT_BLOCK_ATTRIBUTE,
    )
    const url = new URL(location.href)
    url.searchParams.set(QuestionBankController.BANK_SHOWN_URL_PARAM, blockId)
    window.history.replaceState({}, '', url.toString())
    QuestionBankController.filterQuestions()

    container.classList.remove('hidden')
    window.requestAnimationFrame(() => {
      container.classList.remove(QuestionBankController.QUESTION_BANK_HIDDEN)
    })
  }

  private static hideQuestionBank(container: HTMLElement) {
    container.querySelector('.cf-question-bank-panel').addEventListener(
      'transitionend',
      () => {
        container.classList.add('hidden')
      },
      {once: true},
    )
    container.classList.add(QuestionBankController.QUESTION_BANK_HIDDEN)
    const url = new URL(location.href)
    url.searchParams.delete(QuestionBankController.BANK_SHOWN_URL_PARAM)
    window.history.replaceState({}, '', url.toString())
  }

  /**
   * Filter questions in the question bank with the filter input string, on the question name and description.
   */
  private static filterQuestions() {
    const filterString = (
      document.getElementById(
        QuestionBankController.FILTER_ID,
      ) as HTMLInputElement
    ).value.toUpperCase()
    const blockId =
      new URL(location.href).searchParams.get(
        QuestionBankController.BANK_SHOWN_URL_PARAM,
      ) ?? 'data-screen-none'
    const questions = Array.from(
      document.getElementsByClassName(QuestionBankController.QUESTION_CLASS),
    ) as HTMLElement[]
    questions.forEach((question) => {
      const questionContents = question.innerText
      const hidden =
        !question.hasAttribute(blockId) ||
        (filterString.length &&
          !questionContents.toUpperCase().includes(filterString))
      question.classList.toggle('hidden', hidden)
    })
  }
}

new QuestionBankController()
