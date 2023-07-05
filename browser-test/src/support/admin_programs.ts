import {ElementHandle, Frame, Page} from 'playwright'
import {readFileSync} from 'fs'
import {
  clickAndWaitForModal,
  dismissModal,
  waitForAnyModal,
  waitForPageJsLoad,
} from './wait'
import {BASE_URL, TEST_CIVIC_ENTITY_SHORT_NAME} from './config'
import {AdminProgramStatuses} from './admin_program_statuses'
import {validateScreenshot} from '.'

/**
 * JSON object representing downloaded application. It can be retrieved by
 * program admins. To see all fields check buildJsonApplication() method in
 * JsonExporter.java.
 */
export interface DownloadedApplication {
  program_name: string
  program_version_id: number
  applicant_id: number
  application_id: number
  language: string
  create_time: string
  submitter_email: string
  submit_time: string
  // Applicant answers as a map of question name to answer data.
  application: {
    [questionName: string]: {
      [questionField: string]: unknown
    }
  }
}

export enum ProgramVisibility {
  HIDDEN = 'Hide from applicants.',
  PUBLIC = 'Publicly visible',
  TI_ONLY = 'Trusted Intermediaries ONLY',
  SELECT_TI = 'Visible to Selected Trusted Intermediaries ONLY',
}

function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/ /g, '-')
    .replace(/[^a-zA-Z0-9-]/g, '')
}

