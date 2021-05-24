import { Page } from 'playwright'

export class AdminQuestions {
  public page!: Page

  static readonly DOES_NOT_REPEAT_OPTION = 'does not repeat';

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminQuestionsPage() {
    await this.page.click('nav :text("Questions")');
    await this.expectAdminQuestionsPage();
  }

  async expectAdminQuestionsPage() {
    expect(await this.page.innerText('h1')).toEqual('All Questions');
  }

  async fillInQuestionBasics(questionName: string,
    description: string,
    questionText: string,
    helpText: string,
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    // This function should only be called on question create/edit page.
    await this.page.fill('text="Name"', questionName);
    await this.page.fill('text=Description', description);
    await this.page.fill('text=Question Text', questionText);
    await this.page.fill('text=Question help text', helpText);
    await this.page.selectOption('#question-enumerator-select', { label: enumeratorName });
  }

  async updateQuestionText(updateText: string) {
    // This function should only be called on question create/edit page.
    const questionText = await this.page.textContent('#question-text-textarea');
    const updatedText = questionText + updateText;
    await this.page.fill('text=Question Text', updatedText);
    return updatedText;
  }

  selectQuestionTableRow(questionName: string) {
    return `.cf-admin-question-table-row:has-text("${questionName}")`;
  }

  selectWithinQuestionTableRow(questionName: string, selector: string) {
    return this.selectQuestionTableRow(questionName) + ' ' + selector;
  }

  async expectDraftQuestionExist(questionName: string, questionText = '') {
    await this.gotoAdminQuestionsPage();
    const tableInnerText = await this.page.innerText('table');

    expect(tableInnerText).toContain(questionName);
    expect(tableInnerText).toContain(questionText);
    expect(await this.page.innerText(this.selectQuestionTableRow(questionName))).toContain('Edit Draft');
  }

  async expectActiveQuestionExist(questionName: string, questionText = '') {
    await this.gotoAdminQuestionsPage();
    const tableInnerText = await this.page.innerText('table');

    expect(tableInnerText).toContain(questionName);
    expect(tableInnerText).toContain(questionText);
    expect(await this.page.innerText(this.selectQuestionTableRow(questionName))).toContain('View');
    expect(await this.page.innerText(this.selectQuestionTableRow(questionName))).toContain('New Version');
  }

  async gotoQuestionEditPage(questionName: string) {
    await this.gotoAdminQuestionsPage();
    await this.page.click(this.selectWithinQuestionTableRow(questionName, ':text("Edit")'));
    await this.expectQuestionEditPage(questionName);
  }

  async goToQuestionTranslationPage(questionName: string) {
    await this.gotoAdminQuestionsPage();
    await this.page.click(this.selectWithinQuestionTableRow(questionName, ':text("Manage Translations")'));
    await this.expectQuestionTranslationPage();
  }

  async expectQuestionEditPage(questionName: string) {
    expect(await this.page.innerText('h1')).toContain('Edit');
    expect(await this.page.getAttribute('input#question-name-input', 'value')).toEqual(questionName);
  }

  async expectQuestionTranslationPage() {
    expect(await this.page.innerText('h1')).toContain('Manage Question Translations');
  }

  async updateQuestion(questionName: string) {
    await this.gotoQuestionEditPage(questionName);
    const newQuestionText = await this.updateQuestionText(' updated');
    await this.page.click('button:text("Update")');
    await this.expectDraftQuestionExist(questionName, newQuestionText);
  }

  async createNewVersion(questionName: string) {
    await this.gotoAdminQuestionsPage();
    await this.page.click(this.selectWithinQuestionTableRow(questionName, ':text("New Version")'));
    await this.expectQuestionEditPage(questionName);
    const newQuestionText = await this.updateQuestionText(' new version');
    await this.page.click('button:text("Update")');
    await this.expectDraftQuestionExist(questionName, newQuestionText);
  }

