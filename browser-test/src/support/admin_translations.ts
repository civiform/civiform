import { Page } from 'playwright'

export class AdminTranslations {
  public page!: Page

  constructor(page: Page) {
    this.page = page;
  }

  async selectLanguage(language: string) {
    await this.page.click(`.language-link:has-text("${language}")`);
  }

  async editTranslations(name: string, description: string) {
    await this.page.fill('#localize-display-name', name);
    await this.page.fill('#localize-display-description', description);
    await this.page.click('#update-localizations-button');
  }
}