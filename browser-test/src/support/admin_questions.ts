import {expect} from '@playwright/test'
import {ElementHandle, Page} from 'playwright'
import {dismissModal, waitForAnyModal, waitForPageJsLoad} from './wait'

type QuestionOption = {
  adminName: string
  text: string
}

type QuestionParams = {
  questionName: string
  minNum?: number | null
  maxNum?: number | null
  options?: Array<QuestionOption>
  description?: string
  questionText?: string
  expectedQuestionText?: string | null
  markdownText?: string
  helpText?: string
  enumeratorName?: string
  exportOption?: string
  universal?: boolean
  primaryApplicantInfo?: boolean // Ignored if there isn't one for the question type
  markdown?: boolean
}

// Should match the fieldName set in PrimaryApplicantInfoTag.java
export enum PrimaryApplicantInfoField {
  APPLICANT_DOB = 'primaryApplicantDob',
  APPLICANT_EMAIL = 'primaryApplicantEmail',
  APPLICANT_NAME = 'primaryApplicantName',
  APPLICANT_PHONE = 'primaryApplicantPhone',
}

export enum PrimaryApplicantInfoAlertType {
  NOT_UNIVERSAL = '.cf-pai-not-universal-alert',
  TAG_SET = '.cf-pai-tag-set-alert',
  TAG_SET_NOT_UNIVERSAL = '.cf-pai-tag-set-not-universal-alert',
}

export enum QuestionType {
  ADDRESS = 'address',
  CHECKBOX = 'checkbox',
  CURRENCY = 'currency',
  DATE = 'date',
  DROPDOWN = 'dropdown',
  EMAIL = 'email',
  ID = 'id',
  NAME = 'name',
  NUMBER = 'number',
  RADIO = 'radio',
  TEXT = 'text',
  ENUMERATOR = 'enumerator',
  FILE_UPLOAD = 'file-upload',
}
/* eslint-enable */

export class AdminQuestions {
  public page!: Page

  static readonly DOES_NOT_REPEAT_OPTION = 'does not repeat'

  public static readonly NO_EXPORT_OPTION =
    "Don't include in demographic export"
  public static readonly EXPORT_VALUE_OPTION = 'Include in demographic export'
  public static readonly EXPORT_OBFUSCATED_OPTION =
    'Obfuscate and include in demographic export'
  public static readonly NUMBER_QUESTION_TEXT = 'number question text'
  public static readonly multiOptionInputSelector = (index: number) =>
    `:nth-match(#question-settings div.cf-multi-option-question-option, ${
      index + 1
    }) .cf-multi-option-input input`
  public static readonly multiOptionAdminInputSelector = (index: number) =>
    `:nth-match(#question-settings div.cf-multi-option-question-option, ${
      index + 1
    }) .cf-multi-option-admin-input input`
  public static readonly multiOptionDeleteButtonSelector = (index: number) =>
    `:nth-match(#question-settings div.cf-multi-option-question-option, ${
      index + 1
    }) .multi-option-question-field-remove-button`

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminQuestionsPage() {
    await this.page.click('nav :text("Questions")')
    await waitForPageJsLoad(this.page)
    await this.expectAdminQuestionsPage()
  }

  async goToViewQuestionPage(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.page.click(this.selectQuestionTableRow(questionName))
    await waitForPageJsLoad(this.page)
  }

  async clickSubmitButtonAndNavigate(buttonText: string) {
    await this.page.click(`button:text-is("${buttonText}")`)
    await waitForPageJsLoad(this.page)
  }

  async expectAdminQuestionsPage() {
    await expect(this.page.locator('h1')).toHaveText('All questions')
  }

  selectorForExportOption(exportOption: string) {
    return `label:has-text("${exportOption}") input`
  }

  async expectAdminQuestionsPageWithSuccessToast(successText: string) {
    const toastContainer = await this.page.innerHTML('#toast-container')

    expect(toastContainer).toContain('bg-emerald-200')
    expect(toastContainer).toContain(successText)
    await this.expectAdminQuestionsPage()
  }