export class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminProgramsPage() {
    await this.page.click('nav :text("Programs")')
    await this.expectAdminProgramsPage()
    await waitForPageJsLoad(this.page)
  }

  async expectAdminProgramsPage() {
    expect(await this.page.innerText('h1')).toEqual('Program dashboard')
    expect(await this.page.innerText('h2')).toEqual(
      'Create, edit and publish programs in ' + TEST_CIVIC_ENTITY_SHORT_NAME,
    )
  }

  async expectProgramExist(programName: string, description: string) {
    await this.gotoAdminProgramsPage()
    const tableInnerText = await this.page.innerText('main')

    expect(tableInnerText).toContain(programName)
    expect(tableInnerText).toContain(description)
  }

  async expectApplicationHasStatusString(
    applicant: string,
    statusString: string,
  ) {
    expect(
      await this.page.innerText(
        this.selectApplicationCardForApplicant(applicant),
      ),
    ).toContain(`Status: ${statusString}`)
  }

  async expectApplicationStatusDoesntContain(
    applicant: string,
    statusString: string,
  ) {
    expect(
      await this.page.innerText(
        this.selectApplicationCardForApplicant(applicant),
      ),
    ).not.toContain(statusString)
  }

  /**
   * Creates program with given name. At the end of this method the current
   * page is going to be block edit page.
   */
  async addProgram(
    programName: string,
    description = 'program description',
    externalLink = 'https://usa.gov',
    visibility = ProgramVisibility.PUBLIC,
    adminDescription = 'admin description',
    isCommonIntake = false,
    selectedTI = 'none',
  ) {
    await this.gotoAdminProgramsPage()
    await this.page.click('#new-program-button')
    await waitForPageJsLoad(this.page)

    // program name must be in url-compatible form so we slugify it
    await this.page.fill('#program-name-input', slugify(programName))
    await this.page.fill('#program-description-textarea', adminDescription)
    await this.page.fill('#program-display-name-input', programName)
    await this.page.fill('#program-display-description-textarea', description)
    await this.page.fill('#program-external-link-input', externalLink)

    await this.page.check(`label:has-text("${visibility}")`)
    if (visibility == ProgramVisibility.SELECT_TI) {
      const screenshotname = programName.replaceAll(' ', '-').toLowerCase()
      await validateScreenshot(this.page, screenshotname)
      await this.page.check(`label:has-text("${selectedTI}")`)
    }

    if (isCommonIntake && this.getCommonIntakeFormToggle != null) {
      await this.clickCommonIntakeFormToggle()
    }

    await this.page.click('#program-update-button')
    await waitForPageJsLoad(this.page)
    await this.expectProgramBlockEditPage(programName)
  }
  async editProgram(
    programName: string,
    visibility = ProgramVisibility.PUBLIC,
    selectedTI = 'none',
  ) {
    await this.gotoAdminProgramsPage()
    await this.page.click('button :text("View")')
    await this.page.click('#header_edit_button')
    await this.page.click('#header_edit_button')
    await waitForPageJsLoad(this.page)

    await this.page.check(`label:has-text("${visibility}")`)
    if (visibility == ProgramVisibility.SELECT_TI) {
      await this.page.check(`label:has-text("${selectedTI}")`)
    }

    await this.page.click('#program-update-button')
    await waitForPageJsLoad(this.page)
  }

  async programNames() {
    await this.gotoAdminProgramsPage()
    const titles = this.page.locator('.cf-admin-program-card .cf-program-title')
    return titles.allTextContents()
  }

  /**
   * Expects a specific program block to be selected inside the read only view
   * that is used to view the configuration of an active program.
   */
  async expectReadOnlyProgramBlock(blockId: string) {
    // The block info shows us we are viewing a block.
    expect(this.page.locator('id=block-info-display-' + blockId)).not.toBeNull()
    // The absence of one of the edit buttons ensures it is the read only view.
    expect(
      await this.page.locator('id=block-description-modal-button').count(),
    ).toEqual(0)
  }

  /**
   * Expects a question card with a specified text label in it.
   */
  async expectQuestionCardWithLabel(questionName: string, label: string) {
    expect(
      await this.page
        .locator(
          this.withinQuestionCardSelectorInProgramView(
            questionName,
            `p:has-text("${label}")`,
          ),
        )
        .count(),
    ).toBe(1)
  }

  // Question card within a program edit or read only page
  questionCardSelectorInProgramView(questionName: string) {
    return `.cf-program-question:has(:text("Admin ID: ${questionName}"))`
  }

  // Question card within a program edit page
  withinQuestionCardSelectorInProgramView(
    questionName: string,
    selector: string,
  ) {
    return this.questionCardSelectorInProgramView(questionName) + ' ' + selector
  }

  programCardSelector(programName: string, lifecycle: string) {
    return `.cf-admin-program-card:has(:text("${programName}")):has(:text("${lifecycle}"))`
  }

  withinProgramCardSelector(
    programName: string,
    lifecycle: string,
    selector: string,
  ) {
    return this.programCardSelector(programName, lifecycle) + ' ' + selector
  }

  async gotoDraftProgramManageStatusesPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.page.click(
      this.withinProgramCardSelector(programName, 'Draft', '.cf-with-dropdown'),
    )
    await this.page.click(
      this.withinProgramCardSelector(
        programName,
        'Draft',
        ':text("Manage application statuses")',
      ),
    )
    await waitForPageJsLoad(this.page)
    const adminProgramStatuses = new AdminProgramStatuses(this.page)
    await adminProgramStatuses.expectProgramManageStatusesPage(programName)
  }

  async gotoDraftProgramManageTranslationsPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.page.click(
      this.withinProgramCardSelector(programName, 'Draft', '.cf-with-dropdown'),
    )
    await this.page.click(
      this.withinProgramCardSelector(
        programName,
        'Draft',
        ':text("Manage Translations")',
      ),
    )
    await waitForPageJsLoad(this.page)
    await this.expectProgramManageTranslationsPage(programName)
  }

  async gotoManageProgramAdminsPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.page.click(
      this.withinProgramCardSelector(programName, 'Draft', '.cf-with-dropdown'),
    )
    await this.page.click(
      this.withinProgramCardSelector(
        programName,
        'Draft',
        ':text("Manage Program Admins")',
      ),
    )
    await waitForPageJsLoad(this.page)
    await this.expectManageProgramAdminsPage()
  }

  async gotoProgramSettingsPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)

    await this.page.click(
      this.withinProgramCardSelector(programName, 'Draft', '.cf-with-dropdown'),
    )
    await this.page.click(
      this.withinProgramCardSelector(programName, 'Draft', ':text("Settings")'),
    )

    await waitForPageJsLoad(this.page)
    await this.expectProgramSettingsPage()
  }

  async setProgramEligibilityToNongating(programName: string) {
    await this.gotoProgramSettingsPage(programName)
    const nonGatingEligibilityValue = await this.page
      .locator('input[name=eligibilityIsGating]')
      .inputValue()
    if (nonGatingEligibilityValue == 'false') {
      await this.page.locator('#eligibility-toggle').click()
    }
  }

  async gotoEditDraftProgramPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.page.click(
      this.withinProgramCardSelector(
        programName,
        'Draft',
        'button :text("Edit")',
      ),
    )
    await waitForPageJsLoad(this.page)
    await this.expectProgramBlockEditPage(programName)
  }

  async gotoViewActiveProgramPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectActiveProgram(programName)
    await this.page.click(
      this.withinProgramCardSelector(programName, 'Active', ':text("View")'),
    )
    await waitForPageJsLoad(this.page)
    await this.expectProgramBlockReadOnlyPage(programName)
  }

  async gotoViewActiveProgramPageAndStartEditing(programName: string) {
    await this.gotoViewActiveProgramPage(programName)
    await this.page.click('button:has-text("Edit")')
    await waitForPageJsLoad(this.page)
  }

  async goToBlockInProgram(programName: string, blockName: string) {
    await this.gotoEditDraftProgramPage(programName)
    // Click on the block to edit
    await this.page.click(`a:has-text("${blockName}")`)
    await waitForPageJsLoad(this.page)
  }

  async gotoToBlockInReadOnlyProgram(blockId: string) {
    await this.page.click('#block_list_item_' + blockId)
    await waitForPageJsLoad(this.page)
  }

  async goToEditBlockVisibilityPredicatePage(
    programName: string,
    blockName: string,
  ) {
    await this.goToBlockInProgram(programName, blockName)

    // Click on the edit predicate button
    await this.page.click('#cf-edit-visibility-predicate')
    await waitForPageJsLoad(this.page)
    await this.expectEditVisibilityPredicatePage(blockName)
  }

  async goToEditBlockEligibilityPredicatePage(
    programName: string,
    blockName: string,
  ) {
    await this.goToBlockInProgram(programName, blockName)

    // Click on the edit predicate button
    await this.page.click('#cf-edit-eligibility-predicate')
    await waitForPageJsLoad(this.page)
    await this.expectEditEligibilityPredicatePage(blockName)
  }

  async goToProgramDescriptionPage(programName: string) {
    await this.gotoEditDraftProgramPage(programName)
    await this.page.click('button:has-text("Edit program details")')
    await waitForPageJsLoad(this.page)
  }

  async expectDraftProgram(programName: string) {
    expect(
      await this.page.isVisible(this.programCardSelector(programName, 'Draft')),
    ).toBe(true)
  }

  async expectDoesNotHaveDraftProgram(programName: string) {
    expect(
      await this.page.isVisible(this.programCardSelector(programName, 'Draft')),
    ).toBe(false)
  }

  async expectActiveProgram(programName: string) {
    expect(
      await this.page.isVisible(
        this.programCardSelector(programName, 'Active'),
      ),
    ).toBe(true)
  }

  async expectProgramEditPage(programName = '') {
    expect(await this.page.innerText('h1')).toContain(
      `Edit program: ${programName}`,
    )
  }

  async expectProgramManageTranslationsPage(programName: string) {
    expect(await this.page.innerText('h1')).toContain(
      `Manage program translations: ${programName}`,
    )
  }

  async expectManageProgramAdminsPage() {
    expect(await this.page.innerText('h1')).toContain(
      'Manage Admins for Program',
    )
  }

  async expectProgramSettingsPage() {
    expect(await this.page.innerText('h1')).toContain('settings')
  }

  async expectAddProgramAdminErrorToast() {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain(
      'does not have an admin account and cannot be added as a Program Admin.',
    )
    expect(toastMessages).toContain('Error: ')
  }

  async expectEditVisibilityPredicatePage(blockName: string) {
    expect(await this.page.innerText('h1')).toContain(
      'Visibility condition for ' + blockName,
    )
  }

  async expectEditEligibilityPredicatePage(blockName: string) {
    expect(await this.page.innerText('h1')).toContain(
      'Eligibility condition for ' + blockName,
    )
  }

  async expectSuccessToast(successToastMessage: string) {
    const toastContainer = await this.page.innerHTML('#toast-container')

    expect(toastContainer).toContain('bg-emerald-200')
    expect(toastContainer).toContain(successToastMessage)
  }

  async expectProgramBlockEditPage(programName = '') {
    expect(await this.page.innerText('id=program-title')).toContain(programName)
    expect(await this.page.innerText('id=block-edit-form')).not.toBeNull()
    // Compare string case insensitively because style may not have been computed.
    expect(
      (await this.page.innerText('[for=block-name-input]')).toUpperCase(),
    ).toEqual('SCREEN NAME')
    expect(
      (
        await this.page.innerText('[for=block-description-textarea]')
      ).toUpperCase(),
    ).toEqual('SCREEN DESCRIPTION')
    expect(await this.page.innerText('h1')).toContain('Add a question')
  }

  async expectProgramBlockReadOnlyPage(programName = '') {
    expect(await this.page.innerText('id=program-title')).toContain(programName)
    // The only element for editing should be one top level button
    expect(await this.page.innerText('#header_edit_button'))
    expect(await this.page.locator('id=block-edit-form').count()).toEqual(0)
  }

  // Removes questions from given block in program.
  async removeQuestionFromProgram(
    programName: string,
    blockName: string,
    questionNames: string[] = [],
  ) {
    await this.goToBlockInProgram(programName, blockName)

    for (const questionName of questionNames) {
      await this.page.click(
        this.withinQuestionCardSelectorInProgramView(
          questionName,
          'button:has-text("Delete")',
        ),
      )
    }
  }

  async editProgramBlock(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[] = [],
  ) {
    await this.gotoEditDraftProgramPage(programName)

    await clickAndWaitForModal(this.page, 'block-description-modal')
    await this.page.fill('textarea', blockDescription)
    // Make sure input validation enables the button before clicking.
    await this.page.click('#update-block-button:not([disabled])')

    for (const questionName of questionNames) {
      await this.addQuestionFromQuestionBank(questionName)
    }
  }

  async launchDeleteScreenModal() {
    const programName = 'Test program 7'
    await this.addProgram(programName)
    await this.addProgramBlock(programName)
    await this.goToBlockInProgram(programName, 'Screen 1')
    await clickAndWaitForModal(this.page, 'block-delete-modal')
  }

  async removeProgramBlock(programName: string, blockName: string) {
    await this.goToBlockInProgram(programName, blockName)
    await clickAndWaitForModal(this.page, 'block-delete-modal')
    await this.page.click('#delete-block-button')
    await waitForPageJsLoad(this.page)
    await this.gotoAdminProgramsPage()
  }

  private async waitForQuestionBankAnimationToFinish() {
    // Animation is 150ms. Give some extra overhead to avoid flakiness on slow CPU.
    // This is currently called over 300 times which adds up.
    // https://tailwindcss.com/docs/transition-property
    await this.page.waitForTimeout(250)
  }

  async openQuestionBank() {
    await this.page.click('button:has-text("Add a question")')
    await this.waitForQuestionBankAnimationToFinish()
  }

  async closeQuestionBank() {
    await this.page.click('svg.cf-close-question-bank-button')
    await this.waitForQuestionBankAnimationToFinish()
  }

  async addQuestionFromQuestionBank(questionName: string) {
    await this.openQuestionBank()
    await this.page.click(
      `.cf-question-bank-element:has-text("Admin ID: ${questionName}") button:has-text("Add")`,
    )
    await waitForPageJsLoad(this.page)
    // After question was added question bank is still open. Close it first.
    await this.closeQuestionBank()
    // Make sure the question is successfully added to the screen.
    await this.page.waitForSelector(
      `div.cf-program-question p:text("Admin ID: ${questionName}")`,
    )
  }

  async questionBankNames(): Promise<string[]> {
    const titles = this.page.locator(
      '.cf-question-bank-element:visible .cf-question-title',
    )
    return titles.allTextContents()
  }

  async editProgramBlockWithOptional(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[],
    optionalQuestionName: string,
  ) {
    await this.gotoEditDraftProgramPage(programName)

    await clickAndWaitForModal(this.page, 'block-description-modal')
    await this.page.fill('textarea', blockDescription)
    await this.page.click('#update-block-button:not([disabled])')

    // Add the optional question
    await this.addQuestionFromQuestionBank(optionalQuestionName)
    // Only allow one optional question per block; this selector will always toggle the first optional button.  It
    // cannot tell the difference between multiple option buttons
    await this.page.click(`:is(button:has-text("optional"))`)

    for (const questionName of questionNames) {
      await this.addQuestionFromQuestionBank(questionName)
    }
  }

  async addProgramBlock(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[] = [],
  ) {
    await this.gotoEditDraftProgramPage(programName)

    await this.page.click('#add-block-button')
    await waitForPageJsLoad(this.page)

    await clickAndWaitForModal(this.page, 'block-description-modal')
    await this.page.type('textarea', blockDescription)
    await this.page.click('#update-block-button:not([disabled])')
    // Wait for submit and redirect back to this page.
    await this.page.waitForURL(this.page.url())
    await waitForPageJsLoad(this.page)

    for (const questionName of questionNames) {
      await this.addQuestionFromQuestionBank(questionName)
    }
    return await this.page.$eval(
      '#block-name-input',
      (el) => (el as HTMLInputElement).value,
    )
  }

  async addProgramRepeatedBlock(
    programName: string,
    enumeratorBlockName: string,
    blockDescription = 'screen description',
    questionNames: string[] = [],
  ) {
    await this.gotoEditDraftProgramPage(programName)

    await this.page.click(`text=${enumeratorBlockName}`)
    await waitForPageJsLoad(this.page)
    await this.page.click('#create-repeated-block-button')
    await waitForPageJsLoad(this.page)

    await clickAndWaitForModal(this.page, 'block-description-modal')
    await this.page.fill('#block-description-textarea', blockDescription)
    await this.page.click('#update-block-button:not([disabled])')

    for (const questionName of questionNames) {
      await this.addQuestionFromQuestionBank(questionName)
    }
  }

  async publishProgram(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.publishAllPrograms()
    await this.expectActiveProgram(programName)
  }

  private static PUBLISH_ALL_MODAL_TITLE =
    'All draft programs will be published'

  publishAllProgramsModalLocator() {
    return this.page.locator(
      `.cf-modal:has-text("${AdminPrograms.PUBLISH_ALL_MODAL_TITLE}")`,
    )
  }

  async publishAllPrograms() {
    await this.gotoAdminProgramsPage()
    const modal = await this.openPublishAllProgramsModal()
    const confirmHandle = (await modal.$('button:has-text("Confirm")'))!
    await confirmHandle.click()

    await waitForPageJsLoad(this.page)
  }

  async openPublishAllProgramsModal() {
    await this.page.click('button:has-text("Publish all drafts")')
    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain(
      AdminPrograms.PUBLISH_ALL_MODAL_TITLE,
    )
    return modal
  }

  async expectProgramReferencesModalContains({
    expectedQuestionsContents,
    expectedProgramsContents,
  }: {
    expectedQuestionsContents: string[]
    expectedProgramsContents: string[]
  }) {
    const modal = await this.openPublishAllProgramsModal()

    const editedQuestions = await modal.$$(
      '.cf-admin-publish-references-question li',
    )
    const editedQuestionsContents = await Promise.all(
      editedQuestions.map((editedQuestion) => editedQuestion.innerText()),
    )
    expect(editedQuestionsContents).toEqual(expectedQuestionsContents)

    const editedPrograms = await modal.$$(
      '.cf-admin-publish-references-program li',
    )
    const editedProgramsContents = await Promise.all(
      editedPrograms.map((editedProgram) => editedProgram.innerText()),
    )
    expect(editedProgramsContents).toEqual(expectedProgramsContents)

    await dismissModal(this.page)
  }

  async createNewVersion(
    programName: string,
    programReadOnlyViewEnabled = true,
  ) {
    await this.gotoAdminProgramsPage()
    await this.expectActiveProgram(programName)

    if (programReadOnlyViewEnabled) {
      await this.page.click(
        this.withinProgramCardSelector(
          programName,
          'Active',
          '.cf-with-dropdown',
        ),
      )
    }
    await this.page.click(
      this.withinProgramCardSelector(programName, 'Active', ':text("Edit")'),
    )
    await waitForPageJsLoad(this.page)

    await this.page.click('button:has-text("Edit program details")')
    await waitForPageJsLoad(this.page)

    await this.page.click('#program-update-button')
    await waitForPageJsLoad(this.page)
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
  }

  async viewApplications(programName: string) {
    // Navigate back to the main page for the program admin.
    await this.page.goto(BASE_URL)
    await waitForPageJsLoad(this.page)

    await this.page.click(
      this.withinProgramCardSelector(
        programName,
        'ACTIVE',
        'button :text("Applications")',
      ),
    )
    await waitForPageJsLoad(this.page)
  }

  async expectApplicationCount(expectedCount: number) {
    const cardElements = await this.page.$$('.cf-admin-application-card')
    expect(cardElements.length).toBe(expectedCount)
  }

  selectApplicationCardForApplicant(applicantName: string) {
    return `.cf-admin-application-card:has-text("${applicantName}")`
  }

  selectWithinApplicationForApplicant(applicantName: string, selector: string) {
    return (
      this.selectApplicationCardForApplicant(applicantName) + ' ' + selector
    )
  }

  selectQuestionWithinBlock(question: string) {
    return `.cf-program-question:has-text("${question}")`
  }

  selectWithinQuestionWithinBlock(question: string, selector: string) {
    return this.selectQuestionWithinBlock(question) + ' ' + selector
  }

  public static readonly ANY_STATUS_APPLICATION_FILTER_OPTION =
    'Any application status'
  public static readonly NO_STATUS_APPLICATION_FILTER_OPTION =
    'Only applications without a status'

  async filterProgramApplications({
    searchFragment = '',
    applicationStatusOption = '',
  }: {
    searchFragment?: string
    applicationStatusOption?: string
  }) {
    await this.page.fill('input[name="search"]', searchFragment)
    if (applicationStatusOption) {
      await this.page.selectOption('label:has-text("Application status")', {
        label: applicationStatusOption,
      })
    }
    await Promise.all([
      this.page.waitForNavigation(),
      await this.page.click('button:has-text("Filter")'),
    ])
    await waitForPageJsLoad(this.page)
  }

  selectApplicationBlock(blockName: string) {
    return `.cf-admin-application-block-card:has-text("${blockName}")`
  }

  selectWithinApplicationBlock(blockName: string, selector: string) {
    return this.selectApplicationBlock(blockName) + ' ' + selector
  }

  async viewApplicationForApplicant(applicantName: string) {
    await Promise.all([
      this.waitForApplicationFrame(),
      this.page.click(
        this.selectWithinApplicationForApplicant(
          applicantName,
          'a:text("View")',
        ),
      ),
    ])
  }

  private static APPLICATION_DISPLAY_FRAME_NAME = 'application-display-frame'

  applicationFrame(): Frame {
    return this.page.frame(AdminPrograms.APPLICATION_DISPLAY_FRAME_NAME)!
  }

  applicationFrameLocator() {
    return this.page.frameLocator(
      `iframe[name="${AdminPrograms.APPLICATION_DISPLAY_FRAME_NAME}"]`,
    )
  }

  async waitForApplicationFrame() {
    const frame = this.page.frame(AdminPrograms.APPLICATION_DISPLAY_FRAME_NAME)
    if (!frame) {
      throw new Error('Expected an application frame')
    }
    await frame.waitForNavigation()
    await waitForPageJsLoad(frame)
  }

  async expectApplicationAnswers(
    blockName: string,
    questionName: string,
    answer: string,
  ) {
    const blockText = await this.applicationFrameLocator()
      .locator(this.selectApplicationBlock(blockName))
      .innerText()

    expect(blockText).toContain(questionName)
    expect(blockText).toContain(answer)
  }

  async expectApplicationAnswerLinks(blockName: string, questionName: string) {
    expect(
      await this.applicationFrameLocator()
        .locator(this.selectApplicationBlock(blockName))
        .innerText(),
    ).toContain(questionName)
    expect(
      await this.applicationFrameLocator()
        .locator(this.selectWithinApplicationBlock(blockName, 'a'))
        .getAttribute('href'),
    ).not.toBeNull()
  }

  async isStatusSelectorVisible(): Promise<boolean> {
    return this.applicationFrameLocator()
      .locator(this.statusSelector())
      .isVisible()
  }

  async getStatusOption(): Promise<string> {
    return this.applicationFrameLocator()
      .locator(this.statusSelector())
      .inputValue()
  }

  /**
   * Selects the provided status option and then awaits the confirmation dialog.
   */
  async setStatusOptionAndAwaitModal(
    status: string,
  ): Promise<ElementHandle<HTMLElement>> {
    await this.applicationFrameLocator()
      .locator(this.statusSelector())
      .selectOption(status)

    const frame = this.page.frame(AdminPrograms.APPLICATION_DISPLAY_FRAME_NAME)
    if (!frame) {
      throw new Error('Expected an application frame')
    }

    return waitForAnyModal(frame)
  }

  /**
   * Clicks the confirm button in the status update confirmation dialog and waits until the IFrame
   * containing the modal has been refreshed.
   */
  async confirmStatusUpdateModal(modal: ElementHandle<HTMLElement>) {
    // Confirming should cause the frame to redirect and waitForNavigation must be called prior
    // to taking the action that would trigger navigation.
    const confirmButton = (await modal.$('text=Confirm'))!
    await Promise.all([this.page.waitForNavigation(), confirmButton.click()])
    await waitForPageJsLoad(this.page)
  }

  async expectUpdateStatusToast() {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain('Application status updated')
  }

  private statusSelector() {
    return '.cf-program-admin-status-selector label:has-text("Status:")'
  }

  async isEditNoteVisible(): Promise<boolean> {
    return this.applicationFrameLocator()
      .locator(this.editNoteSelector())
      .isVisible()
  }

  /**
   * Returns the content of the note modal when viewing an application.
   */
  async getNoteContent() {
    await this.applicationFrameLocator()
      .locator(this.editNoteSelector())
      .click()

    const frame = this.page.frame(AdminPrograms.APPLICATION_DISPLAY_FRAME_NAME)
    if (!frame) {
      throw new Error('Expected an application frame')
    }
    const editModal = await waitForAnyModal(frame)
    const noteContentArea = (await editModal.$('textarea'))!
    return noteContentArea.inputValue()
  }

  /**
   * Clicks the edit note button, and returns the modal.
   */
  async awaitEditNoteModal(): Promise<ElementHandle<HTMLElement>> {
    await this.applicationFrameLocator()
      .locator(this.editNoteSelector())
      .click()

    const frame = this.page.frame(AdminPrograms.APPLICATION_DISPLAY_FRAME_NAME)
    if (!frame) {
      throw new Error('Expected an application frame')
    }
    return await waitForAnyModal(frame)
  }

  /**
   * Clicks the edit note button, sets the note content to the provided text,
   * and confirms the dialog.
   */
  async editNote(noteContent: string) {
    const editModal = await this.awaitEditNoteModal()
    const noteContentArea = (await editModal.$('textarea'))!
    await noteContentArea.fill(noteContent)

    // Confirming should cause the page to redirect and waitForNavigation must be called prior
    // to taking the action that would trigger navigation.
    const saveButton = (await editModal.$('text=Save'))!
    await Promise.all([this.page.waitForNavigation(), saveButton.click()])
    await waitForPageJsLoad(this.page)
  }

  private editNoteSelector() {
    return 'button:has-text("Edit note")'
  }

  async expectNoteUpdatedToast() {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain('Application note updated')
  }

  async getJson(applyFilters: boolean): Promise<DownloadedApplication[]> {
    await clickAndWaitForModal(this.page, 'download-program-applications-modal')
    if (applyFilters) {
      await this.page.check('text="Current results"')
    } else {
      await this.page.check('text="All data"')
    }
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download JSON"'),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }

    return JSON.parse(readFileSync(path, 'utf8')) as DownloadedApplication[]
  }
  async getPdf() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.applicationFrameLocator()
        .locator('button:has-text("Export to PDF")')
        .click(),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }
  async getCsv(applyFilters: boolean) {
    await clickAndWaitForModal(this.page, 'download-program-applications-modal')
    if (applyFilters) {
      await this.page.check('text="Current results"')
    } else {
      await this.page.check('text="All data"')
    }
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download CSV"'),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  async getDemographicsCsv() {
    await clickAndWaitForModal(this.page, 'download-demographics-csv-modal')
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(
        '#download-demographics-csv-modal button:has-text("Download Exported Data (CSV)")',
      ),
    ])
    await dismissModal(this.page)
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }
  /*
   * Creates a program, ads the specified questions to it and publishes it.
   * To use this method, questions must have been previously created for example by using one of the helper methods in admin_questions.ts.
   * <BR>Example:
   * <BR> adminQuestions.addAddressQuestion({questionName: 'address-q'})
   */
  async addAndPublishProgramWithQuestions(
    questionNames: string[],
    programName: string,
  ) {
    await this.addProgram(programName)
    await this.editProgramBlock(programName, 'dummy description', questionNames)
    await this.publishProgram(programName)
  }

  getAddressCorrectionToggle() {
    return this.page.locator('input[name=addressCorrectionEnabled]')
  }

  getAddressCorrectionToggleByName(questionName: string) {
    return this.page
      .locator('.cf-program-question')
      .filter({hasText: questionName})
      .locator('input[name=addressCorrectionEnabled]')
  }

  getAddressCorrectionHelpTextByName(questionName: string) {
    return this.page
      .locator('.cf-program-question')
      .filter({hasText: questionName})
      .locator(
        ':is(span:has-text("Enabling \'address correction\' will check"))',
      )
  }
  async clickAddressCorrectionToggle() {
    const responsePromise = this.page.waitForResponse((response) => {
      // The setAddressCorrectionEnabled action either redirects to the edit page or returns an error, in which case the response url remains the same.
      return response.url().includes('edit')
    })
    await this.page.click(':is(button:has-text("Address correction"))')
    await responsePromise
    await this.page.waitForLoadState()
  }

  async clickAddressCorrectionToggleByName(questionName: string) {
    const toggleLocator = this.getAddressCorrectionToggleByName(questionName)
    const responsePromise = this.page.waitForResponse((response) => {
      // The setAddressCorrectionEnabled action either redirects to the edit page or returns an error, in which case the response url remains the same.
      return response.url().includes('edit')
    })

    await toggleLocator
      .locator('..')
      .locator('button:has-text("Address correction")')
      .click()
    await responsePromise
    await this.page.waitForLoadState()
  }

  getCommonIntakeFormToggle() {
    return this.page.locator('input[name=isCommonIntakeForm]')
  }

  async clickCommonIntakeFormToggle() {
    await this.page.click('input[name=isCommonIntakeForm]')
  }
}
