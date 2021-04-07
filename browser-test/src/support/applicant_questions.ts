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
    await this.page.fill('[placeholder="Zip"]', zip);
  }

  async answerNameQuestion(firstName: string, lastName: string, middleName = '') {
    await this.page.fill('[placeholder="First name"]', firstName);
    await this.page.fill('[placeholder="Middle name"]', middleName);
    await this.page.fill('[placeholder="Last name"]', lastName);
  }

  async answerDropdownQuestion(selected: string) {
    await this.page.selectOption('select', selected);
  }

  async answerNumberQuestion(number: string) {
    await this.page.fill('input[type="number"]', number);
  }

  async answerTextQuestion(text: string) {
    await this.page.fill('input[type="text"]', text);
  }

  async applyProgram(programName: string) {
    await this.page.click(`.cf-application-card:has-text("${programName}") :text("Apply")`);
  }

  async saveAndContinue() {
    await this.page.click('text="Save and continue"');
  }
}
