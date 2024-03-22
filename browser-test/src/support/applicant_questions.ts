import {expect} from '@playwright/test'
import {Page} from 'playwright'
import {readFileSync, writeFileSync, unlinkSync} from 'fs'
import {waitForAnyModal, waitForPageJsLoad} from './wait'
import {BASE_URL} from './config'

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
    await this.page.selectOption(`.cf-address-state select >> nth=${index}`, {
      label: state,
    })
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
    await this.page.waitForSelector('.cf-address-zip input')

    // Check values are equal to expected
    await this.validateInputValue(street, '.cf-address-street-1 input')
    await this.validateInputValue(line2, '.cf-address-street-2 input')
    await this.validateInputValue(city, '.cf-address-city input')
    await this.validateDropdownValue(state, '.cf-address-state')
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

  async answerCheckboxQuestion(options: Array<string>) {
    for (const option of options) {
      await this.page.check(`label:has-text("${option}")`)
    }
  }

  async answerCurrencyQuestion(currency: string, index = 0) {
    await this.page.fill(`input[currency] >> nth=${index}`, currency)
  }

  async answerFileUploadQuestion(text: string, fileName = 'file.txt') {
    await this.page.setInputFiles('input[type=file]', {
      name: fileName,
      mimeType: 'text/plain',
      buffer: Buffer.from(text),
    })
  }

  /** Creates a file with the given size in MB and uploads it to the file upload question. */
  async answerFileUploadQuestionWithMbSize(mbSize: int) {
    const filePath = 'file-size-' + mbSize + '-mb.txt'
    writeFileSync(filePath, 'C'.repeat(mbSize * 1024 * 1024))
    await this.page.setInputFiles('input[type=file]', filePath)
    unlinkSync(filePath)
  }

  async answerIdQuestion(id: string, index = 0) {
    await this.page.fill(`input[type="text"] >> nth=${index}`, id)
  }

  async answerRadioButtonQuestion(checked: string) {
    await this.page.check(`text=${checked}`)
  }

  async answerDropdownQuestion(selected: string, index = 0) {
    await this.page.selectOption(
      `.cf-dropdown-question select >> nth=${index}`,
      {
        label: selected,
      },
    )
  }

  async answerPhoneQuestion(phone: string, index = 0) {
    await this.page.fill(`.cf-question-phone input >> nth=${index}`, phone)
  }

  async answerNumberQuestion(number: string, index = 0) {
    await this.page.fill(`input[type="number"] >> nth=${index}`, number)
  }

  async checkNumberQuestionValue(number: string) {
    await this.validateInputTypePresent('number')
    await this.validateInputValue(number)
  }

  async answerDateQuestion(date: string, index = 0) {
    await this.page.fill(`input[type="date"] >> nth=${index}`, date)
  }

  async checkDateQuestionValue(date: string) {
    await this.validateInputTypePresent('date')
    await this.validateInputValue(date)
  }

  async answerTextQuestion(text: string, index = 0) {
    await this.page.fill(`input[type="text"] >> nth=${index}`, text)
  }

  async answerEmailQuestion(email: string, index = 0) {
    await this.page.fill(`input[type="email"] >> nth=${index}`, email)
  }

  async checkEmailQuestionValue(email: string) {
    await this.validateInputTypePresent('email')
    await this.validateInputValue(email)
  }

  async addEnumeratorAnswer(entityName: string) {
    await this.page.click('button:has-text("Add entity")')
    // TODO(leonwong): may need to specify row index to wait for newly added row.
    await this.page.fill(
      '#enumerator-fields .cf-enumerator-field:last-of-type input[data-entity-input]',
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

  /** On the review page, click "Answer" on a previously unanswered question. */
  async answerQuestionFromReviewPage(questionText: string) {
    await this.page.click(
      `.cf-applicant-summary-row:has(div:has-text("${questionText}")) a:has-text("Answer")`,
    )
    await waitForPageJsLoad(this.page)
  }

  /** On the review page, click "Edit" to change an answer to a previously answered question. */
  async editQuestionFromReviewPage(questionText: string) {
    await this.page.click(
      `.cf-applicant-summary-row:has(div:has-text("${questionText}")) a:has-text("Edit")`,
    )
    await waitForPageJsLoad(this.page)
  }

  async validateInputTypePresent(type: string) {
    await this.page.waitForSelector(`input[type="${type}"]`)
  }

  async validateDropdownValue(value: string, dropdownId: string) {
    expect(await this.page.innerText(dropdownId)).toContain(value)
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

    // If we are as a guest, we will get a prompt to log in before continuing to the
    // application. Bypass this to continue as a guest.
    const loginPromptButton = await this.page.$(
      `[id^="bypass-login-prompt-button-"]:visible`,
    )
    if (loginPromptButton !== null) {
      await loginPromptButton.click()
    }

    await waitForPageJsLoad(this.page)
  }

  async clickApplyToAnotherProgramButton() {
    await this.page.click('button:has-text("Apply to another program")')
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

  async gotoApplicantHomePage() {
    await this.page.goto(BASE_URL)
    await waitForPageJsLoad(this.page)
  }

  async seeEligibilityTag(programName: string, isEligible: boolean) {
    const cardLocator = this.page.locator('.cf-application-card', {
      has: this.page.locator(`:text("${programName}")`),
    })
    const tag = isEligible ? '.cf-eligible-tag' : '.cf-not-eligible-tag'
    expect(await cardLocator.locator(tag).count()).toEqual(1)
  }

  async seeNoEligibilityTags(programName: string) {
    const cardLocator = this.page.locator('.cf-application-card', {
      has: this.page.locator(`:text("${programName}")`),
    })
    expect(await cardLocator.locator('.cf-eligible-tag').count()).toEqual(0)
    expect(await cardLocator.locator('.cf-not-eligible-tag').count()).toEqual(0)
  }

  async expectPrograms({
    wantNotStartedPrograms,
    wantInProgressPrograms,
    wantSubmittedPrograms,
  }: {
    wantNotStartedPrograms: string[]
    wantInProgressPrograms: string[]
    wantSubmittedPrograms: string[]
  }) {
    const gotNotStartedProgramNames =
      await this.programNamesForSection('Not started')
    const gotInProgressProgramNames =
      await this.programNamesForSection('In progress')
    const gotSubmittedProgramNames =
      await this.programNamesForSection('Submitted')

    // Sort results before comparing since we don't care about order.
    gotNotStartedProgramNames.sort()
    wantNotStartedPrograms.sort()
    gotInProgressProgramNames.sort()
    wantInProgressPrograms.sort()
    gotSubmittedProgramNames.sort()
    wantSubmittedPrograms.sort()

    expect(gotNotStartedProgramNames).toEqual(wantNotStartedPrograms)
    expect(gotInProgressProgramNames).toEqual(wantInProgressPrograms)
    expect(gotSubmittedProgramNames).toEqual(wantSubmittedPrograms)
  }

  async expectCommonIntakeForm(commonIntakeFormName: string) {
    const commonIntakeFormSectionNames =
      await this.programNamesForSection('Get Started')
    expect(commonIntakeFormSectionNames).toEqual([commonIntakeFormName])
  }

  private programNamesForSection(sectionName: string): Promise<string[]> {
    const sectionLocator = this.page.locator(
      '.cf-application-program-section',
      {has: this.page.locator(`:text("${sectionName}")`)},
    )
    const programTitlesLocator = sectionLocator.locator(
      '.cf-application-card .cf-application-card-title',
    )
    return programTitlesLocator.allTextContents()
  }

  async clickNext() {
    await this.page.click('text="Save and next"')
    await waitForPageJsLoad(this.page)
  }

  async clickContinue() {
    await this.page.click('text="Continue"')
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

  async clickSubmit() {
    await this.page.click('text="Submit"')
    await waitForPageJsLoad(this.page)
  }

  async clickDownload() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download PDF"'),
    ])
    const path = await downloadEvent.path()
    if (path === null || readFileSync(path, 'utf8').length === 0) {
      throw new Error('download failed')
    }
    await waitForPageJsLoad(this.page)
  }

  async clickConfirmAddress() {
    await this.page.getByRole('button', {name: 'Confirm address'}).click()
    await waitForPageJsLoad(this.page)
  }

  async clickEdit() {
    await this.page.click('text="Edit"')
    await waitForPageJsLoad(this.page)
  }

  async clickGoBackAndEdit() {
    await this.page.getByRole('button', {name: 'Go back and edit'}).click()
    await waitForPageJsLoad(this.page)
  }

  async deleteEnumeratorEntity(entityName: string) {
    this.page.once('dialog', (dialog) => {
      void dialog.accept()
    })
    await this.page.click(
      `.cf-enumerator-field:has(input[value="${entityName}"]) button`,
    )
  }

  async deleteEnumeratorEntityByIndex(entityIndex: number) {
    this.page.once('dialog', (dialog) => {
      void dialog.accept()
    })
    await this.page.click(`:nth-match(:text("Remove entity"), ${entityIndex})`)
  }

  async downloadSingleQuestionFromReviewPage() {
    // Assert that we're on the review page.
    expect(await this.page.innerText('h2')).toContain(
      'Program application summary',
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

  async returnToProgramsFromSubmissionPage() {
    // Assert that we're on the submission page.
    await this.expectConfirmationPage()
    await this.clickApplyToAnotherProgramButton()

    // If we are a guest, we will get a prompt to log in before going back to the
    // programs page. Bypass this to continue as a guest.
    const pageContent = await this.page.textContent('html')
    if (pageContent!.includes('Continue without an account')) {
      await this.page.click('text="Continue without an account"')
    }

    // Ensure that we redirected to the programs list page.
    await this.expectProgramsPage()
  }

  async expectProgramsPage() {
    await waitForPageJsLoad(this.page)
    expect(this.page.url().split('/').pop()).toEqual('programs')
  }

  async expectReviewPage() {
    expect(await this.page.innerText('h2')).toContain(
      'Program application summary',
    )
  }

  async expectConfirmationPage() {
    expect(await this.page.innerText('h1')).toContain(
      'Application confirmation',
    )
  }

  async expectCommonIntakeReviewPage() {
    expect(await this.page.innerText('h2')).toContain(
      'Benefits pre-screener summary',
    )
  }

  async expectCommonIntakeConfirmationPage(
    wantUpsell: boolean,
    wantTrustedIntermediary: boolean,
    wantEligiblePrograms: string[],
  ) {
    if (wantTrustedIntermediary) {
      expect(await this.page.innerText('h1')).toContain(
        'Programs your client may qualify for',
      )
    } else {
      expect(await this.page.innerText('h1')).toContain(
        'Programs you may qualify for',
      )
    }

    const upsellLocator = this.page.locator(
      ':text("Create an account or sign in"):visible',
    )
    if (wantUpsell) {
      expect(await upsellLocator.count()).toEqual(1)
    } else {
      expect(await upsellLocator.count()).toEqual(0)
    }

    const programLocator = this.page.locator(
      '.cf-applicant-cif-eligible-program-name',
    )
    if (wantEligiblePrograms.length == 0) {
      expect(await programLocator.count()).toEqual(0)
    } else {
      expect(await programLocator.count()).toEqual(wantEligiblePrograms.length)
      const allProgramTitles = await programLocator.allTextContents()
      expect(allProgramTitles.sort()).toEqual(wantEligiblePrograms.sort())
    }
  }

  async expectIneligiblePage() {
    expect(await this.page.innerText('h2')).toContain('you may not qualify')
  }

  async clickGoBackAndEditOnIneligiblePage() {
    await this.page.click('text="Go back and edit"')
    await waitForPageJsLoad(this.page)
  }

  async expectDuplicatesPage() {
    expect(await this.page.innerText('h2')).toContain(
      'There are no changes to save',
    )
  }

  async expectIneligibleQuestion(questionText: string) {
    expect(await this.page.innerText('li')).toContain(questionText)
  }

  async expectIneligibleQuestionsCount(number: number) {
    expect(await this.page.locator('li').count()).toEqual(number)
  }

  async expectQuestionIsNotEligible(questionText: string) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    expect(await questionLocator.count()).toEqual(1)
    expect(
      await questionLocator.locator('.cf-applicant-not-eligible-text').count(),
    ).toEqual(1)
  }

  async expectQuestionHasNoEligibilityIndicator(questionText: string) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    expect(await questionLocator.count()).toEqual(1)
    expect(
      await questionLocator.locator('.cf-applicant-not-eligible-text').count(),
    ).toEqual(0)
  }

  async expectVerifyAddressPage(validAddress: boolean) {
    const header = validAddress ? 'Verify address' : 'No valid address found'
    expect(await this.page.innerText('h2')).toContain(header)
  }

  async expectAddressPage() {
    expect(await this.page.innerText('legend')).toContain('With Correction')
  }

  async selectAddressSuggestion(addressName: string) {
    await this.page.check(`label:has-text("${addressName}")`)
  }

  async expectQuestionAnsweredOnReviewPage(
    questionText: string,
    answerText: string,
  ) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    expect(await questionLocator.count()).toEqual(1)
    const summaryRowText = await questionLocator.innerText()
    expect(summaryRowText.includes(answerText)).toBeTruthy()
  }

  async submitFromReviewPage() {
    // Assert that we're on the review page.
    await this.expectReviewPage()

    // Click on submit button.
    await this.clickSubmit()
  }

  async downloadFromConfirmationPage() {
    // Assert that we're on the review page.
    await this.expectConfirmationPage()

    // Click on download button.
    await this.clickDownload()
  }

  async validateHeader(lang: string) {
    await expect(this.page.locator('html')).toHaveAttribute('lang', lang)
    expect(await this.page.innerHTML('head')).toContain(
      '<meta name="viewport" content="width=device-width, initial-scale=1">',
    )
  }

  async validateQuestionIsOnPage(questionText: string) {
    await expect(
      this.page
        .locator('.cf-applicant-question-text')
        .filter({hasText: questionText}),
    ).toBeVisible()
  }

  async validatePreviouslyAnsweredText(questionText: string) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    await expect(
      questionLocator.locator('.cf-applicant-question-previously-answered'),
    ).toBeVisible()
  }

  async validateNoPreviouslyAnsweredText(questionText: string) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    await expect(
      questionLocator.locator('.cf-applicant-question-previously-answered'),
    ).toBeHidden()
  }

  async seeStaticQuestion(questionText: string) {
    expect(await this.page.textContent('html')).toContain(questionText)
  }

  async expectErrorOnReviewModal() {
    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain(
      `Questions on this page are not complete`,
    )
    expect(await modal.innerText()).toContain(
      `Continue to review page without saving`,
    )
    expect(await modal.innerText()).toContain(`Stay and fix your answers`)
  }
  async clickReviewWithoutSaving() {
    await this.page.click(
      'button:has-text("Continue to review page without saving")',
    )
  }

  async expectErrorOnPreviousModal() {
    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain(
      `Questions on this page are not complete`,
    )
    expect(await modal.innerText()).toContain(
      `Continue to previous questions without saving`,
    )
    expect(await modal.innerText()).toContain(`Stay and fix your answers`)
  }

  async clickPreviousWithoutSaving() {
    await this.page.click(
      'button:has-text("Continue to previous questions without saving")',
    )
  }

  async clickStayAndFixAnswers() {
    await this.page.click('button:has-text("Stay and fix your answers")')
  }

  async completeApplicationWithPaiQuestions(
    programName: string,
    firstName: string,
    middleName: string,
    lastName: string,
    email: string,
    phone: string,
  ) {
    await this.applyProgram(programName)
    await this.answerNameQuestion(firstName, lastName, middleName)
    await this.answerEmailQuestion(email)
    await this.answerPhoneQuestion(phone)
    await this.clickNext()
    await this.submitFromReviewPage()
    await this.page.click('text=End session')
  }
}
