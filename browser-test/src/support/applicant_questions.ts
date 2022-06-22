import {Page} from 'playwright'
import {readFileSync} from 'fs'
import {waitForPageJsLoad} from './wait'

export class ApplicantQuestions {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async answerAddressQuestion(
    street: string,
    line2: string,
    city: string,
    state: string,
    zip: string,
    index = 0,
  ) {
    await this.page.fill(`.cf-address-street-1 input >> nth=${index}`, street)
    await this.page.fill(`.cf-address-street-2 input >> nth=${index}`, line2)
    await this.page.fill(`.cf-address-city input >> nth=${index}`, city)
    await this.page.fill(`.cf-address-state input >> nth=${index}`, state)
    await this.page.fill(`.cf-address-zip input >> nth=${index}`, zip)
  }

  async checkAddressQuestionValue(
    street: string,
    line2: string,
    city: string,
    state: string,
    zip: string,
  ) {
    // Verify elements are present
    await this.page.waitForSelector('.cf-address-street-1 input')
    await this.page.waitForSelector('.cf-address-street-2 input')
    await this.page.waitForSelector('.cf-address-city input')
    await this.page.waitForSelector('.cf-address-state input')
    await this.page.waitForSelector('.cf-address-zip input')

    // Check values are equal to expected
    await this.validateInputValue(street, '.cf-address-street-1 input')
    await this.validateInputValue(line2, '.cf-address-street-2 input')
    await this.validateInputValue(city, '.cf-address-city input')
    await this.validateInputValue(state, '.cf-address-state input')
    await this.validateInputValue(zip, '.cf-address-zip input')
  }

  async answerNameQuestion(
    firstName: string,
    lastName: string,
    middleName = '',
    index = 0,
  ) {
    await this.page.fill(`.cf-name-first input >> nth=${index}`, firstName)
    await this.page.fill(`.cf-name-middle input >> nth=${index}`, middleName)
    await this.page.fill(`.cf-name-last input >> nth=${index}`, lastName)
  }

  async checkNameQuestionValue(
    firstName: string,
    lastName: string,
    middleName = '',
  ) {
    // Verify elements are present
    await this.page.waitForSelector('.cf-name-first input')
    await this.page.waitForSelector('.cf-name-middle input')
    await this.page.waitForSelector('.cf-name-last input')

    // Check values are equal to expected
    await this.validateInputValue(firstName, '.cf-name-first input')
    await this.validateInputValue(middleName, '.cf-name-middle input')
    await this.validateInputValue(lastName, '.cf-name-last input')
  }

  async answerCheckboxQuestion(checked: Array<string>) {
    for (const index in checked) {
      await this.page.check(`label:has-text("${checked[index]}")`)
    }
  }

  async answerCurrencyQuestion(currency: string, index = 0) {
    await this.page.fill(`input[currency] >> nth=${index}`, currency)
  }

  async answerFileUploadQuestion(text: string) {
    await this.page.setInputFiles('input[type=file]', {
      name: 'file.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from(text),
    })
  }

  async answerIdQuestion(id: string) {
    await this.page.fill('input[type="text"]', id)
  }

  async answerRadioButtonQuestion(checked: string) {
    await this.page.check(`text=${checked}`)
  }

  async answerDropdownQuestion(selected: string) {
    await this.page.selectOption('.cf-dropdown-question select', {
      label: selected,
    })
  }

  async answerNumberQuestion(number: string) {
    await this.page.fill('input[type="number"]', number)
  }

  async checkNumberQuestionValue(number: string) {
    await this.validateInputTypePresent('number')
    await this.validateInputValue(number)
  }

  async answerDateQuestion(date: string) {
    await this.page.fill('input[type="date"]', date)
  }

  async checkDateQuestionValue(date: string) {
    await this.validateInputTypePresent('date')
    await this.validateInputValue(date)
  }

  async answerTextQuestion(text: string) {
    await this.page.fill('input[type="text"]', text)
  }

  async answerEmailQuestion(email: string) {
    await this.page.fill('input[type="email"]', email)
  }

  async checkEmailQuestionValue(email: string) {
    await this.validateInputTypePresent('email')
    await this.validateInputValue(email)
  }

  async addEnumeratorAnswer(entityName: string) {
    await this.page.click('button:text("add entity")')
    // TODO(leonwong): may need to specify row index to wait for newly added row.
    await this.page.fill(
      '#enumerator-fields .cf-enumerator-field:last-of-type input',
      entityName,
    )
  }