  async expectAdminQuestionsPageWithUpdateSuccessToast() {
    await this.expectAdminQuestionsPageWithSuccessToast('updated')
  }

  async expectAdminQuestionsPageWithCreateSuccessToast() {
    await this.expectAdminQuestionsPageWithSuccessToast('created')
  }

  async expectMultiOptionBlankOptionError(
    options: QuestionOption[],
    blankIndices: number[],
  ) {
    const errors = this.page.locator(
      '#question-settings .cf-multi-option-input-error',
    )
    // Checks that the error is not hidden when its corresponding option is blank.
    // The order of the options array corresponds to the order of the errors array.
    for (let i = 0; i < options.length; i++) {
      if (blankIndices.includes(i)) {
        await expect(errors.nth(i)).toBeVisible()
      } else {
        await expect(errors.nth(i)).toBeHidden()
      }
    }
  }

  async expectMultiOptionInvalidOptionAdminError(
    options: QuestionOption[],
    invalidIndices: number[],
  ) {
    const errors = this.page.locator(
      '#question-settings .cf-multi-option-admin-input-error',
    )
    // Checks that the error is not hidden when its corresponding option adminName is invalid.
    // The order of the options array corresponds to the order of the errors array.
    for (let i = 0; i < options.length; i++) {
      if (invalidIndices.includes(i)) {
        await expect(errors.nth(i)).toBeVisible()
      } else {
        await expect(errors.nth(i)).toBeHidden()
      }
    }
  }

  async fillInQuestionBasics({
    questionName,
    description,
    questionText,
    helpText,
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
  }: QuestionParams) {
    // This function should only be called on question create/edit page.
    await this.page.fill('label:has-text("Question text")', questionText ?? '')
    await this.page.fill('label:has-text("Question help text")', helpText ?? '')
    await this.page.fill(
      'label:has-text("Administrative identifier")',
      questionName,
    )
    await this.page.fill(
      'label:has-text("Question note for administrative use only")',
      description ?? '',
    )
    await this.page.selectOption('#question-enumerator-select', {
      label: enumeratorName,
    })
    if (exportOption) {
      await this.selectExportOption(exportOption)
    }
    if (universal) {
      await this.clickUniversalToggle()
    }
  }

  async selectExportOption(exportOption: string) {
    if (!exportOption) {
      throw new Error('A non-empty export option must be provided')
    }
    await this.page.check(this.selectorForExportOption(exportOption))
  }

  async updateQuestionText(updateText: string) {
    // This function should only be called on question create/edit page.
    const questionText = await this.page.textContent('#question-text-textarea')
    const updatedText = questionText! + updateText

    await this.page.fill('text=Question text', updatedText)
    return updatedText
  }

  selectQuestionTableRow(questionName: string) {
    return `.cf-admin-question-table-row:has-text("Admin ID: ${questionName}")`
  }

  selectWithinQuestionTableRow(questionName: string, selector: string) {
    return this.selectQuestionTableRow(questionName) + ' ' + selector
  }

  async expectDraftQuestionExist(
    questionName: string,
    questionText = '',
    markdown = false,
  ) {
    await this.gotoAdminQuestionsPage()
    const questionRowText = await this.page.innerText(
      this.selectQuestionTableRow(questionName),
    )
    if (!markdown) {
      expect(questionRowText).toContain(questionText)
    }
    expect(questionRowText).toContain('Draft')
  }

  async expectActiveQuestionExist(questionName: string, questionText = '') {
    await this.gotoAdminQuestionsPage()
    const questionRowText = await this.page.innerText(
      this.selectQuestionTableRow(questionName),
    )
    expect(questionRowText).toContain(questionText)
    expect(questionRowText).toContain('Active')
  }

  async expectQuestionNotExist(questionName: string) {
    await this.gotoAdminQuestionsPage()
    expect(
      await this.page
        .locator(this.selectQuestionTableRow(questionName))
        .count(),
    ).toEqual(0)
  }

