import { Page } from 'playwright'

const { BASE_URL } = process.env

export class AdminQuestions {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async addDropdownQuestion(questionName: string, description = 'test description',
    questionText = 'test question text',
    helpText = 'test question help text',
    options: Array<string>) {
    await this.page.goto(BASE_URL + '/admin/questions')
    await this.page.click('#create-question-button')

    await this.page.click('#create-dropdown-question')
    this.fillBaseQuestionForm(questionName, description, questionText, helpText)
    for (var index in options) {
      await this.page.click('#add-new-option')
      await this.page.fill('text=Question option', options[index])
    }

    this.assertQuestionExists(questionName, questionText)
  }

  async addNameQuestion(questionName: string,
    path: string,
    description = 'test description',
    questionText = 'test question text',
    helpText = 'test question help text') {
    await this.page.goto(BASE_URL + '/admin/questions')
    await this.page.click('#create-question-button')

    await this.page.click('#create-name-question')
    this.fillBaseQuestionForm(questionName, description, questionText, helpText)
    this.assertQuestionExists(questionName, questionText)
  }

  async fillBaseQuestionForm(questionName: string, description: string, questionText: string, helpText: string) {
    await this.page.fill('text="Name"', questionName)
    await this.page.fill('text=Description', description)
    await this.page.fill('text=Question Text', questionText)
    await this.page.fill('text=Question help text', helpText)
  }

  async assertQuestionExists(questionName: string, questionText: string) {
    await this.page.click('text=Create')
    expect(await this.page.innerText('h1')).toEqual('All Questions')

    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
  }
}
