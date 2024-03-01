import {expect} from '@playwright/test'
import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'

type ProgramStatusTranslationParams = {
  configuredStatusText: string
  statusText: string
  // Undefined implies that no email input is expected to be present.
  statusEmail?: string
}

export class AdminTranslations {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async selectLanguage(language: string) {
    await this.page.click(`.cf-admin-language-link:text("${language}")`)
    await waitForPageJsLoad(this.page)
  }

  async editProgramTranslations({
    name,
    description,
    statuses = [],
  }: {
    name: string
    description: string
    statuses: ProgramStatusTranslationParams[]
  }) {
    await this.page.fill('text=Program name', name)
    await this.page.fill('text=Program description', description)

    for (const status of statuses) {
      await this.page.fill(
        this.statusNameFieldSelector(status.configuredStatusText),
        status.statusText,
      )
      const isEmailVisible = await this.page.isVisible(
        this.statusEmailFieldSelector(status.configuredStatusText),
      )
      if (status.statusEmail === undefined) {
        expect(isEmailVisible).toBe(false)
      } else {
        expect(isEmailVisible).toBe(true)
        await this.page.fill(
          this.statusEmailFieldSelector(status.configuredStatusText),
          status.statusEmail,
        )
      }
    }

    await this.page.click('#update-localizations-button')
    await waitForPageJsLoad(this.page)
  }

  async expectProgramTranslation({
    expectProgramName,
    expectProgramDescription,
  }: {
    expectProgramName: string
    expectProgramDescription: string
  }) {
    const programNameValue = await this.page.inputValue('text=Program name')
    expect(programNameValue).toEqual(expectProgramName)
    const programDescriptionValue = await this.page.inputValue(
      'text=Program description',
    )
    expect(programDescriptionValue).toEqual(expectProgramDescription)
  }

  async expectNoProgramStatusTranslations() {
    expect(await this.page.isVisible(':has-text("Application status: ")')).toBe(
      false,
    )
  }

  async expectProgramStatusTranslationWithEmail({
    configuredStatusText,
    expectStatusText,
    expectStatusEmail,
  }: {
    configuredStatusText: string
    expectStatusText: string
    expectStatusEmail: string
  }) {
    const statusTextValue = await this.page.inputValue(
      this.statusNameFieldSelector(configuredStatusText),
    )
    expect(statusTextValue).toEqual(expectStatusText)
    const statusEmailValue = await this.page.inputValue(
      this.statusEmailFieldSelector(configuredStatusText),
    )
    expect(statusEmailValue).toEqual(expectStatusEmail)
  }

  async expectProgramStatusTranslationWithNoEmail({
    configuredStatusText,
    expectStatusText,
  }: {
    configuredStatusText: string
    expectStatusText: string
  }) {
    const statusTextValue = await this.page.inputValue(
      this.statusNameFieldSelector(configuredStatusText),
    )
    expect(statusTextValue).toEqual(expectStatusText)
    const isStatusEmailVisible = await this.page.isVisible(
      this.statusEmailFieldSelector(configuredStatusText),
    )
    expect(isStatusEmailVisible).toBe(false)
  }

  /**
   * Note that the program name and description must be translated using
   * {@link editProgramTranslations} before calling this method.
   */
  async editProgramImageDescription(imageDescription: string) {
    await this.page.fill('text=Program image description', imageDescription)
    await this.page.click('#update-localizations-button')
    await waitForPageJsLoad(this.page)
  }

  async expectNoProgramImageDescription() {
    expect(
      await this.page.locator('text=Program image description').count(),
    ).toEqual(0)
  }

  async expectProgramImageDescriptionTranslation(
    expectImageDescription: string,
  ) {
    const imageDescriptionValue = await this.page.inputValue(
      'text=Program image description',
    )
    expect(imageDescriptionValue).toEqual(expectImageDescription)
  }

  async editQuestionTranslations(
    text: string,
    helpText: string,
    configText: string[] = [],
  ) {
    await this.page.fill('text=Question text', text)
    await this.page.fill('text=Question help text', helpText)

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
    await waitForPageJsLoad(this.page)
    await this.page.click('#back-to-list-button')
    await waitForPageJsLoad(this.page)
  }

  private statusFieldsSelector(configuredStatusText: string): string {
    return `fieldset:has-text("Application status: ${configuredStatusText}")`
  }

  private statusNameFieldSelector(configuredStatusText: string): string {
    return `${this.statusFieldsSelector(
      configuredStatusText,
    )} :text("Status name")`
  }

  private statusEmailFieldSelector(configuredStatusText: string): string {
    return `${this.statusFieldsSelector(
      configuredStatusText,
    )} :text("Email content")`
  }
}