  async expectQuestionProgramReferencesText({
    questionName,
    expectedProgramReferencesText,
    version,
  }: {
    questionName: string
    expectedProgramReferencesText: string
    version: 'draft' | 'active'
  }) {
    await this.gotoAdminQuestionsPage()
    const programReferencesText = await this.page.innerText(
      this.selectWithinQuestionTableRow(
        questionName,
        `:has-text("${version}")`,
      ),
    )
    expect(programReferencesText).toContain(expectedProgramReferencesText)
  }

  async clickOnProgramReferencesModal(questionName: string) {
    await this.page.click(
      this.selectProgramReferencesFromRow(questionName) + ' a',
    )

    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain(
      `Programs referencing ${questionName}`,
    )
    return modal
  }

  async expectProgramReferencesModalContains({
    questionName,
    expectedUsedProgramReferences,
    expectedAddedProgramReferences,
    expectedRemovedProgramReferences,
  }: {
    questionName: string
    expectedUsedProgramReferences: string[]
    expectedAddedProgramReferences: string[]
    expectedRemovedProgramReferences: string[]
  }) {
    const modal = await this.clickOnProgramReferencesModal(questionName)

    const usedReferences = await modal.$$(
      '.cf-admin-question-program-reference-counts-used li',
    )
    const usedReferenceNames = await Promise.all(
      usedReferences.map((reference) => reference.innerText()),
    )
    expect(usedReferenceNames).toEqual(expectedUsedProgramReferences)

    const addedReferences = await modal.$$(
      '.cf-admin-question-program-reference-counts-added li',
    )
    const addedReferenceNames = await Promise.all(
      addedReferences.map((reference) => reference.innerText()),
    )
    expect(addedReferenceNames).toEqual(expectedAddedProgramReferences)

    const removedReferences = await modal.$$(
      '.cf-admin-question-program-reference-counts-removed li',
    )
    const removedReferenceNames = await Promise.all(
      removedReferences.map((reference) => reference.innerText()),
    )
    expect(removedReferenceNames).toEqual(expectedRemovedProgramReferences)

    await dismissModal(this.page)
  }

  private selectProgramReferencesFromRow(questionName: string) {
    return (
      this.selectQuestionTableRow(questionName) +
      ' .cf-admin-question-program-reference-counts'
    )
  }

  async questionNames(): Promise<string[]> {
    await this.gotoAdminQuestionsPage()
    const titles = this.page.locator(
      '.cf-admin-question-table-row .cf-question-title',
    )
    return titles.allTextContents()
  }

  // Note the following two functions do not reload the
  // questions page, so can be used to verify sorting
  async universalQuestionNames(): Promise<string[]> {
    const titles = this.page.locator(
      '#questions-list-universal .cf-admin-question-table-row .cf-question-title',
    )
    return titles.allTextContents()
  }

  async nonUniversalQuestionNames(): Promise<string[]> {
    const titles = this.page.locator(
      '#questions-list-non-universal .cf-admin-question-table-row .cf-question-title',
    )
    return titles.allTextContents()
  }

  private async gotoQuestionEditOrNewVersionPage({
    questionName,
    buttonText,
  }: {
    questionName: string
    buttonText: string
  }) {
    await this.gotoAdminQuestionsPage()
    await this.page.click(
      this.selectWithinQuestionTableRow(
        questionName,
        `button:has-text("${buttonText}")`,
      ),
    )
    await waitForPageJsLoad(this.page)
    await this.expectQuestionEditPage(questionName)
  }

  async gotoQuestionEditPage(questionName: string) {
    await this.gotoQuestionEditOrNewVersionPage({
      questionName,
      buttonText: 'Edit',
    })
  }

  async gotoQuestionNewVersionPage(questionName: string) {
    await this.gotoQuestionEditOrNewVersionPage({
      questionName,
      buttonText: 'Edit',
    })
  }

  async undeleteQuestion(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.openDropdownMenu(questionName)
    await this.page.click(
      this.selectWithinQuestionTableRow(
        questionName,
        ':text("Restore archived")',
      ),
    )
    await waitForPageJsLoad(this.page)
    await this.expectAdminQuestionsPage()
  }

