import {expect} from '@playwright/test'
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
import {AdminProgramImage} from './admin_program_image'
import {validateScreenshot, extractEmailsForRecipient} from '.'

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
  TI_ONLY = 'Trusted intermediaries only',
  SELECT_TI = 'Visible to selected trusted intermediaries only',
  DISABLED = 'Disabled',
}

export enum Eligibility {
  IS_GATING = 'Only allow residents to submit applications if they meet all eligibility requirements',
  IS_NOT_GATING = "Allow residents to submit applications even if they don't meet eligibility requirements",
}

export interface QuestionSpec {
  name: string
  isOptional?: boolean
}

export interface BlockSpec {
  name?: string
  description?: string
  questions: QuestionSpec[]
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

  async gotoDisabledProgramIndexPage() {
    await this.page.click('a:has-text("Disabled")')
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
   * Creates a disabled program with given name.
   */
  async addDisabledProgram(programName: string) {
    await this.addProgram(
      programName,
      'program description',
      'https://usa.gov',
      ProgramVisibility.DISABLED,
    )
  }

  /**
   * Creates a program with given name.
   *
   * @param {boolean} submitNewProgram - If true, the new program will be submitted
   * to the database and then the admin will be redirected to the next page in the
   * program creation flow. If false, the new program information will be filled in
   * but *not* submitted to the database and the current page will still be the
   * program creation page.
   */
  async addProgram(
    programName: string,
    description = 'program description',
    externalLink = 'https://usa.gov',
    visibility = ProgramVisibility.PUBLIC,
    adminDescription = 'admin description',
    isCommonIntake = false,
    selectedTI = 'none',
    confirmationMessage = 'This is the _custom confirmation message_ with markdown\n' +
      '[This is a link](https://www.example.com)\n' +
      'This is a list:\n' +
      '* Item 1\n' +
      '* Item 2\n' +
      '\n' +
      'There are some empty lines below this that should be preserved\n' +
      '\n' +
      '\n' +
      'This link should be autodetected: https://www.example.com\n',
    eligibility = Eligibility.IS_GATING,
    submitNewProgram = true,
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
    await this.page.fill(
      '#program-confirmation-message-textarea',
      confirmationMessage,
    )

    await this.page.check(`label:has-text("${visibility}")`)
    if (visibility == ProgramVisibility.SELECT_TI) {
      const screenshotname = programName.replaceAll(' ', '-').toLowerCase()
      await validateScreenshot(this.page, screenshotname)
      await this.page.check(`label:has-text("${selectedTI}")`)
    }

    await this.chooseEligibility(eligibility)

    if (isCommonIntake && this.getCommonIntakeFormToggle != null) {
      await this.clickCommonIntakeFormToggle()
    }

    if (submitNewProgram) {
      await this.submitProgramDetailsEdits()
    }
  }

  async submitProgramDetailsEdits() {
    await this.page.click('#program-update-button')
    await waitForPageJsLoad(this.page)
  }

  async expectProgramDetailsSaveAndContinueButton() {
    expect(await this.page.innerText('#program-update-button')).toEqual(
      'Save and continue to next step',
    )
  }

  async expectProgramDetailsSaveButton() {
    expect(await this.page.innerText('#program-update-button')).toEqual('Save')
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

    await this.submitProgramDetailsEdits()
  }

  async programNames() {
    await this.gotoAdminProgramsPage()
    const titles = this.page.locator('.cf-admin-program-card .cf-program-title')
    return titles.allTextContents()
  }

  async disabledProgramNames() {
    await this.gotoDisabledProgramIndexPage()
    await waitForPageJsLoad(this.page)
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

  /**
   * Expects a question card either with or without a universal question badge.
   */
  async expectQuestionCardUniversalBadgeState(
    questionName: string,
    universal: boolean,
  ) {
    expect(
      await this.page
        .locator(
          this.withinQuestionCardSelectorInProgramView(
            questionName,
            '.cf-universal-badge',
          ),
        )
        .count(),
    ).toBe(universal ? 1 : 0)
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

  async goToProgramImagePage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.gotoEditDraftProgramPage(programName)
    await this.page.click('button:has-text("Edit program image")')
    await this.expectProgramImagePage()
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
        ':text("Manage program admins")',
      ),
    )
    await waitForPageJsLoad(this.page)
    await this.expectManageProgramAdminsPage()
  }

  async setProgramEligibility(programName: string, eligibility: Eligibility) {
    await this.goToProgramDescriptionPage(programName)
    await this.chooseEligibility(eligibility)
    await this.submitProgramDetailsEdits()
  }

