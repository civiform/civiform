import { Page } from 'playwright'

export class ApplicantQuestions {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async answerAddressQuestion(street: string, city: string, state: string, zip: string) {
    await this.page.fill('[placeholder="Enter your street address"]', street);
    await this.page.fill('[placeholder="City"]', city);
    await this.page.fill('[placeholder="State"]', state);
    await this.page.fill('[placeholder="Zipcode"]', zip);
  }

  async answerNameQuestion(firstName: string, lastName: string, middleName = '') {
    await this.page.fill('[placeholder="First name"]', firstName);
    await this.page.fill('[placeholder="Middle name"]', middleName);
    await this.page.fill('[placeholder="Last name"]', lastName);
  }

  async answerCheckboxQuestion(checked: Array<string>) {
    for (var index in checked) {
      await this.page.check(`text=${checked[index]}`);
    }
  }

  async answerFileUploadQuestion(text: string) {
    await this.page.fill('input[type="text"]', text);
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

  async applyProgram(programName: string) {
    await this.page.click(`.cf-application-card:has-text("${programName}") .apply-button`);
  }

  async saveAndContinue() {
    await this.page.click('text="Save and continue"');
  }
}
