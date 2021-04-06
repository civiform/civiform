import { Page } from 'playwright'

export class ApplicantQuestions {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async answerQuestion(questionName: string, answer: string) {
    await this.page.fill('[name="' + questionName + '"]', answer);
  }

  async applyProgram(programName: string) {
    await this.page.click(`.cf-application-card:has-text("${programName}") :text("Apply")`);
  }

  async saveAndContinue() {
    await this.page.click('text="Save and continue"');
  }
}
