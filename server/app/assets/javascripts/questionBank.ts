/** The question bank controller is responsible for manipulating the question bank. */

class QuestionBankController {
  static readonly FILTER_ID = 'question-bank-filter'
  static readonly QUESTION_CLASS = 'cf-question-bank-element'

  constructor() {
    const questionBankFilter = document.getElementById(
      QuestionBankController.FILTER_ID,
    )
    if (questionBankFilter) {
      questionBankFilter.addEventListener(
        'input',
        QuestionBankController.filterQuestions,
        false,
      )
      QuestionBankController.filterQuestions()
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
