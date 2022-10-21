/** The question bank controller is responsible for manipulating the question bank. */

class QuestionBankController {
  static readonly FILTER_ID = 'question-bank-filter'
  static readonly QUESTION_CLASS = 'cf-question-bank-element'
  static readonly OPEN_QUESTION_BANK_BUTTON = 'cf-open-question-bank-button'
  static readonly CLOSE_QUESTION_BANK_BUTTON = 'cf-close-question-bank-button'
  static readonly QUESTION_BANK_CONTAINER = 'cf-question-bank-container'
  static readonly QUESTION_BANK_HIDDEN = 'cf-question-bank-hidden'

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
    const form = questionBankContainer.querySelector('form')

    const openQuestionBankElements = Array.from(
      document.getElementsByClassName(
        QuestionBankController.OPEN_QUESTION_BANK_BUTTON,
      ),
    )
    for (const element of openQuestionBankElements) {
      element.addEventListener('click', () => {
        questionBankContainer.classList.remove('hidden')
        window.requestAnimationFrame(() => {
          questionBankContainer.classList.remove(
            QuestionBankController.QUESTION_BANK_HIDDEN,
          )
        })
      })
    }
    const closeQuestionBankElements = Array.from(
      document.getElementsByClassName(
        QuestionBankController.CLOSE_QUESTION_BANK_BUTTON,
      ),
    )
    for (const element of closeQuestionBankElements) {
      element.addEventListener('click', () => {
        form.addEventListener(
          'transitionend',
          () => {
            questionBankContainer.classList.add('hidden')
          },
          {once: true},
        )
        questionBankContainer.classList.add(
          QuestionBankController.QUESTION_BANK_HIDDEN,
        )
      })
    }
  }

  /**
   * Filter questions in the question bank with the filter input string, on the question name and description.
   */
  static filterQuestions() {
    const filterString = (
      document.getElementById(
        QuestionBankController.FILTER_ID,
      ) as HTMLInputElement
    ).value.toUpperCase()
    const questions = Array.from(
      document.getElementsByClassName(QuestionBankController.QUESTION_CLASS),
    )
    questions.forEach((question) => {
      const questionElement = question as HTMLElement
      const questionContents = questionElement.innerText
      questionElement.classList.toggle(
        'hidden',
        filterString.length &&
          !questionContents.toUpperCase().includes(filterString),
      )
    })
  }
}

new QuestionBankController()
