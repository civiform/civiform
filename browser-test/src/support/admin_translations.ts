import {Page} from 'playwright'

export class AdminTranslations {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async selectLanguage(language: string) {
    await this.page.click(`.cf-admin-language-link:text("${language}")`)
  }

  async editProgramTranslations(name: string, description: string) {
    await this.page.fill('#localize-display-name', name)
    await this.page.fill('#localize-display-description', description)
    await this.page.click('#update-localizations-button')
  }

  async editQuestionTranslations(
    text: string,
    helpText: string,
    configText = [],
  ) {
    await this.page.fill('#localize-question-text', text)
    await this.page.fill('#localize-question-help-text', helpText)

    // If there are multi-option inputs to translate, fill them in
    // with the provided translations in configText
    const optionInputs = await this.page.$$('[name="options[]"]')
    for (let index = 0; index < optionInputs.length; index++) {
      await optionInputs[index].fill(configText[index])
    }

    const enumeratorInput = await this.page.$('[name="entityType"]')
    if (enumeratorInput) {
      await enumeratorInput.fill(configText[0])
    }

    await this.page.click('#update-localizations-button')
  }
}