  async addAllNonSingleBlockQuestionTypes(questionNamePrefix: string) {
    await this.addAddressQuestion(questionNamePrefix + 'address');
    await this.addCheckboxQuestion(questionNamePrefix + 'checkbox', ['op1', 'op2', 'op3', 'op4']);
    await this.addDateQuestion(questionNamePrefix + 'date');
    await this.addDropdownQuestion(questionNamePrefix + 'dropdown', ['op1', 'op2', 'op3']);
    await this.addNameQuestion(questionNamePrefix + 'name');
    await this.addNumberQuestion(questionNamePrefix + 'number');
    await this.addRadioButtonQuestion(questionNamePrefix + 'radio', ['one', 'two', 'three']);
    await this.addTextQuestion(questionNamePrefix + 'text');
    return [questionNamePrefix + 'address',
    questionNamePrefix + 'checkbox',
    questionNamePrefix + 'date',
    questionNamePrefix + 'dropdown',
    questionNamePrefix + 'name',
    questionNamePrefix + 'number',
    questionNamePrefix + 'radio',
    questionNamePrefix + 'text',
    ];
  }

  async addAllSingleBlockQuestionTypes(questionNamePrefix: string) {
    await this.addEnumeratorQuestion(questionNamePrefix + 'enumerator');
    await this.addFileUploadQuestion(questionNamePrefix + 'fileupload');
    return [questionNamePrefix + 'enumerator',
    questionNamePrefix + 'fileupload',
    ];
  }

  async updateAllQuestions(questions: string[]) {
    for (var i in questions) {
      await this.updateQuestion(questions[i]);
    }
  }

  async createNewVersionForQuestions(questions: string[]) {
    for (var i in questions) {
      await this.createNewVersion(questions[i]);
    }
  }

  async expectDraftQuestions(questions: string[]) {
    for (var i in questions) {
      await this.expectDraftQuestionExist(questions[i]);
    }
  }

  async expectActiveQuestions(questions: string[]) {
    for (var i in questions) {
      await this.expectActiveQuestionExist(questions[i]);
    }
  }

  async addAddressQuestion(questionName: string,
    description = 'address description',
    questionText = 'address question text',
    helpText = 'address question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-address-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addDateQuestion(questionName: string,
                           description = 'date description',
                           questionText = 'date question text',
                           helpText = 'date question help text',
                           enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-date-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addCheckboxQuestion(questionName: string,
    options: Array<string>,
    description = 'checkbox description',
    questionText = 'checkbox question text',
    helpText = 'checkbox question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-checkbox-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    for (var index in options) {
      await this.page.click('#add-new-option');
      await this.page.fill('input:above(#add-new-option)', options[index]);
    }

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addDropdownQuestion(questionName: string,
    options: Array<string>,
    description = 'dropdown description',
    questionText = 'dropdown question text',
    helpText = 'dropdown question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-dropdown-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    for (var index in options) {
      await this.page.click('#add-new-option');
      await this.page.fill('input:above(#add-new-option)', options[index]);
    }

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addFileUploadQuestion(questionName: string,
    description = 'fileupload description',
    questionText = 'fileupload question text',
    helpText = 'fileupload question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-fileupload-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addNameQuestion(questionName: string,
    description = 'name description',
    questionText = 'name question text',
    helpText = 'name question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-name-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addNumberQuestion(questionName: string,
    description = 'number description',
    questionText = 'number question text',
    helpText = 'number question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-number-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addRadioButtonQuestion(questionName: string,
    options: Array<string>,
    description = 'radio button description',
    questionText = 'radio button question text',
    helpText = 'radio button question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-radio_button-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    for (var index in options) {
      await this.page.click('#add-new-option')
      await this.page.fill('input:above(#add-new-option)', options[index])
    }

    await this.page.click('text=Create')

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addTextQuestion(questionName: string,
    description = 'text description',
    questionText = 'text question text',
    helpText = 'text question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-text-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  /**
   * The `enumeratorName` argument is used to make _this_ enumerator question a repeated question.
   */
  async addEnumeratorQuestion(questionName: string,
    description = 'enumerator description',
    questionText = 'enumerator question text',
    helpText = 'enumerator question help text',
    enumeratorName = AdminQuestions.DOES_NOT_REPEAT_OPTION) {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-enumerator-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText, enumeratorName);

    await this.page.fill('text=Repeated Entity Type', 'Entity');

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }
}
