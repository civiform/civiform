import { Page } from 'playwright'

export class ApplicantQuestions {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async answerAddressQuestion(street: string, line2: string, city: string, state: string, zip: string) {
    await this.page.fill('input:near(:text("Address line 1"))', street);
    await this.page.fill('input:near(:text("Address line 2"))', line2);
    await this.page.fill('input:near(:text("City"))', city);
    await this.page.fill('input:near(:text("State"))', state);
    await this.page.fill('input:near(:text("ZIP Code"))', zip);
  }

  async answerNameQuestion(firstName: string, lastName: string, middleName = '') {
    await this.page.fill('input:near(:text("First name"))', firstName);
    await this.page.fill('input:near(:text("Middle name"))', middleName);
    await this.page.fill('input:near(:text("Last name"))', lastName);
  }

  async answerCheckboxQuestion(checked: Array<string>) {
    for (var index in checked) {
      await this.page.check(`text=${checked[index]}`);
    }
  }

  async answerFileUploadQuestion(text: string) {
    await this.page.setInputFiles('input[type=file]', {
      name: 'file.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from(text)
    });
  }

  async answerRadioButtonQuestion(checked: string) {
    await this.page.check(`text=${checked}`);
  }

  async answerDropdownQuestion(selected: string) {
    await this.page.selectOption('select', { label: selected });
  }

  async answerNumberQuestion(number: string) {
    await this.page.fill('input[type="number"]', number);
  }

  async answerTextQuestion(text: string) {
    await this.page.fill('input[type="text"]', text);
  }

  async addEnumeratorAnswer(entityName: string) {
    await this.page.click('button:text("add entity")');
    await this.page.fill('input:above(#enumerator-field-add-button)', entityName)
  }

  async deleteEnumeratorEntity(entityName: string) {
    await this.page.click(`.cf-enumerator-field:has(input[value="${entityName}"]) button`);
  }

  async applyProgram(programName: string) {
    await this.page.click(`.cf-application-card:has-text("${programName}") .cf-apply-button`);
  }

  async clickNext() {
    await this.page.click('text="Next"');
  }

  async submitFromReviewPage(programName: string) {
    // assert that we're on the review page.
    expect(await this.page.innerText('h1')).toContain('Application review for ' + programName);

    // click on submit button.
    await this.page.click('text="Submit"');

    // Ensure that we redirected to the programs list page.
    expect(await this.page.url().split('/').pop()).toEqual('confirmation');

    // And grab the toast message to verify that the app was submitted.
  }
}
