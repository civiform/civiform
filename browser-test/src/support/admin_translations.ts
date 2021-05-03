import { Page } from 'playwright'

export class AdminTranslations {
  public page!: Page

  constructor(page: Page) {
    this.page = page;
  }

  async selectLanguage(language: string) {
    await this.page.click(`.cf-admin-language-link:text("${language}")`);
  }

  async editProgramTranslations(name: string, description: string) {
    await this.page.fill('#localize-display-name', name);
    await this.page.fill('#localize-display-description', description);
    await this.page.click('#update-localizations-button');
  }

  async editQuestionTranslations(text: string, helpText: string) {
    await this.page.fill('#localize-question-text', text);
    await this.page.fill('#localize-question-help-text', helpText);
    await this.page.click('#update-localizations-button');
  }
}
