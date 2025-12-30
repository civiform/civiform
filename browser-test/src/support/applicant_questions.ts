import {expect, Locator} from '@playwright/test'
import {Page} from 'playwright'
import {readFileSync, writeFileSync, unlinkSync} from 'fs'
import {
  waitForAnyModalLocator,
  waitForPageJsLoad,
  waitForHtmxReady,
} from './wait'
import {BASE_URL} from './config'
import {
  ApplicantProgramList,
  CardSectionName,
} from '../support/applicant_program_list'
import {ApplicantProgramOverview} from './applicant_program_overview'

export class ApplicantQuestions {
  public page!: Page
  private applicantProgramList: ApplicantProgramList
  private applicantProgramOverview: ApplicantProgramOverview

  constructor(page: Page) {
    this.page = page
    this.applicantProgramList = new ApplicantProgramList(page)
    this.applicantProgramOverview = new ApplicantProgramOverview(page)
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
      value: state,
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
    nameSuffix = '',
    index = 0,
  ) {
    await this.page.fill(`.cf-name-first input >> nth=${index}`, firstName)
    await this.page.fill(`.cf-name-middle input >> nth=${index}`, middleName)
    await this.page.fill(`.cf-name-last input >> nth=${index}`, lastName)
    if (nameSuffix.length != 0) {
      await this.answerDropdownQuestion(nameSuffix)
    }
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
      mimeType: 'image/png',
      buffer: Buffer.from(text),
    })
  }

  async answerFileUploadQuestionFromAssets(fileName: string) {
    await this.page.setInputFiles('input[type=file]', 'src/assets/' + fileName)
  }

  /** Creates a file with the given size in MB and uploads it to the file upload question. */
  async answerFileUploadQuestionWithMbSize(mbSize: int) {
    const filePath = 'file-size-' + mbSize + '-mb.pdf'
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

  async answerYesNoQuestion(checked: string, order = 0) {
    await this.page.getByText(checked, {exact: true}).nth(order).check()
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

  async answerMemorableDateQuestion(
    year: string,
    month: string,
    day: string,
    index = 0,
  ) {
    await this.page.fill(`.cf-date-year input >> nth=${index}`, year)

    // Empty string means "delete this answer". The dropdown default is "Select"
    if (month == '') {
      month = 'Select'
    }

    await this.page.selectOption(`.cf-date-month select >> nth=${index}`, {
      label: month,
    })
    await this.page.fill(`.cf-date-day input >> nth=${index}`, day)
  }

  async checkMemorableDateQuestionValue(
    year: string,
    month: string,
    day: string,
    index = 0,
  ) {
    const yearValue = await this.page
      .locator(`.cf-date-year input >> nth=${index}`)
      .inputValue()
    expect(this.trimLeadingZeros(yearValue)).toBe(year)

    const monthValue = await this.page
      .locator(`.cf-date-month select >> nth=${index}`)
      .inputValue()
    expect(this.trimLeadingZeros(monthValue)).toBe(month)

    const dayValue = await this.page
      .locator(`.cf-date-day input >> nth=${index}`)
      .inputValue()
    expect(this.trimLeadingZeros(dayValue)).toBe(day)
  }

  trimLeadingZeros(str: string): string {
    return str.replace(/^0+/, '')
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
    await this.page
      .locator(
        '#enumerator-fields .cf-enumerator-field input[data-entity-input]:visible',
      )
      .last()
      .fill(entityName)
  }

  async editEnumeratorAnswer(
    existingEntityName: string,
    newEntityName: string,
  ) {
    await this.page
      .locator(
        `#enumerator-fields .cf-enumerator-field input[value="${existingEntityName}"]`,
      )
      .fill(newEntityName)
  }

  async checkEnumeratorAnswerValue(entityName: string, index: number) {
    await this.page
      .locator(`#enumerator-fields .cf-enumerator-field >> nth=${index}`)
      .getByText(entityName)
      .isVisible()
  }

  /** On the review page, click "Answer" on a previously unanswered question. */
  async answerQuestionFromReviewPage(questionText: string) {
    await this.page.click(
      `.cf-applicant-summary-row:has(div:has-text("${questionText}")) a:has-text("Answer")`,
    )
    await waitForPageJsLoad(this.page)
  }

  async expectQuestionOnReviewPage(questionText: string) {
    await expect(this.page.getByText(questionText)).toBeVisible()
  }

  /** On the review page, click "Edit" to change an answer to a previously answered question. */
  async editQuestionFromReviewPage(questionText: string) {
    const locator = this.page.locator(
      `.block-summary:has(div:has-text("${questionText}")) a:has-text("Edit")`,
    )
    await locator.click()
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

  async applyProgram(
    programName: string,
    showProgramOverviewPage = true,
    translatedOverviewTitle?: string,
    translatedLinkText?: string,
  ) {
    await this.clickApplyProgramButton(programName)

    // Clicking on "Apply" navigates to the program overview page if it's the applicant's first time applying.
    // If the applicant has already submitted an application, it will take them to the review page.
    // If the applicant has a partially completed application, it will take them to the page with the first unanswered question.
    if (showProgramOverviewPage) {
      await this.applicantProgramOverview.startApplicationFromProgramOverviewPage(
        programName,
        translatedOverviewTitle,
        translatedLinkText,
      )
    }

    await waitForPageJsLoad(this.page)
  }

  async clickApplyProgramButton(programName: string) {
    await this.page
      .locator('.cf-application-card')
      .filter({hasText: programName})
      .locator('.cf-apply-button')
      .click()

    // If we are as a guest, we will get a prompt to log in before continuing to the
    // application. Bypass this to continue as a guest.
    const loginPromptButton = this.page
      .locator('[id^="bypass-login-prompt-button-"]')
      .and(this.page.locator(':visible'))

    if ((await loginPromptButton.count()) > 0) {
      await loginPromptButton.click()
    }

    await waitForPageJsLoad(this.page)
  }

  async clickBreadcrumbHomeLink() {
    await this.page.getByRole('link', {name: 'Home'}).click()
  }

  async clickBreadcrumbProgramLink(programName: string) {
    await this.page.getByRole('link', {name: `${programName}`}).click()
  }

  async clickApplyToAnotherProgramButton() {
    await this.page.click('text="Apply to another program"')
  }

  async clickApplyToProgramsButton() {
    await this.page.click('text="Apply to programs"')
    await waitForPageJsLoad(this.page)
  }

  async clickBack() {
    await this.page.click('text="Back"')
  }

  async clickBackToHomepageButton() {
    await this.page.click('text="Back to homepage"')
  }

  /**
   * @deprecated
   */
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
    wantInProgressOrSubmittedPrograms,
  }: {
    wantNotStartedPrograms: string[]
    wantInProgressOrSubmittedPrograms: string[]
  }) {
    await this.expectProgramsinCorrectSections(
      {
        expectedProgramsInMyApplicationsSection:
          wantInProgressOrSubmittedPrograms,
        expectedProgramsInProgramsAndServicesSection: wantNotStartedPrograms,
        expectedProgramsInRecommendedSection: [],
        expectedProgramsInOtherProgramsSection: [],
      },
      /* filtersOn= */ false,
    )
  }

  async filterProgramsAndExpectInCorrectSections(
    {
      filterCategory,
      expectedProgramsInMyApplicationsSection,
      expectedProgramsInProgramsAndServicesSection,
      expectedProgramsInRecommendedSection,
      expectedProgramsInOtherProgramsSection,
    }: {
      filterCategory: string
      expectedProgramsInMyApplicationsSection: string[]
      expectedProgramsInProgramsAndServicesSection: string[]
      expectedProgramsInRecommendedSection: string[]
      expectedProgramsInOtherProgramsSection: string[]
    },
    /* Toggle whether filters have been selected */ filtersOn = false,
  ) {
    await this.filterProgramsByCategory(filterCategory)

    // Check the program count in the section headings
    await expect(
      this.page.getByRole('heading', {
        name: `Programs based on your selections (${expectedProgramsInRecommendedSection.length})`,
      }),
    ).toBeVisible()
    await expect(
      this.page.getByRole('heading', {
        name: `Other programs and services (${expectedProgramsInOtherProgramsSection.length})`,
      }),
    ).toBeVisible()

    await this.expectProgramsinCorrectSections(
      {
        expectedProgramsInMyApplicationsSection,
        expectedProgramsInProgramsAndServicesSection,
        expectedProgramsInRecommendedSection,
        expectedProgramsInOtherProgramsSection,
      },
      filtersOn,
    )
  }

  async expectProgramsinCorrectSections(
    {
      expectedProgramsInMyApplicationsSection,
      expectedProgramsInProgramsAndServicesSection,
      expectedProgramsInRecommendedSection,
      expectedProgramsInOtherProgramsSection,
    }: {
      expectedProgramsInMyApplicationsSection: string[]
      expectedProgramsInProgramsAndServicesSection: string[]
      expectedProgramsInRecommendedSection: string[]
      expectedProgramsInOtherProgramsSection: string[]
    },
    /* Toggle whether filters have been selected */ filtersOn = false,
  ) {
    const gotMyApplicationsProgramNames = await this.programNamesForSection(
      CardSectionName.MyApplications,
    )

    let gotRecommendedProgramNames
    let gotOtherProgramNames
    let gotProgramsAndServicesNames

    if (filtersOn) {
      const recommendedSectionLocator = this.page.locator(
        '.cf-application-program-section',
        {has: this.page.locator(':text("Programs based on your selections")')},
      )
      gotRecommendedProgramNames = await this.findProgramsWithSectionLocator(
        recommendedSectionLocator,
      )
      gotRecommendedProgramNames.sort()

      const otherSectionLocator = this.page.locator(
        '.cf-application-program-section',
        {has: this.page.locator(':text("Other programs and services")')},
      )
      gotOtherProgramNames =
        await this.findProgramsWithSectionLocator(otherSectionLocator)
      gotOtherProgramNames.sort()
    } else {
      gotProgramsAndServicesNames = await this.programNamesForSection(
        CardSectionName.ProgramsAndServices,
      )
      gotProgramsAndServicesNames.sort()
    }

    // Sort results before comparing since we don't care about order.
    expectedProgramsInMyApplicationsSection.sort()
    expectedProgramsInProgramsAndServicesSection.sort()
    expectedProgramsInRecommendedSection.sort()
    expectedProgramsInOtherProgramsSection.sort()
    gotMyApplicationsProgramNames.sort()

    expect(gotMyApplicationsProgramNames).toEqual(
      expectedProgramsInMyApplicationsSection,
    )

    if (filtersOn) {
      expect(gotRecommendedProgramNames).toEqual(
        expectedProgramsInRecommendedSection,
      )
      expect(gotOtherProgramNames).toEqual(
        expectedProgramsInOtherProgramsSection,
      )
    } else {
      expect(gotProgramsAndServicesNames).toEqual(
        expectedProgramsInProgramsAndServicesSection,
      )
    }
  }

  async expectPreScreenerForm(preScreenerFormName: string) {
    const sectionLocator = this.page.locator('[aria-label="Get Started"]')

    const programTitlesLocator = sectionLocator.locator(
      '.cf-application-card-title',
    )

    await expect(programTitlesLocator).toHaveText(preScreenerFormName)
  }

  private programNamesForSection(
    sectionName: CardSectionName,
  ): Promise<string[]> {
    const sectionLocator =
      this.applicantProgramList.getCardSectionLocator(sectionName)
    return this.findProgramsWithSectionLocator(sectionLocator)
  }

  private findProgramsWithSectionLocator(sectionLocator: Locator) {
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

  async clickContinueEditing() {
    await this.page.click('text="Continue editing"')
    await waitForPageJsLoad(this.page)
  }

  async clickExitApplication() {
    await this.page.click('text="Exit application"')
    await waitForPageJsLoad(this.page)
  }

  async clickPrevious() {
    await this.page.click('text="Previous"')
    await waitForPageJsLoad(this.page)
  }

  /**
   * @deprecated
   */
  async clickSkip() {
    await this.page.click('text="Skip"')
    await waitForPageJsLoad(this.page)
  }

  async clickReview() {
    const reviewButton = 'text="Review and submit"'
    await this.page.click(reviewButton)
    await waitForPageJsLoad(this.page)
  }

  async clickSubmit() {
    await this.page.click('text="Submit"')
    await waitForPageJsLoad(this.page)
  }

  async clickSubmitApplication() {
    await this.page.click('text="Submit application"')
    await waitForPageJsLoad(this.page)
  }

  async expectSubmitApplicationButton() {
    await expect(
      this.page.getByRole('button', {name: 'Submit application'}),
    ).toBeVisible()
  }

  async clickDownload() {
    const downloadButton = 'text="Download your application"'
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(downloadButton),
    ])
    const path = await downloadEvent.path()
    if (!path) {
      throw new Error('Download failed: File path is null.')
    }

    const fileContent = readFileSync(path, 'utf8')
    if (fileContent.length === 0) {
      throw new Error('Download failed: File content is empty.')
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

  async clickEditMyResponses() {
    await this.page.getByRole('button', {name: 'Edit my responses'}).click()
    await waitForPageJsLoad(this.page)
  }

  /**
   * Remove the enumerator answer specified by entityName.
   * Note: only works if the value is in the DOM, i.e. was set at page load. Does not work if the
   * value has been filled after the page loaded. Explanation: https://stackoverflow.com/q/10645552
   */
  async deleteEnumeratorEntity(entityName: string) {
    this.page.once('dialog', async (dialog) => {
      await dialog.accept()
    })
    await this.page
      .locator(`.cf-enumerator-field:has(input[value="${entityName}"])`)
      .getByRole('button')
      .click()
  }

  /** Remove the enumerator entity at entityIndex (1-based) */
  async deleteEnumeratorEntityByIndex(entityIndex: number) {
    this.page.once('dialog', async (dialog) => {
      await dialog.accept()
    })
    await this.page
      .locator(
        `#enumerator-fields .cf-enumerator-field .cf-enumerator-delete-button >> nth=${entityIndex}`,
      )
      .click()
  }

  /**
   * On the review page, users can download already-uploaded files;
   * this method downloads one of them and returns the file content.
   *
   * The anchor text for the download link is the name
   * of the file.
   */
  async downloadSingleQuestionFromReviewPage(
    downloadText = 'click to download',
  ) {
    // Assert that we're on the review page.
    await expect(this.page.getByText('Review and submit')).toBeVisible()

    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(`a:has-text("${downloadText}")`),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  /**
   * On the upload page, users can download already-uploaded files;
   * this method downloads the one specified by the user returns the file content.
   */
  async downloadFileFromUploadPage(fileName: string) {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.getByText(fileName).click(),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  async downloadFileFromReviewPage(fileName: string) {
    await expect(
      this.page.getByRole('heading', {name: 'Program application summary'}),
    ).toBeVisible()

    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.getByText(fileName).click(),
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
    await this.clickBackToHomepageButton()

    // If we are a guest, we will get a prompt to log in before going back to the
    // programs page. Bypass this to continue as a guest.
    const pageContent = await this.page.textContent('html')
    if (pageContent!.includes('Continue without an account')) {
      await this.page.click('text="Continue without an account"')
    }

    // Ensure that we redirected to the programs list page.
    await this.expectProgramsPage()
  }

  // Expect the program index (home) page
  async expectProgramsPage() {
    await waitForPageJsLoad(this.page)
    expect(this.page.url().split('/').pop()).toEqual('programs')
  }

  async expectReviewPage() {
    await expect(
      this.page.locator('[data-testid="programSummary"]'),
    ).toBeVisible()
  }

  async expectConfirmationPage() {
    await expect(this.page.getByText('Your application details')).toBeVisible()
  }

  async expectPreScreenerReviewPage() {
    expect(await this.page.innerText('h2')).toContain(
      'Benefits pre-screener summary',
    )
  }

  async expectPreScreenerConfirmationPage(
    wantUpsell: boolean,
    wantTrustedIntermediary: boolean,
    wantEligiblePrograms: string[],
  ) {
    if (wantTrustedIntermediary) {
      await expect(
        this.page.getByRole('heading', {
          name: 'Programs your client may qualify for',
        }),
      ).toBeVisible()
    } else {
      await expect(
        this.page.getByRole('heading', {name: 'Programs you may qualify for'}),
      ).toBeVisible()
    }

    const createAccountHeading = this.page.getByRole('heading', {
      name: 'To access your application later, create an account',
    })
    if (wantUpsell) {
      await expect(createAccountHeading).toBeVisible()
    } else {
      await expect(createAccountHeading).toBeHidden()
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
    await expect(this.page).toHaveTitle('Ineligible for program')

    await expect(
      this.page
        .getByText('You may not be eligible for this program')
        .and(this.page.getByRole('heading')),
    ).toBeVisible()

    await expect(this.page.getByText('Apply to another program')).toBeVisible()
    await expect(this.page.getByText('Edit my responses')).toBeVisible()
  }

  async clickGoBackAndEditOnIneligiblePage() {
    await this.page
      .getByRole('button', {
        name: 'Edit my responses',
      })
      .click()
    await waitForPageJsLoad(this.page)
  }

  async expectDuplicatesPage() {
    expect(await this.page.innerText('h2')).toContain(
      'There are no changes to save',
    )
  }

  async expectIneligibleQuestion(questionText: string) {
    await expect(
      this.page
        .getByRole('alert')
        .getByRole('listitem')
        .getByText(questionText),
    ).toHaveCount(1)
  }

  async expectIneligibleQuestionsCount(number: number) {
    await expect(
      this.page.getByRole('alert').getByRole('listitem'),
    ).toHaveCount(number)
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

  async expectVerifyAddressPage(hasAddressSuggestions: boolean) {
    await expect(
      this.page.getByRole('heading', {name: 'Confirm your address'}),
    ).toBeVisible()

    //    expect(await this.page.innerText('h2')).toContain('Confirm your address')
    // Note: If there's only one suggestion, the heading will be "Suggested address"
    // not "Suggested addresses". But, our browser setup always returns multiple
    // suggestions so we can safely assert the heading is always "Suggested addresses".
    await expect(
      this.page.getByRole('heading', {name: 'Suggested addresses'}),
    ).toBeVisible({visible: hasAddressSuggestions})
  }

  async expectAddressPage() {
    expect(await this.page.innerText('legend')).toContain('With Correction')
  }

  async selectAddressSuggestion(addressName: string) {
    await this.page.click(`label:has-text("${addressName}")`)
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

  async expectQuestionExistsOnReviewPage(questionText: string) {
    await expect(
      this.page.getByRole('listitem').getByText(questionText),
    ).toBeVisible()
  }

  async expectQuestionDoesNotExistOnReviewPage(questionText: string) {
    await expect(
      this.page.getByRole('listitem').getByText(questionText),
    ).toBeHidden()
  }

  async submitFromReviewPage() {
    // Assert that we're on the review page.
    await this.expectReviewPage()

    // Click on submit button.
    await this.clickSubmitApplication()
  }

  async downloadFromConfirmationPage(): Promise<void> {
    // Assert that we're on the confirmation page.
    await this.expectConfirmationPage()
    // Click on the download button
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

  async validateNoPreviouslyAnsweredText(questionText: string) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    await expect(
      questionLocator.locator('.cf-applicant-question-previously-answered'),
    ).toBeHidden()
  }

  async validatePreviouslyAnsweredText(questionText: string) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    await expect(questionLocator.locator('.summary-answer')).toBeVisible()
  }

  async validateNoPreviouslyAnsweredText(questionText: string) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    await expect(questionLocator.locator('.summary-answer')).toHaveText('-')
  }

  async validateNoPreviouslyAnsweredTextFileUpload(questionText: string) {
    const questionLocator = this.page.locator('.cf-applicant-summary-row', {
      has: this.page.locator(`:text("${questionText}")`),
    })
    await expect(questionLocator.locator('.summary-answer')).toHaveText(
      '-- NO FILE SELECTED --',
    )
  }

  async seeStaticQuestion(questionText: string) {
    expect(await this.page.textContent('html')).toContain(questionText)
  }

  async expectRequiredQuestionError(questionLocator: string) {
    expect(await this.page.innerText(questionLocator)).toContain(
      'This question is required',
    )
  }
  async expectErrorOnReviewModal() {
    const modalTitle = 'Some answers on this page need to be fixed'
    const modalContent =
      'This page of your application either has some errors or some fields were left blank. If you continue to the review page, none of the information entered on this page will be saved until the errors are corrected.'
    const modalContinueButton = 'Go to the review page without saving'
    const modalFixButton = 'Stay here and fix your answers'

    const modal = this.page.getByRole('dialog', {state: 'visible'})
    await expect(modal.getByText(modalTitle)).toBeVisible()
    await expect(modal.getByText(modalContent)).toBeVisible()
    await expect(
      modal.getByRole('button').getByText(modalContinueButton),
    ).toBeVisible()
    await expect(
      modal.getByRole('button').getByText(modalFixButton),
    ).toBeVisible()
  }

  async clickReviewWithoutSaving() {
    const buttonText = 'Go to the review page without saving'
    await this.page.click(`button:has-text("${buttonText}")`)
  }

  async expectErrorOnPreviousModal() {
    const modalTitle = 'Some answers on this page need to be fixed'
    const modalContent =
      'This page of your application either has some errors or some fields were left blank. If you continue to the previous page, none of the information entered on this page will be saved until the errors are corrected.'
    const modalContinueButton = 'Go to the previous page without saving'
    const modalFixButton = 'Stay here and fix your answers'

    const modal = this.page.getByRole('dialog', {state: 'visible'})
    await expect(modal.getByText(modalTitle)).toBeVisible()
    await expect(modal.getByText(modalContent)).toBeVisible()
    await expect(
      modal.getByRole('button').getByText(modalContinueButton),
    ).toBeVisible()
    await expect(
      modal.getByRole('button').getByText(modalFixButton),
    ).toBeVisible()
  }

  async clickPreviousWithoutSaving() {
    const buttonText = 'Go to the previous page without saving'
    await this.page.click(`button:has-text("${buttonText}")`)
  }

  async clickStayAndFixAnswers() {
    const buttonText = 'Stay here and fix your answers'
    await this.page.click(`button:has-text("${buttonText}")`)
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
    await this.clickContinue()
    await this.submitFromReviewPage()
    await this.page.getByRole('button', {name: 'End your session'}).click()
  }

  async expectMayBeEligibileAlertToBeVisible() {
    await expect(
      this.page.getByRole('heading', {name: 'may be eligible'}),
    ).toBeVisible()
    await expect(
      this.page.getByRole('heading', {name: 'may not be eligible'}),
    ).not.toBeAttached()
  }

  async expectMayBeEligibileAlertToBeHidden() {
    await expect(
      this.page.getByRole('heading', {name: 'may be eligible'}),
    ).not.toBeAttached()
  }

  async expectIneligibleQuestionInReviewPageAlert(questionText: string) {
    await expect(
      this.page
        .getByRole('heading', {name: 'may not be eligible'})
        .locator('..')
        .getByRole('listitem')
        .filter({hasText: questionText}),
    ).toBeAttached()
  }

  async expectMayNotBeEligibileAlertToBeVisible() {
    await expect(
      this.page.getByRole('heading', {name: 'may not be eligible'}),
    ).toBeVisible()
    await expect(
      this.page.getByRole('heading', {name: 'may be eligible'}),
    ).not.toBeAttached()
  }

  async expectMayNotBeEligibleAlertToBeHidden() {
    await expect(
      this.page.getByRole('heading', {name: 'may not be eligible'}),
    ).not.toBeAttached()
  }

  async expectTitle(page: Page, title: string) {
    await expect(page).toHaveTitle(title)
  }

  async filterProgramsByCategory(category: string) {
    await this.page
      .locator('#ns-category-filter-form')
      .getByText(category)
      .check()
    await this.page
      .getByRole('button', {name: 'Apply selections', exact: true})
      .click()
    await waitForHtmxReady(this.page)
  }

  // On the application summary page, find the block with the given name
  // and click "Edit"
  async editBlock(blockName: string) {
    await this.page
      .locator(
        '.block-summary:has-text("' +
          blockName +
          '") >> .summary-edit-button:has-text("Edit")',
      )
      .click()
  }

  async continueToApplicationFromLoginPromptModal() {
    await this.page.getByRole('link', {name: 'Continue to application'}).click()
  }

  async expectLoginModal() {
    const modal = await waitForAnyModalLocator(this.page)
    await expect(modal).toContainText('Sign in')
  }
}
