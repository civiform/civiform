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
    blockName,
    blockDescription,
    statuses = [],
    shortDescription = 'short desc',
    applicationStepTitle = 'step one title',
    applicationStepDescription = 'step one description',
    blockEligibilityMsg = '',
    programType = 'default',
    northStar = false,
  }: {
    name: string
    description: string
    blockName: string
    blockDescription: string
    statuses: ProgramStatusTranslationParams[]
    shortDescription: string
    applicationStepTitle: string
    applicationStepDescription: string
    blockEligibilityMsg?: string
    programType: string
    northStar: boolean
  }) {
    await this.page.fill('text=Program name', name)

    if (programType === 'default' || !northStar) {
      await this.page.fill('text=Program description', description)
    }
    await this.page.fill('text=Short program description', shortDescription)

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

    if (programType === 'default') {
      await this.page.fill(
        'text=Application step 1 title',
        applicationStepTitle,
      )
      await this.page.fill(
        'text=Application step 1 description',
        applicationStepDescription,
      )
    }

    await this.page.fill('text=Screen name', blockName)
    await this.page.fill('text=Screen description', blockDescription)
    if (blockEligibilityMsg != '') {
      await this.page.fill(
        'text=Screen eligibility message',
        blockEligibilityMsg,
      )
    }

    await this.page.click('#update-localizations-button')
    await waitForPageJsLoad(this.page)
  }

  async expectProgramTranslation({
    expectProgramName,
    expectProgramDescription,
    expectProgramShortDescription = 'short description',
    expectApplicationStepTitle = 'step one title',
    expectApplicationStepDescription = 'step one description',
    programType = 'default',
    northStar = false,
  }: {
    expectProgramName: string
    expectProgramDescription: string
    expectProgramShortDescription: string
    expectApplicationStepTitle: string
    expectApplicationStepDescription: string
    programType: string
    northStar: boolean
  }) {
    const programNameValue = this.page.getByLabel('Program name')
    await expect(programNameValue).toHaveValue(expectProgramName)

    if (programType === 'default' || !northStar) {
      const programDescriptionValue = this.page.getByLabel(
        'Program description',
        {exact: true},
      )
      await expect(programDescriptionValue).toHaveValue(
        expectProgramDescription,
      )
    }

    const programShortDescriptionValue = this.page.getByLabel(
      'Short program description',
    )
    await expect(programShortDescriptionValue).toHaveValue(
      expectProgramShortDescription,
    )

    if (programType === 'default') {
      const applicationStepTitleValue = this.page.getByLabel(
        'Application step 1 title',
      )
      await expect(applicationStepTitleValue).toHaveValue(
        expectApplicationStepTitle,
      )
      const applicationStepDescriptionValue = this.page.getByLabel(
        'Application step 1 description',
      )
      await expect(applicationStepDescriptionValue).toHaveValue(
        expectApplicationStepDescription,
      )
    }
  }

  async expectNoProgramStatusTranslations() {
    // Fix me! ESLint: playwright/prefer-web-first-assertions
    expect(await this.page.isVisible(':has-text("Application status: ")')).toBe(
      false,
    )
  }

  async expectNoApplicationSteps() {
    await expect(this.page.getByLabel('Application step 1 title')).toBeHidden()
    await expect(
      this.page.getByLabel('Application step 1 description'),
    ).toBeHidden()
  }

  async expectNoLongDescription() {
    await expect(
      this.page.getByLabel('Program description', {exact: true}),
    ).toBeHidden()
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
    const statusTextValue = this.page.locator(
      this.statusNameFieldSelector(configuredStatusText),
    )
    await expect(statusTextValue).toHaveValue(expectStatusText)

    const statusEmailValue = this.page.locator(
      this.statusEmailFieldSelector(configuredStatusText),
    )
    await expect(statusEmailValue).toHaveValue(expectStatusEmail)
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
      await this.page.getByLabel('Program image description').count(),
    ).toEqual(0)
  }

  async expectProgramImageDescriptionTranslation(
    expectImageDescription: string,
  ) {
    const imageDescriptionValue = this.page.getByLabel(
      'Program image description',
    )
    await expect(imageDescriptionValue).toHaveValue(expectImageDescription)
  }

  async expectBlockTranslations(
    blockName: string,
    blockDescription: string,
    blockEligibilityMsg?: string,
  ) {
    const blockNameValue = this.page.getByLabel('Screen name')
    await expect(blockNameValue).toHaveValue(blockName)
    const blockDescriptionValue = this.page.getByLabel('Screen description')
    await expect(blockDescriptionValue).toHaveValue(blockDescription)
    if (blockEligibilityMsg != undefined) {
      const blockEligibilityMsgValue = this.page.getByLabel(
        'Screen eligibility message',
      )
      await expect(blockEligibilityMsgValue).toHaveValue(blockEligibilityMsg)
    }
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
