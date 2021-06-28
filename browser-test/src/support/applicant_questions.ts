import { Page } from 'playwright'

export class ApplicantQuestions {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async answerAddressQuestion(street: string, line2: string, city: string, state: string, zip: string) {
    await this.page.fill('.cf-address-street-1 input', street);
    await this.page.fill('.cf-address-street-2 input', line2);
    await this.page.fill('.cf-address-city input', city);
    await this.page.fill('.cf-address-state input', state);
    await this.page.fill('.cf-address-zip input', zip);
  }

  async answerNameQuestion(firstName: string, lastName: string, middleName = '') {
    await this.page.fill('.cf-name-first input', firstName);
    await this.page.fill('.cf-name-middle input', middleName);
    await this.page.fill('.cf-name-last input', lastName);
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
    await this.page.selectOption('.cf-dropdown-question select', { label: selected });
  }

  async answerNumberQuestion(number: string) {
    await this.page.fill('input[type="number"]', number);
  }

  async answerDateQuestion(date: string) {
    await this.page.fill('input[type="date"]', date);
  }

  async answerTextQuestion(text: string) {
    await this.page.fill('input[type="text"]', text);
  }

  async answerEmailQuestion(email: string) {
    await this.page.fill('input[type="email"]', email);
  }

  async addEnumeratorAnswer(entityName: string) {
    await this.page.click('button:text("add entity")');
    await this.page.fill('#enumerator-fields .cf-enumerator-field:last-of-type input', entityName)
  }

  async applyProgram(programName: string) {
    // User clicks the apply button on an application card. It takes them to the application info page.
    await this.page.click(`.cf-application-card:has-text("${programName}") .cf-apply-button`);

    // The user can see the application preview page. Clicking on apply sends them to the first unanswered question.
    await this.page.click(`#continue-application-button`);
  }

  async clickNext() {
    await this.page.click('text="Next"');
  }

  async clickReview() {
    await this.page.click('text="Review"');
  }

  async clickUpload() {
    await this.page.click('text="Upload"');
  }

  async deleteEnumeratorEntity(entityName: string) {
    await this.page.click(`.cf-enumerator-field:has(input[value="${entityName}"]) button`);
  }

  async submitFromReviewPage(programName: string) {
    // Assert that we're on the review page.
    expect(await this.page.innerText('h1')).toContain('Program application review');

    // Click on submit button.
    await this.page.click('text="Submit"');

    await this.page.click('text="Apply to another program"');

    // Ensure that we redirected to the programs list page.
    expect(await this.page.url().split('/').pop()).toEqual('programs');
  }

  async validateHeader(lang: string) {
    expect(await this.page.getAttribute('html', 'lang')).toEqual(lang);
    expect(await this.page.innerHTML('head'))
      .toContain('<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">');
  }
}
