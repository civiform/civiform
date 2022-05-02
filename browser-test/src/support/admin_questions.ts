import { Page } from 'playwright'
import { waitForPageJsLoad } from './wait'

type QuestionParams = {
  questionName: string
  options?: Array<string>
  description?: string
  questionText?: string
  helpText?: string
  enumeratorName?: string
  exportOption?: string
}

export class AdminQuestions {
  public page!: Page

  static readonly DOES_NOT_REPEAT_OPTION = 'does not repeat'
  
  public static readonly NO_EXPORT_OPTION = 'No export'
  public static readonly EXPORT_VALUE_OPTION = 'Export Value'
  public static readonly EXPORT_OBFUSCATED_OPTION = 'Export Obfuscated'

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
      await this.page.click('text=View')
      await waitForPageJsLoad(this.page)
  }

  async clickSubmitButtonAndNavigate(buttonText: string) {
    await this.page.click(`button:has-text("${buttonText}")`)
    await waitForPageJsLoad(this.page)
  }

  async expectAdminQuestionsPage() {
    expect(await this.page.innerText('h1')).toEqual('All Questions')
  }

  async expectViewOnlyQuestion(questionName: string) {
    expect(await this.page.isDisabled('text=No Export')).toEqual(true)
    // TODO(sgoldblatt): This test does not find any questions need to look into
    // expect(await this.page.isDisabled(`text=${questionName}`)).toEqual(true)
  }

  selectorForExportOption(exportOption: string) {
    return `label:has-text("${exportOption}") input`
  }

  async expectAdminQuestionsPageWithSuccessToast(successText: string) {
    const toastContainer = await this.page.innerHTML('#toast-container')

    expect(toastContainer).toContain('bg-green-200')
    expect(toastContainer).toContain(successText)
    await this.expectAdminQuestionsPage()
  }

  async expectAdminQuestionsPageWithUpdateSuccessToast() {
    await this.expectAdminQuestionsPageWithSuccessToast('updated')
  }

  async expectAdminQuestionsPageWithCreateSuccessToast() {
    await this.expectAdminQuestionsPageWithSuccessToast('created')
  }

  async expectMultiOptionBlankOptionError(options: String[]) {
    const questionSettings = await this.page.$('#question-settings')
    const errors = await questionSettings.$$('.cf-multi-option-input-error')
    // Checks that the error is not hidden when it's corresponding option is empty. The order of the options array corresponds to the order of the errors array.
    for (let i in errors) {
      if (options[i] === '') {
        expect(await errors[i].isHidden()).toEqual(false)
      } else {
        expect(await errors[i].isHidden()).toEqual(true)
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
  } : QuestionParams) {
    // This function should only be called on question create/edit page.
    await this.page.fill('label:has-text("Name")', questionName)
    await this.page.fill('label:has-text("Description")', description)
    await this.page.fill('label:has-text("Question Text")', questionText)
    await this.page.fill('label:has-text("Question help text")', helpText)
    await this.page.selectOption('#question-enumerator-select', {
      label: enumeratorName,
    })
    if (exportOption) {
      await this.selectExportOption(exportOption)
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
    const updatedText = questionText + updateText

    await this.page.fill('text=Question Text', updatedText)
    return updatedText
  }

  selectQuestionTableRow(questionName: string) {
    return `.cf-admin-question-table-row:has-text("${questionName}")`
  }

  selectWithinQuestionTableRow(questionName: string, selector: string) {
    return this.selectQuestionTableRow(questionName) + ' ' + selector
  }

  async expectDraftQuestionExist(questionName: string, questionText = '') {
    await this.gotoAdminQuestionsPage()
    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
    expect(
      await this.page.innerText(this.selectQuestionTableRow(questionName))
    ).toContain('Edit Draft')
  }

  async expectActiveQuestionExist(questionName: string, questionText = '') {
    await this.gotoAdminQuestionsPage()
    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
    expect(
      await this.page.innerText(this.selectQuestionTableRow(questionName))
    ).toContain('View')
    expect(
      await this.page.innerText(this.selectQuestionTableRow(questionName))
    ).toContain('New Version')
  }

  async expectActiveQuestionNotExist(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await waitForPageJsLoad(this.page)
    const tableInnerText = await this.page.innerText('table')
    expect(tableInnerText).not.toContain(questionName)
  }

  async gotoQuestionEditPage(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.page.click(
      this.selectWithinQuestionTableRow(questionName, ':text("Edit")')
    )
    await waitForPageJsLoad(this.page)
    await this.expectQuestionEditPage(questionName)
  }

  async undeleteQuestion(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.page.click(
      this.selectWithinQuestionTableRow(questionName, ':text("Restore")')
    )
    await waitForPageJsLoad(this.page)
    await this.expectAdminQuestionsPage()
  }

  async discardDraft(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.page.click(
      this.selectWithinQuestionTableRow(questionName, ':text("Discard Draft")')
    )
    await waitForPageJsLoad(this.page)
    await this.expectAdminQuestionsPage()
  }

  async archiveQuestion(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.page.click(
      this.selectWithinQuestionTableRow(questionName, ':text("Archive")')
    )
    await waitForPageJsLoad(this.page)
    await this.expectAdminQuestionsPage()
  }

  async goToQuestionTranslationPage(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.page.click(
      this.selectWithinQuestionTableRow(
        questionName,
        ':text("Manage Translations")'
      )
    )
    await waitForPageJsLoad(this.page)
    await this.expectQuestionTranslationPage()
  }

  async expectQuestionEditPage(questionName: string) {
    expect(await this.page.innerText('h1')).toContain('Edit')
    expect(
      await this.page.getAttribute('input#question-name-input', 'value')
    ).toEqual(questionName)
  }

  async expectQuestionTranslationPage() {
    expect(await this.page.innerText('h1')).toContain(
      'Manage Question Translations'
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
    await this.page.fill('text=Question Help Text', questionHelpText)
    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName)
  }

  async exportQuestion(questionName: string) {
    await this.gotoQuestionEditPage(questionName)
    await this.page.click('text="Export Value"')
    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName)
  }

  async exportQuestionOpaque(questionName: string) {
    await this.gotoQuestionEditPage(questionName)
    await this.page.click('text="Export Obfuscated"')
    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName)
  }

  async createNewVersion(questionName: string) {
    await this.gotoAdminQuestionsPage()
    await this.page.click(
      this.selectWithinQuestionTableRow(questionName, ':text("New Version")')
    )
    await waitForPageJsLoad(this.page)
    await this.expectQuestionEditPage(questionName)
    const newQuestionText = await this.updateQuestionText(' new version')

    await this.clickSubmitButtonAndNavigate('Update')
    await this.expectAdminQuestionsPageWithUpdateSuccessToast()
    await this.expectDraftQuestionExist(questionName, newQuestionText)
  }

  async addAllNonSingleBlockQuestionTypes(questionNamePrefix: string) {
    await this.addAddressQuestion({
      questionName: questionNamePrefix + 'address',
    })
    await this.addCheckboxQuestion({
      questionName: questionNamePrefix + 'checkbox',
      options: ['op1', 'op2', 'op3', 'op4'],
    })
    await this.addCurrencyQuestion({
      questionName: questionNamePrefix + 'currency',
    })
    await this.addDateQuestion({ questionName: questionNamePrefix + 'date' })
    await this.addDropdownQuestion({
      questionName: questionNamePrefix + 'dropdown',
      options: ['op1', 'op2', 'op3'],
    })
    await this.addEmailQuestion({ questionName: questionNamePrefix + 'email' })
    await this.addIdQuestion({ questionName: questionNamePrefix + 'id' })
    await this.addNameQuestion({ questionName: questionNamePrefix + 'name' })
    await this.addNumberQuestion({
      questionName: questionNamePrefix + 'number',
    })
    await this.addRadioButtonQuestion({
      questionName: questionNamePrefix + 'radio',
      options: ['one', 'two', 'three'],
    })
    await this.addTextQuestion({ questionName: questionNamePrefix + 'text' })
    return [
      questionNamePrefix + 'address',
      questionNamePrefix + 'checkbox',
      questionNamePrefix + 'currency',
      questionNamePrefix + 'date',
      questionNamePrefix + 'dropdown',
      questionNamePrefix + 'email',
      questionNamePrefix + 'id',
      questionNamePrefix + 'name',
      questionNamePrefix + 'number',
      questionNamePrefix + 'radio',
      questionNamePrefix + 'text',
    ]
  }

  async addAllSingleBlockQuestionTypes(questionNamePrefix: string) {
    await this.addEnumeratorQuestion({
      questionName: questionNamePrefix + 'enumerator',
    })
    await this.addFileUploadQuestion({
      questionName: questionNamePrefix + 'fileupload',
    })
    return [
      questionNamePrefix + 'enumerator',
      questionNamePrefix + 'fileupload',
    ]
  }

  async updateAllQuestions(questions: string[]) {
    for (var i in questions) {
      await this.updateQuestion(questions[i])
    }
  }

  async createNewVersionForQuestions(questions: string[]) {
    for (var i in questions) {
      await this.createNewVersion(questions[i])
    }
  }

  async expectDraftQuestions(questions: string[]) {
    for (var i in questions) {
      await this.expectDraftQuestionExist(questions[i])
    }
  }

  async expectActiveQuestions(questions: string[]) {
    for (var i in questions) {
      await this.expectActiveQuestionExist(questions[i])
    }
  }

  async addAddressQuestion({
    questionName,
    description = 'address description',
    questionText = 'address question text',
    helpText = 'address question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
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
    })

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
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addCheckboxQuestion({
    questionName,
    options,
    description = 'checkbox description',
    questionText = 'checkbox question text',
    helpText = 'checkbox question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
  }: QuestionParams) {
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
    })

    for (var index in options) {
      await this.page.click('#add-new-option')
      var matchIndex = Number(index) + 1
      await this.page.fill(
        `:nth-match(#question-settings div.flex-row, ${matchIndex}) input`,
        options[index]
      )
    }

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  /** Fills out the form for a dropdown question and clicks submit.  */
  async createDropdownQuestion({
    questionName,
    options,
    description = 'dropdown description',
    questionText = 'dropdown question text',
    helpText = 'dropdown question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-dropdown-question')
    await this.page.waitForURL('**/admin/questions/new?type=dropdown')
    await waitForPageJsLoad(this.page)

    await this.fillInQuestionBasics({
      questionName,
      description,
      questionText,
      helpText,
      enumeratorName,
      exportOption,
    })

    for (let index in options) {
      await this.page.click('#add-new-option')
      let matchIndex = Number(index) + 1
      await this.changeMultiOptionAnswer(matchIndex, options[index])
    }

    await this.clickSubmitButtonAndNavigate('Create')
  }

  /** Changes the input field of a multi option answer. */
  async changeMultiOptionAnswer(index: number, text: string) {
    await this.page.fill(
      `:nth-match(#question-settings div.cf-multi-option-question-option, ${index}) input`,
      text
    )
  }

  async addCurrencyQuestion({
    questionName,
    description = 'currency description',
    questionText = 'currency question text',
    helpText = 'currency question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
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
    })
    await this.clickSubmitButtonAndNavigate('Create')
    await this.expectAdminQuestionsPageWithCreateSuccessToast()
    await this.expectDraftQuestionExist(questionName, questionText)
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
  }: QuestionParams) {
    await this.createDropdownQuestion({
      questionName,
      options,
      description,
      questionText,
      helpText,
      enumeratorName,
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
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addStaticQuestion({
    questionName,
    description = 'static description',
    questionText = 'static question text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = '',
  }: QuestionParams) {
    await this.gotoAdminQuestionsPage()

    await this.page.click('#create-question-button')
    await this.page.click('#create-static-question')
    await waitForPageJsLoad(this.page)

    await this.page.fill('label:has-text("Name")', questionName)
    await this.page.fill('label:has-text("Description")', description)
    await this.page.fill('label:has-text("Question Text")', questionText)
    await this.page.selectOption('#question-enumerator-select', {
      label: enumeratorName,
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPage()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addNameQuestion({
    questionName,
    description = 'name description',
    questionText = 'name question text',
    helpText = 'name question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
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
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addNumberQuestion({
    questionName,
    description = 'number description',
    questionText = 'number question text',
    helpText = 'number question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
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
    })

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async createRadioButtonQuestion({
    questionName,
    options,
    description = 'radio button description',
    questionText = 'radio button question text',
    helpText = 'radio button question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
  }: QuestionParams) {
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
    })

    for (var index in options) {
      await this.page.click('#add-new-option')
      var matchIndex = Number(index) + 1
      await this.page.fill(
        `:nth-match(#question-settings div.flex-row, ${matchIndex}) input`,
        options[index]
      )
    }

    await this.clickSubmitButtonAndNavigate('Create')
  }

  async addTextQuestion({
    questionName,
    description = 'text description',
    questionText = 'text question text',
    helpText = 'text question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
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
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }

  async addIdQuestion({
    questionName,
    description = 'id description',
    questionText = 'id question text',
    helpText = 'id question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION,
    exportOption = AdminQuestions.NO_EXPORT_OPTION,
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
    })

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
    })

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
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
    })

    await this.page.fill('text=Repeated Entity Type', 'Entity')

    await this.clickSubmitButtonAndNavigate('Create')

    await this.expectAdminQuestionsPageWithCreateSuccessToast()

    await this.expectDraftQuestionExist(questionName, questionText)
  }
}
