import {Locator, Page} from '@playwright/test'
import {expect} from './civiform_fixtures'
import {waitForHtmxReady, waitForPageJsLoad} from './wait'
import {readFileSync} from 'fs'

export class AdminProgramMigration {
  public page!: Page
  public USE_EXISTING = 'Use the existing question'
  public CREATE_DUPLICATE = 'Create a new duplicate question'
  public OVERWRITE_EXISTING = 'Overwrite all instances of the existing question'

  constructor(page: Page) {
    this.page = page
  }

  async expectExportPage() {
    await expect(
      this.page.getByRole('heading', {name: 'Export a program'}),
    ).toBeVisible()
  }

  async expectJsonPreview() {
    const jsonPreview = this.page.locator('#program-json')

    // The json preview should be a text area and should be disabled to prevent editing
    const tagName = await jsonPreview.evaluate((element) =>
      element.tagName.toLowerCase(),
    )
    expect(tagName).toBe('textarea')
    await expect(jsonPreview).toBeDisabled()

    await expect(
      this.page.getByRole('button', {name: 'Download JSON'}),
    ).toBeVisible()
    await expect(
      this.page.getByRole('button', {name: 'Copy JSON'}),
    ).toBeVisible()

    return jsonPreview.innerHTML()
  }

  async downloadJson() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.getByRole('button', {name: 'Download JSON'}).click(),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  async clickBackButton() {
    const backButton = this.page.getByText('Back to all programs')
    await backButton.click()
    await waitForPageJsLoad(this.page)
  }

  async goToImportPage() {
    await this.page.getByRole('link', {name: 'Import existing program'}).click()
    await waitForPageJsLoad(this.page)
    await this.expectImportPage()
  }

  async expectImportPage() {
    await expect(
      this.page.getByRole('heading', {name: 'Import a program'}),
    ).toBeVisible()
  }

  async submitProgramJson(content: string) {
    await waitForPageJsLoad(this.page)
    await this.page.getByRole('textbox').fill(content)
    await this.clickButtonWithSpinner('Preview program')
  }

  async clickButtonWithSpinner(buttonText: string) {
    // Look up the button by accessible name and get the element ID so
    // that the ID doesn't have to be hard coded.
    const buttonId = await this.page
      .getByRole('button', {name: buttonText})
      .getAttribute('id')

    // There is a race condition with htmx when trying to check if
    // the button is disabled while the network request is being processed.
    //
    // Instead of using Playwright's normal `locator.click()` to trigger
    // the click event do so from within the browser so that the button
    // disabled state can be captured.
    //
    // This has the browser listen for htmx's beforeSend event which
    // occurs directly before the network request is made. This is the point
    // at which the button will be disabled when we capture it. Then later
    // it can be asserted that it was in a disabled state at some point.
    const buttonWasDisabled = await this.page.evaluate((buttonId) => {
      return new Promise<boolean>((resolve) => {
        const button = document.querySelector<HTMLButtonElement>(
          '#' + buttonId,
        )!

        document.body.addEventListener(
          'htmx:beforeSend',
          () => {
            resolve(button.disabled)
          },
          {once: true},
        )

        button.click()
      })
    }, buttonId)

    // Finally here we verify that the element had been in a disabled state
    // at some point.
    expect(buttonWasDisabled).toBeTruthy()

    await waitForHtmxReady(this.page)
    await waitForPageJsLoad(this.page)
  }

  async expectAlert(alertText: string, alertType: string) {
    const alert = this.page.getByRole('alert').filter({hasText: alertText})
    await expect(alert).toBeVisible()
    await expect(alert).toHaveClass(new RegExp(alertType))
    return alert
  }

  async expectProgramImported(programName: string) {
    await expect(
      this.page.getByRole('heading', {name: 'Program name: ' + programName}),
    ).toBeVisible()
  }

  async expectAllQuestionsHaveDuplicateHandlingOption(option: string) {
    for (const question of await this.page
      .locator('.cf-program-question')
      .all()) {
      if (await question.locator('fieldset').isVisible()) {
        await this.expectOptionSelected(question, option)
      }
    }
  }

  async expectOptionSelected(question: Locator, option: string) {
    await expect(question.getByLabel(option)).toBeChecked()
  }

  async clickButton(buttonText: string) {
    await this.page.getByRole('button', {name: buttonText}).click()
    await waitForPageJsLoad(this.page)
  }

  async selectDuplicateHandlingForQuestions(
    questionsToOption: Map<string, string>,
  ) {
    for (const [adminName, option] of questionsToOption) {
      await this.page
        .getByTestId('question-admin-name-' + adminName)
        // Specify only exact matches, since there are alerts that reference similar text
        .getByText(option, {exact: true})
        .click()
    }
  }

  async expectOptionsDisabledForQuestion(
    questionsToOption: Map<string, string>,
  ) {
    for (const [adminName, option] of questionsToOption) {
      await expect(
        this.page
          .getByTestId('question-admin-name-' + adminName)
          .getByText(option, {exact: true}),
      ).toBeDisabled()
    }
  }

  async selectToplevelOverwriteExisting() {
    await this.page
      .getByTestId('toplevel-duplicate-handling')
      .getByText('Overwrite all instances of the existing questions')
      .click()
  }
}
