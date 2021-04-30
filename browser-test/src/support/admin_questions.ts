import { Page } from 'playwright'

export class AdminQuestions {
  public page!: Page

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
    helpText: string) {
    // This function should only be called on question create/edit page.
    await this.page.fill('text="Name"', questionName);
    await this.page.fill('text=Description', description);
    await this.page.fill('text=Question Text', questionText);
    await this.page.fill('text=Question help text', helpText);
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

  async addAllQuestionTypes(questionNamePrefix: string) {
    await this.addAddressQuestion(questionNamePrefix + 'address');
    await this.addCheckboxQuestion(questionNamePrefix + 'checkbox', ['op1', 'op2', 'op3', 'op4']);
    await this.addDropdownQuestion(questionNamePrefix + 'dropdown', ['op1', 'op2', 'op3']);
    await this.addFileUploadQuestion(questionNamePrefix + 'fileupload');
    await this.addNameQuestion(questionNamePrefix + 'name');
    await this.addNumberQuestion(questionNamePrefix + 'number');
    await this.addRadioButtonQuestion(questionNamePrefix + 'radio', ['one', 'two', 'three']);
    await this.addTextQuestion(questionNamePrefix + 'text');
    return [questionNamePrefix + 'address',
    questionNamePrefix + 'checkbox',
    questionNamePrefix + 'dropdown',
    questionNamePrefix + 'fileupload',
    questionNamePrefix + 'name',
    questionNamePrefix + 'number',
    questionNamePrefix + 'radio',
    questionNamePrefix + 'text',
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
    helpText = 'address question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-address-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addCheckboxQuestion(questionName: string,
    options: Array<string>,
    description = 'checkbox description',
    questionText = 'checkbox question text',
    helpText = 'checkbox question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-checkbox-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

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
    helpText = 'dropdown question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-dropdown-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

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
    helpText = 'fileupload question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-fileupload-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addNameQuestion(questionName: string,
    description = 'name description',
    questionText = 'name question text',
    helpText = 'name question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-name-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addNumberQuestion(questionName: string,
    description = 'number description',
    questionText = 'number question text',
    helpText = 'number question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-number-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addRadioButtonQuestion(questionName: string,
    options: Array<string>,
    description = 'radio button description',
    questionText = 'radio button question text',
    helpText = 'radio button question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-radio_button-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

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
    helpText = 'text question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-text-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addRepeaterQuestion(questionName: string,
    description = 'repeater description',
    questionText = 'repeater question text',
    helpText = 'repeater question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-repeater-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }

  async addRepeatedQuestion(questionName: string,
    repeaterName: string,
    description = 'repeated description',
    questionText = 'repeated question text',
    helpText = 'repeated question help text') {
    await this.gotoAdminQuestionsPage();
    await this.page.click('#create-question-button');

    await this.page.click('#create-text-question');

    await this.fillInQuestionBasics(questionName, description, questionText, helpText);

    await this.page.selectOption('#question-enumeration-select', { label: repeaterName });

    await this.page.click('text=Create');

    await this.expectAdminQuestionsPage();

    await this.expectDraftQuestionExist(questionName, questionText);
  }
}
