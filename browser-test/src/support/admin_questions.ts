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

  async expectDraftQuestionExist(questionName: string, questionText = '') {
    await this.gotoAdminQuestionsPage();
    const tableInnerText = await this.page.innerText('table');

    expect(tableInnerText).toContain(questionName);
    expect(tableInnerText).toContain(questionText);
    expect(await this.page.innerText(`tr:has-text("${questionName}") a`)).toContain('Edit Draft');
  }

  async expectActiveQuestionExist(questionName: string, questionText = '') {
    await this.gotoAdminQuestionsPage();
    const tableInnerText = await this.page.innerText('table');

    expect(tableInnerText).toContain(questionName);
    expect(tableInnerText).toContain(questionText);
    expect(await this.page.innerText(`tr:has-text("${questionName}") a`)).toContain('New Version');
  }

  async gotoQuestionEditPage(questionName: string) {
    await this.gotoAdminQuestionsPage();
    await this.page.click(`tr:has-text("${questionName}") :text("Edit")`);
    await this.expectQuestionEditPage(questionName);
  }

  async expectQuestionEditPage(questionName: string) {
    expect(await this.page.innerText('h1')).toContain('Edit');
    expect(await this.page.getAttribute('input#question-name-input', 'value')).toEqual(questionName);
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
}