  async checkEnumeratorAnswerValue(entityName: string, index: number) {
    await this.page.waitForSelector(
      `#enumerator-fields .cf-enumerator-field:nth-of-type(${index}) input`,
    )
    await this.validateInputValue(
      entityName,
      `#enumerator-fields .cf-enumerator-field:nth-of-type(${index}) input`,
    )
  }

  async validateInputTypePresent(type: string) {
    await this.page.waitForSelector(`input[type="${type}"]`)
  }

  async validateInputValue(value: string, element = 'input') {
    await this.page.waitForSelector(`${element}[value="${value}"]`)
  }

  async applyProgram(programName: string) {
    // User clicks the apply button on an application card. It takes them to the application info page.
    await this.clickApplyProgramButton(programName)

    // The user can see the application preview page. Clicking on apply sends them to the first unanswered question.
    await this.page.click(`#continue-application-button`)
    await waitForPageJsLoad(this.page)
  }

  async clickApplyProgramButton(programName: string) {
    await this.page.click(
      `.cf-application-card:has-text("${programName}") .cf-apply-button`,
    )
    await waitForPageJsLoad(this.page)
  }

  async expectProgramPublic(programName: string, description: string) {
    const tableInnerText = await this.page.innerText('main')

    expect(tableInnerText).toContain(programName)
    expect(tableInnerText).toContain(description)
  }

  async expectProgramHidden(programName: string) {
    const tableInnerText = await this.page.innerText('main')

    expect(tableInnerText).not.toContain(programName)
  }

  async clickNext() {
    await this.page.click('text="Save and next"')
    await waitForPageJsLoad(this.page)
  }

  async clickPrevious() {
    await this.page.click('text="Previous"')
    await waitForPageJsLoad(this.page)
  }

  async clickSkip() {
    await this.page.click('text="Skip"')
    await waitForPageJsLoad(this.page)
  }

  async clickReview() {
    await this.page.click('text="Review"')
    await waitForPageJsLoad(this.page)
  }

  async clickUpload() {
    await this.page.click('text="Upload"')
    await waitForPageJsLoad(this.page)
  }

  async deleteEnumeratorEntity(entityName: string) {
    this.page.once('dialog', async (dialog) => {
      await dialog.accept()
    })
    await this.page.click(
      `.cf-enumerator-field:has(input[value="${entityName}"]) button`,
    )
  }

  async deleteEnumeratorEntityByIndex(entityIndex: number) {
    this.page.once('dialog', async (dialog) => {
      await dialog.accept()
    })
    await this.page.click(`:nth-match(:text("Remove Entity"), ${entityIndex})`)
  }

  async downloadSingleQuestionFromReviewPage() {
    // Assert that we're on the review page.
    expect(await this.page.innerText('h1')).toContain(
      'Program application review',
    )

    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('a:has-text("click to download")'),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  async submitFromReviewPage(_programName: string) {
    // Assert that we're on the review page.
    expect(await this.page.innerText('h1')).toContain(
      'Program application review',
    )

    // Click on submit button.
    await this.page.click('text="Submit"')
    await waitForPageJsLoad(this.page)

    await this.page.click('text="Apply to another program"')
    await waitForPageJsLoad(this.page)

    // Ensure that we redirected to the programs list page.
    expect(await this.page.url().split('/').pop()).toEqual('programs')
  }

  async submitFromPreviewPage(_programName: string) {
    // Assert that we're on the preview page.
    expect(await this.page.innerText('h1')).toContain(
      'Program application preview',
    )

    // Click on submit button.
    await this.page.click('text="Submit"')
    await waitForPageJsLoad(this.page)

    await this.page.click('text="Apply to another program"')
    await waitForPageJsLoad(this.page)

    // Ensure that we redirected to the programs list page.
    expect(await this.page.url().split('/').pop()).toEqual('programs')
  }

  async validateHeader(lang: string) {
    expect(await this.page.getAttribute('html', 'lang')).toEqual(lang)
    expect(await this.page.innerHTML('head')).toContain(
      '<meta name="viewport" content="width=device-width, initial-scale=1">',
    )
  }

  async seeStaticQuestion(questionText: string) {
    expect(await this.page.textContent('html')).toContain(questionText)
  }
}