  async discardDraft(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.openDropdownMenu(questionName)
    await this.page.click(
      this.selectWithinQuestionTableRow(questionName, ':text("Discard draft")'),
    )
    await this.page.click('#discard-button')
    await waitForPageJsLoad(this.page)
    await this.expectAdminQuestionsPage()
  }

  async archiveQuestion({
    questionName,
    expectModal,
  }: {
    questionName: string
    expectModal: boolean
  }) {
    await this.gotoAdminQuestionsPage()
    await this.openDropdownMenu(questionName)
    await this.page.click(
      this.selectWithinQuestionTableRow(questionName, ':text("Archive")'),
    )
    if (expectModal) {
      const modal = await waitForAnyModal(this.page)
      expect(await modal.innerText()).toContain(
        'This question cannot be archived since there are still programs using it',
      )
      await dismissModal(this.page)
    } else {
      await waitForPageJsLoad(this.page)
      await this.expectAdminQuestionsPage()
      await this.openDropdownMenu(questionName)
      // Ensure that the page has been reloaded and the "Restore archive" link
      // appears.
      const restoreArchiveIsVisible = this.page.locator(
        this.selectWithinQuestionTableRow(
          questionName,
          ':text("Restore archived")',
        ),
      )
      await expect(restoreArchiveIsVisible).toBeVisible()
    }
  }

  async goToQuestionTranslationPage(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.openDropdownMenu(questionName)
    await this.page.click(
      this.selectWithinQuestionTableRow(
        questionName,
        ':text("Manage translations")',
      ),
    )
    await waitForPageJsLoad(this.page)
    await this.expectQuestionTranslationPage(questionName)
  }

  private async openDropdownMenu(questionName: string) {
    await this.page.click(
      this.selectWithinQuestionTableRow(questionName, '.cf-with-dropdown'),
    )
  }

  async expectQuestionEditPage(questionName: string) {
    expect(await this.page.innerText('h1')).toContain('Edit')
    await expect(this.page.locator('#question-name-input')).toHaveText(
      questionName,
    )
  }

  async expectQuestionTranslationPage(questionName: string) {
    expect(await this.page.innerText('h1')).toContain(
      `Manage question translations: ${questionName}`,
    )
  }