  async chooseEligibility(eligibility: Eligibility) {
    await this.page.check(`label:has-text("${eligibility}")`)
  }

  getEligibilityIsGatingInput() {
    return this.page.locator(`label:has-text("${Eligibility.IS_GATING}")`)
  }

  getEligibilityIsNotGatingInput() {
    return this.page.locator(`label:has-text("${Eligibility.IS_NOT_GATING}")`)
  }

  async gotoEditDisabledDraftProgramPage(programName: string) {
    await this.gotoEditDraftProgramPage(programName, true)
  }

  async gotoEditDraftProgramPage(
    programName: string,
    isProgramDisabled: boolean = false,
  ) {
    await this.gotoAdminProgramsPage()
    if (isProgramDisabled) {
      await this.gotoDisabledProgramIndexPage()
    }
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

  async expectProgramImagePage() {
    const adminProgramImage = new AdminProgramImage(this.page)
    await adminProgramImage.expectProgramImagePage()
  }

  async expectManageProgramAdminsPage() {
    expect(await this.page.innerText('h1')).toContain(
      'Manage admins for program',
    )
  }

  async expectProgramSettingsPage() {
    expect(await this.page.innerText('h1')).toContain('settings')
  }

  async expectAddProgramAdminErrorToast() {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain(
      'as a Program Admin because they do not have an admin account. Have the user log in as admin on the home page, then they can be added as a Program Admin.',
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
    await expect(this.page.locator('#header_edit_button')).toBeVisible()
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

  /**
   * Edit basic block details and required questions
   * @deprecated prefer using {@link #editProgramBlockUsingSpec} instead.
   */
  async editProgramBlock(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[] = [],
  ) {
    await this.editProgramBlockUsingSpec(programName, {
      description: blockDescription,
      questions: questionNames.map((questionName) => {
        return {
          name: questionName,
        }
      }),
    })
  }

  /**
   * Edit basic block details and required and optional questions. Cannot handle more than one optional question.
   * @deprecated prefer using {@link #editProgramBlockUsingSpec} instead. Be aware that the new method will place
   * the optional question in the order as defined in the question array. The older method could only handle one
   * optional question and forced it to be the first question on the list. Tests may need to be updated to handle
   * a different question order.
   */
  async editProgramBlockWithOptional(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[],
    optionalQuestionName: string,
  ) {
    const block: BlockSpec = {
      description: blockDescription,
      questions: [],
    }

    block.questions.push({
      name: optionalQuestionName,
      isOptional: true,
    })

    questionNames.forEach((questionName) => {
      block.questions.push({
        name: questionName,
      })
    })

    await this.editProgramBlockUsingSpec(programName, block)
  }

  /**
   * Edit basic block details and questions
   * @param programName Name of the program to edit
   * @param block Block information
   */
  async editProgramBlockUsingSpec(programName: string, block: BlockSpec) {
    await this.gotoEditDraftProgramPage(programName)

    await clickAndWaitForModal(this.page, 'block-description-modal')

    if (block.name !== undefined) {
      await this.page.fill('#block-name-input', block.name)
    }

    await this.page.fill('textarea', block.description || 'screen description')

    await this.page.click('#update-block-button:not([disabled])')

    for (const question of block.questions) {
      await this.addQuestionFromQuestionBank(question.name)

      if (question.isOptional) {
        await this.page
          .getByTestId(`question-admin-name-${question.name}`)
          .locator(':is(button:has-text("optional"))')
          .click()
      }
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
    await this.page.click('button.cf-close-question-bank-button')
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

  async questionBankNames(universal = false): Promise<string[]> {
    const loc = '.cf-question-bank-element:visible .cf-question-title'
    const titles = this.page.locator(
      universal
        ? '#question-bank-universal ' + loc
        : '#question-bank-nonuniversal ' + loc,
    )
    return titles.allTextContents()
  }

  /**
   * Creates a new program block with the given questions, all marked as required.
   *
   * @deprecated prefer using {@link #addProgramBlockUsingSpec} instead.
   */
  async addProgramBlock(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[] = [],
  ) {
    const questionSpecs: QuestionSpec[] = questionNames.map((qName) => {
      const questionSpec: QuestionSpec = {name: qName, isOptional: false}
      return questionSpec
    })
    return await this.addProgramBlockUsingSpec(
      programName,
      blockDescription,
      questionSpecs,
    )
  }

  async addDisabledProgramBlockUsingSpec(
    programName: string,
    blockDescription = 'screen description',
    questions: QuestionSpec[] = [],
  ) {
    await this.addProgramBlockUsingSpec(
      programName,
      blockDescription,
      questions,
      true,
    )
  }

  /**
   * Creates a new program block with the given questions as defined by {@link QuestionSpec}.
   *
   * Prefer this method over {@link #addProgramBlock}: This method provides the same functionality
   * but also makes it easy to use optional questions.
   */
  async addProgramBlockUsingSpec(
    programName: string,
    blockDescription = 'screen description',
    questions: QuestionSpec[] = [],
    isProgramDisabled: boolean = false,
  ) {
    if (isProgramDisabled) {
      await this.gotoEditDisabledDraftProgramPage(programName)
    } else {
      await this.gotoEditDraftProgramPage(programName)
    }

    await this.page.click('#add-block-button')
    await waitForPageJsLoad(this.page)

    await clickAndWaitForModal(this.page, 'block-description-modal')
    await this.page.fill('textarea', blockDescription)
    await this.page.click('#update-block-button:not([disabled])')
    // Wait for submit and redirect back to this page.
    await this.page.waitForURL(this.page.url())
    await waitForPageJsLoad(this.page)

    for (const question of questions) {
      await this.addQuestionFromQuestionBank(question.name)
      if (question.isOptional) {
        const optionalToggle = this.page
          .locator(this.questionCardSelectorInProgramView(question.name))
          .getByRole('button', {name: 'optional'})
        await optionalToggle.click()
      }
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
    await this.publishAllDrafts()
    await this.expectActiveProgram(programName)
  }

  private static PUBLISH_ALL_MODAL_TITLE =
    'Do you want to publish all draft programs?'

  publishAllProgramsModalLocator() {
    return this.page.locator(
      `.cf-modal:has-text("${AdminPrograms.PUBLISH_ALL_MODAL_TITLE}")`,
    )
  }

  async publishAllDrafts() {
    await this.gotoAdminProgramsPage()
    const modal = await this.openPublishAllDraftsModal()
    const confirmHandle = (await modal.$(
      'button:has-text("Publish all draft programs and questions")',
    ))!
    await confirmHandle.click()

    await waitForPageJsLoad(this.page)
  }

  async openPublishAllDraftsModal() {
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
    const modal = await this.openPublishAllDraftsModal()

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

  async createNewVersionForDisabledProgram(
    programName: string,
    programReadOnlyViewEnabled = true,
  ) {
    await this.createNewVersion(programName, programReadOnlyViewEnabled, true)
  }

  async createNewVersion(
    programName: string,
    programReadOnlyViewEnabled = true,
    isProgramDisabled = false,
  ) {
    await this.gotoAdminProgramsPage()
    if (isProgramDisabled) {
      await this.gotoDisabledProgramIndexPage()
    }
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

    await this.submitProgramDetailsEdits()
    await this.gotoAdminProgramsPage()
    if (isProgramDisabled) {
      await this.gotoDisabledProgramIndexPage()
    }
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
    fromDate = '',
    untilDate = '',
    searchFragment = '',
    applicationStatusOption = '',
  }: {
    fromDate?: string
    untilDate?: string
    searchFragment?: string
    applicationStatusOption?: string
  }) {
    await this.page.fill(`input[type="date"] >> nth=${0}`, fromDate)
    await this.page.fill(`input[type="date"] >> nth=${1}`, untilDate)
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

  async clearFilterProgramApplications() {
    await Promise.all([
      this.page.waitForNavigation(),
      await this.page.click('a:has-text("Clear")'),
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
  async getApplicationPdf() {
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

  async getProgramPdf() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.getByRole('button', {name: 'Download PDF preview'}).click(),
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
        '#download-demographics-csv-modal button:has-text("Download demographic data (CSV)")',
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

  async isPaginationVisibleForApplicationList(): Promise<boolean> {
    const applicationListDiv = this.page.getByTestId('application-list')
    return applicationListDiv.locator('.usa-pagination').isVisible()
  }

  async expectEmailSent(
    numEmailsBefore: number,
    userEmail: string,
    emailBody: string,
    programName: string,
  ) {
    const emailsAfter = await extractEmailsForRecipient(this.page, userEmail)
    expect(emailsAfter.length).toEqual(numEmailsBefore + 1)
    const sentEmail = emailsAfter[emailsAfter.length - 1]
    expect(sentEmail.Subject).toEqual(
      `[Test Message] An update on your application ${programName}`,
    )
    expect(sentEmail.Body.text_part).toContain(emailBody)
  }
}