  async updateQuestion(questionName: string) {
    await this.gotoQuestionEditPage(questionName)
    const newQuestionText = await this.updateQuestionText(' updated')

    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName, newQuestionText)
  }

  async changeQuestionHelpText(questionName: string, questionHelpText: string) {
    await this.gotoQuestionEditPage(questionName)
    await this.page.fill('text=Question help text', questionHelpText)
    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName)
  }

  async exportQuestion(questionName: string) {
    await this.gotoQuestionEditPage(questionName)
    await this.page.click(`text="${AdminQuestions.EXPORT_VALUE_OPTION}"`)
    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName)
  }

  async exportQuestionOpaque(questionName: string) {
    await this.gotoQuestionEditPage(questionName)
    await this.page.click(`text="${AdminQuestions.EXPORT_OBFUSCATED_OPTION}"`)
    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName)
  }

  async createNewVersion(questionName: string) {
    await this.gotoQuestionNewVersionPage(questionName)
    const newQuestionText = await this.updateQuestionText(' new version')

    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName, newQuestionText)
  }

  async addQuestionForType(type: QuestionType, questionName: string) {
    switch (type) {
      case QuestionType.ADDRESS:
        await this.addAddressQuestion({
          questionName,
        })
        break
      case QuestionType.CHECKBOX:
        await this.addCheckboxQuestion({
          questionName,
          options: [
            {adminName: 'op1_admin', text: 'op1'},
            {adminName: 'op2_admin', text: 'op2'},
            {adminName: 'op3_admin', text: 'op3'},
            {adminName: 'op4_admin', text: 'op4'},
          ],
        })
        break
      case QuestionType.CURRENCY:
        await this.addCurrencyQuestion({
          questionName,
        })
        break
      case QuestionType.DATE:
        await this.addDateQuestion({questionName})
        break
      case QuestionType.DROPDOWN:
        await this.addDropdownQuestion({
          questionName,
          options: [
            {adminName: 'op1_admin', text: 'op1'},
            {adminName: 'op2_admin', text: 'op2'},
            {adminName: 'op3_admin', text: 'op3'},
          ],
        })
        break
      case QuestionType.EMAIL:
        await this.addEmailQuestion({questionName})
        break
      case QuestionType.ID:
        await this.addIdQuestion({questionName})
        break
      case QuestionType.NAME:
        await this.addNameQuestion({questionName})
        break
      case QuestionType.NUMBER:
        await this.addNumberQuestion({
          questionName,
        })
        break
      case QuestionType.RADIO:
        await this.addRadioButtonQuestion({
          questionName,
          options: [
            {adminName: 'one_admin', text: 'one'},
            {adminName: 'two_admin', text: 'two'},
            {adminName: 'three_admin', text: 'three'},
          ],
        })
        break
      case QuestionType.TEXT:
        await this.addTextQuestion({questionName})
        break
      case QuestionType.ENUMERATOR:
        await this.addEnumeratorQuestion({
          questionName,
        })
        break
      case QuestionType.FILE_UPLOAD:
        await this.addFileUploadQuestion({
          questionName,
        })
        break
      default:
        throw new Error(`Unhandled question type ${type as string}`)
    }
  }

  async updateAllQuestions(questions: string[]) {
    for (const question of questions) {
      await this.updateQuestion(question)
    }
  }

  async createNewVersionForQuestions(questions: string[]) {
    for (const question of questions) {
      await this.createNewVersion(question)
    }
  }

  async expectActiveQuestions(questions: string[]) {
    for (const question of questions) {
      await this.expectActiveQuestionExist(question)
    }
  }

  async addAddressQuestion({
    questionName,
    description = 'address description',
    questionText = 'address question text',
    helpText = 'address question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-address-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addPhoneQuestion({
    questionName,
    description = 'Phone description',
    questionText = 'Phone question text',
    helpText = 'Phone question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
    primaryApplicantInfo = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-phone-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    if (universal && primaryApplicantInfo) {
      await this.clickPrimaryApplicantInfoToggle(
        PrimaryApplicantInfoField.APPLICANT_PHONE,
      )
    }

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addDateQuestion({
    questionName,
    description = 'date description',
    questionText = 'date question text',
    helpText = 'date question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
    primaryApplicantInfo = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-date-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    if (universal && primaryApplicantInfo) {
      await this.clickPrimaryApplicantInfoToggle(
        PrimaryApplicantInfoField.APPLICANT_DOB,
      )
    }

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  /** Fills out the form for a checkbox question, clicks submit, and verifies the new question exists.  */
  async addCheckboxQuestion({
    questionName,
    options,
    minNum = null,
    maxNum = null,
    description = 'checkbox description',
    questionText = 'checkbox question text',
    helpText = 'checkbox question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
  }: QuestionParams) {
    await this.createCheckboxQuestion({
      questionName,
      options,
      minNum,
      maxNum,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  /** Fills out the form for a checkbox question and optionally clicks submit.  */
  async createCheckboxQuestion(
    {
      questionName,
      options,
      minNum = null,
      maxNum = null,
      description = 'checkbox description',
      questionText = 'checkbox question text',
      helpText = 'checkbox question help text',
      enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
      exportOption = AdminQuestions.NO_EXPORT_OPTION,
      universal = false,
    }: QuestionParams,
    clickSubmit = true,
  ) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-checkbox-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    if (minNum != null) {
      await this.page.fill(
        'label:has-text("Minimum number of choices required")',
        String(minNum),
      )
    }
    if (maxNum != null) {
      await this.page.fill(
        'label:has-text("Maximum number of choices allowed")',
        String(maxNum),
      )
    }

    expect(options).toBeTruthy()

    for (let index = 0; index < options!.length; index++) {
      await this.page.click('#add-new-option')
      await this.fillMultiOptionAnswer(index, options![index])
    }

    if (clickSubmit) {
      await this.clickSubmitButtonAndNavigate('Create')
    }
  }

  /** Fills out the form for a dropdown question and optionally clicks submit.  */
  async createDropdownQuestion(
    {
      questionName,
      options,
      description = 'dropdown description',
      questionText = 'dropdown question text',
      helpText = 'dropdown question help text',
      enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
      exportOption = AdminQuestions.NO_EXPORT_OPTION,
      universal = false,
    }: QuestionParams,
    clickSubmit = true,
  ) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-dropdown-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    expect(options).toBeTruthy()

    for (let index = 0; index < options!.length; index++) {
      await this.page.click('#add-new-option')
      await this.fillMultiOptionAnswer(index, options![index])
    }

    if (clickSubmit) {
      await this.clickSubmitButtonAndNavigate('Create')
    }
  }

  /** Deletes a multi-option answer */
  async deleteMultiOptionAnswer(index: number) {
    await this.page.click(AdminQuestions.multiOptionDeleteButtonSelector(index))
  }

  /** Changes the input field of a multi option answer. */
  async changeMultiOptionAnswer(index: number, optionText: string) {
    await this.page.fill(
      AdminQuestions.multiOptionInputSelector(index),
      optionText,
    )
  }

  async addMultiOptionAnswer(option: QuestionOption) {
    await this.page.click('#add-new-option')
    const lastDiv = this.page
      .locator('#question-settings')
      .locator('div.cf-multi-option-question-option')
      .last()
    await lastDiv.locator('.cf-multi-option-input input').fill(option.text)
    await lastDiv
      .locator('.cf-multi-option-admin-input input')
      .fill(option.adminName)
  }

  async fillMultiOptionAnswer(index: number, option: QuestionOption) {
    await this.page.fill(
      AdminQuestions.multiOptionInputSelector(index),
      option.text,
    )
    await this.page.fill(
      AdminQuestions.multiOptionAdminInputSelector(index),
      option.adminName,
    )
  }

  async expectNewMultiOptionAnswer(index: number, option: QuestionOption) {
    await this.expectMultiOptionAnswer(index, option, true)
  }

  async expectExistingMultiOptionAnswer(index: number, option: QuestionOption) {
    await this.expectMultiOptionAnswer(index, option, false)
  }

  async expectMultiOptionAnswer(
    index: number,
    option: QuestionOption,
    adminNameIsEditable: boolean,
  ) {
    await expect(
      this.page.locator(AdminQuestions.multiOptionInputSelector(index)),
    ).toHaveValue(option.text)
    await expect(
      this.page.locator(AdminQuestions.multiOptionAdminInputSelector(index)),
    ).toHaveValue(option.adminName)
    await expect(
      this.page.locator(AdminQuestions.multiOptionAdminInputSelector(index)),
    ).toBeEditable({editable: adminNameIsEditable})
  }

  async addCurrencyQuestion({
    questionName,
    description = 'currency description',
    questionText = 'currency question text',
    helpText = 'currency question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
    markdown = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()
    await this.page.click('#create-question-button')
    await this.page.click('#create-currency-question')
    await waitForPageJsLoad(this.page)
    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })
    await this.clickSubmitButtonAndNavigate('Create')
    await this.expectAdminQuestionsPageWithCreateSuccessToast()
    await this.expectDraftQuestionExist(questionName, questionText, markdown)
  }

  /** Fills out the form for a dropdown question, clicks submit, and verifies the new question exists.  */
  async addDropdownQuestion({
    questionName,
    options,
    description = 'dropdown description',
    questionText = 'dropdown question text',
    helpText = 'dropdown question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
  }: QuestionParams) {
    await this.createDropdownQuestion({
      questionName,
      options,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addFileUploadQuestion({
    questionName,
    description = 'fileupload description',
    questionText = 'fileupload question text',
    helpText = 'fileupload question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-fileupload-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addStaticQuestion({
    questionName,
    description = 'static description',
    questionText = 'static question text',
    markdownText = '\n[Here is a link](https://www.example.com)\n',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
  }: QuestionParams) {
    await this.createStaticQuestion({
      questionName,
      description,
      questionText,
      markdownText,
      enumeratorName,
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPage()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async createStaticQuestion({
    questionName,
    description = 'static description',
    questionText = 'static question text',
    markdownText = '',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-static-question')
    await waitForPageJsLoad(this.page)

    await this.page.fill(
      'label:has-text("Question text")',
      questionText + markdownText,
    )
    await this.page.fill(
      'label:has-text("Administrative identifier")',
      questionName,
    )
    await this.page.fill(
      'label:has-text("Question note for administrative use only")',
      description,
    )
    await this.page.selectOption('#question-enumerator-select', {
      label: enumeratorName,
    })
  }

  async addNameQuestion({
    questionName,
    description = 'name description',
    questionText = 'name question text',
    helpText = 'name question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
    primaryApplicantInfo = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-name-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    if (universal && primaryApplicantInfo) {
      await this.clickPrimaryApplicantInfoToggle(
        PrimaryApplicantInfoField.APPLICANT_NAME,
      )
    }

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addNumberQuestion({
    questionName,
    description = 'number description',
    questionText = AdminQuestions.NUMBER_QUESTION_TEXT,
    helpText = 'number question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-number-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addRadioButtonQuestion({
    questionName,
    options,
    description = 'radio button description',
    questionText = 'radio button question text',
    helpText = 'radio button question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
  }: QuestionParams) {
    await this.createRadioButtonQuestion({
      questionName,
      options,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
    })

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async createRadioButtonQuestion(
    {
      questionName,
      options,
      description = 'radio button description',
      questionText = 'radio button question text',
      helpText = 'radio button question help text',
      enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
      exportOption = AdminQuestions.NO_EXPORT_OPTION,
      universal = false,
    }: QuestionParams,
    clickSubmit = true,
  ) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-radio_button-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    expect(options).toBeTruthy()

    for (let index = 0; index < options!.length; index++) {
      await this.page.click('#add-new-option')
      await this.fillMultiOptionAnswer(index, options![index])
    }

    if (clickSubmit) {
      await this.clickSubmitButtonAndNavigate('Create')
    }
  }

  async addTextQuestion({
    questionName,
    description = 'text description',
    questionText = 'text question text',
    helpText = 'text question help text',
    minNum = null,
    maxNum = null,
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-text-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    if (minNum != null) {
      await this.page.fill('label:has-text("Minimum length")', String(minNum))
    }
    if (maxNum != null) {
      await this.page.fill('label:has-text("Maximum length")', String(maxNum))
    }
    if (!markdown) {
      await this.expectDraftQuestionExist(questionName, questionText)
    }

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()
  }

  async clickUniversalToggle() {
    await this.page.click('#universal-toggle')
  }

  async getUniversalToggleValue(): Promise<string> {
    return this.page.inputValue('#universal-toggle-input')
  }

  async clickPrimaryApplicantInfoToggle(field: PrimaryApplicantInfoField) {
    await this.page.click(`#${field}-toggle`)
  }

  async getPrimaryApplicantInfoToggleValue(fieldName: string) {
    return this.page.inputValue(`#${fieldName}-toggle-input`)
  }

  async expectPrimaryApplicantInfoAlert(
    type: PrimaryApplicantInfoAlertType,
    visible: boolean,
  ) {
    const alert = this.page.locator(type.valueOf())
    await expect(alert).toBeVisible({visible: visible})
  }

  async expectPrimaryApplicantInfoSectionVisible(visible: boolean) {
    await expect(this.page.locator('#primary-applicant-info')).toBeVisible({
      visible: visible,
    })
  }

  async expectPrimaryApplicantInfoToggleVisible(
    fieldName: string,
    visible: boolean,
  ) {
    await expect(this.page.locator(`#${fieldName}-toggle`)).toBeVisible({
      visible: visible,
    })
  }

  async expectPrimaryApplicantInfoToggleValue(
    fieldName: string,
    value: boolean,
  ) {
    expect(await this.getPrimaryApplicantInfoToggleValue(fieldName)).toEqual(
      value.toString(),
    )
  }

  async addIdQuestion({
    questionName,
    description = 'id description',
    questionText = 'id question text',
    helpText = 'id question help text',
    minNum = null,
    maxNum = null,
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-id-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    if (minNum != null) {
      await this.page.fill('label:has-text("Minimum length")', String(minNum))
    }
    if (maxNum != null) {
      await this.page.fill('label:has-text("Maximum length")', String(maxNum))
    }

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addEmailQuestion({
    questionName,
    description = 'email description',
    questionText = 'email question text',
    helpText = 'email question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
    universal = false,
    primaryApplicantInfo = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-email-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    if (universal && primaryApplicantInfo) {
      await this.clickPrimaryApplicantInfoToggle(
        PrimaryApplicantInfoField.APPLICANT_EMAIL,
      )
    }

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async expectEnumeratorPreviewValues({
    entityNameInputLabelText,
    deleteEntityButtonText,
    addEntityButtonText,
  }: {
    entityNameInputLabelText: string
    deleteEntityButtonText: string
    addEntityButtonText: string
  }) {
    // Fix me! ESLint: playwright/prefer-web-first-assertions
    // Directly switching to the best practice method fails
    // because of a locator stict mode violation. That is it
    // returns multiple elements.
    //
    // Recommended prefer-web-first-assertions fix:
    // await expect(this.page.locator('.cf-entity-name-input label')).toHaveText(
    //   entityNameInputLabelText,
    // )
    // await expect(this.page.locator('.cf-enumerator-delete-button')).toHaveText(
    //   deleteEntityButtonText,
    // )
    // await expect(this.page.locator('#enumerator-field-add-button')).toHaveText(
    //   addEntityButtonText,
    // )
    expect(await this.page.innerText('.cf-entity-name-input label')).toBe(
      entityNameInputLabelText,
    )
    expect(await this.page.innerText('.cf-enumerator-delete-button')).toBe(
      deleteEntityButtonText,
    )
    expect(await this.page.innerText('#enumerator-field-add-button')).toBe(
      addEntityButtonText,
    )
  }

  async expectCommonPreviewValues({
    questionText,
    questionHelpText,
  }: {
    questionText: string
    questionHelpText: string
  }) {
    await expect(this.page.locator('.cf-applicant-question-text')).toHaveText(
      questionText,
    )
    await expect(
      this.page.locator('.cf-applicant-question-help-text'),
    ).toHaveText(questionHelpText)
  }

  async expectPreviewOptions(options: string[]) {
    const optionElements = Array.from(
      await this.page.$$('#sample-question .cf-multi-option-question-option'),
    )
    const existingOptions = await Promise.all(
      optionElements.map((el) => {
        return (el as ElementHandle<HTMLElement>).innerText()
      }),
    )
    expect(existingOptions).toEqual(options)
  }

  async expectPreviewOptionsWithMarkdown(options: string[]) {
    const optionElements = Array.from(
      await this.page.$$('#sample-question .cf-multi-option-value'),
    )
    const existingOptions = await Promise.all(
      optionElements.map((el) => {
        return (el as ElementHandle<HTMLElement>).innerHTML()
      }),
    )
    expect(existingOptions).toEqual(options)
  }

  /**
   * The `enumeratorName` argument is used to make _this_ enumerator question a repeated question.
   */
  async addEnumeratorQuestion({
    questionName,
    description = 'enumerator description',
    questionText = 'enumerator question text',
    helpText = 'enumerator question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = '',
    universal = false,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-enumerator-question')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
      universal,
    })

    await this.page.fill('text=Repeated entity type', 'Entity')

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async questionBankNames(): Promise<string[]> {
    const titles = this.page.locator(
      '.cf-question-bank-element:visible .cf-question-title',
    )
    return titles.allTextContents()
  }

  /** Clicks on the questions sorting dropdown and selects the specified sortOption. The sortOption should match the value of the desired option. */
  async sortQuestions(sortOption: string) {
    return this.page.locator('#question-bank-sort').selectOption(sortOption)
  }
}
