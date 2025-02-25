import {Page, Locator, expect} from '@playwright/test'

/**
 * Class for interacting with the applicant program list page
 * @class
 * @param {Page} page the Playwright page object in which to operate against
 */
export class ApplicantProgramList {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  /**
   * Get the card section locator for the desired application status card section name
   * @param {CardSectionName} cardSectionName to find
   * @returns {Locator} Locator to the card section
   */
  getCardSectionLocator(cardSectionName: CardSectionName): Locator {
    return this.page.getByRole('region', {name: cardSectionName})
  }

  /**
   * Get the locator for program card heading in the desired section
   * @param {CardSectionName} cardSectionName to find
   * @param {String} programName to find
   * @returns {Locator} Locator for program card in the desired section
   */
  getCardLocator(
    cardSectionName: CardSectionName,
    programName: string,
  ): Locator {
    return this.getCardSectionLocator(cardSectionName)
      .getByRole('listitem')
      .filter({
        hasText: programName,
      })
  }

  /**
   * Get the locator for program card heading in the desired section
   * @param {CardSectionName} cardSectionName to find
   * @param {String} programName to find
   * @returns {Locator} Locator for program card in the desired section
   */
  getCardHeadingLocator(
    cardSectionName: CardSectionName,
    programName: string,
  ): Locator {
    return this.getCardSectionLocator(cardSectionName).getByRole('heading', {
      name: programName,
    })
  }

  /**
   * Clicks the apply button for the specified program within the specified card section
   *
   * @async
   * @param {CardSectionName} cardSectionName to find
   * @param {String} programName to apply to
   */
  async clickApplyButton(
    cardSectionName: CardSectionName,
    programName: string,
  ): Promise<void> {
    await this.getCardLocator(cardSectionName, programName)
      .getByRole('link', {
        name: this.getCardSectionApplyButtonText(cardSectionName),
      })
      .click()
  }

  /**
   * @private
   * Get the label text for the specific card section
   *
   * @param {CardSectionName} cardSectionName to find
   * @returns {String} String of the context specific apply button label text
   */
  private getCardSectionApplyButtonText(
    cardSectionName: CardSectionName,
  ): CardSectionApplyButtonText {
    switch (cardSectionName) {
      case CardSectionName.MyApplications:
        return CardSectionApplyButtonText.Edit
      case CardSectionName.ProgramsAndServices:
        return CardSectionApplyButtonText.ViewAndApply
      default:
        throw new Error(`'${String(cardSectionName)}' is not supported.`)
    }
  }

  /**
   * Get the eligibility tag for the desired application status card section name
   * @param {CardSectionName} cardSectionName to find
   * @returns {Locator} Locator to the eligible tag
   */
  getCardEligibleTagLocator(cardSectionName: CardSectionName): Locator {
    return this.getCardSectionLocator(cardSectionName).locator(
      '.cf-eligible-tag',
    )
  }

  /**
   * Get the not eligibility tag for the desired application status card section name
   * @param {CardSectionName} cardSectionName to find
   * @returns {Locator} Locator to the not eligible tag
   */
  getCardNotEligibleTagLocator(cardSectionName: CardSectionName): Locator {
    return this.getCardSectionLocator(cardSectionName).locator(
      '.cf-not-eligible-tag',
    )
  }

  /**
   * Get the submitted tag for the desired application card
   * @param {CardSectionName} cardSectionName to find
   * @param {String} programName to find
   * @returns {Locator} Locator to the eligible tag
   */
  getSubmittedTagLocator(
    cardSectionName: CardSectionName,
    programName: string,
  ): Locator {
    return this.getCardLocator(cardSectionName, programName).locator(
      '.tag-submitted',
    )
  }

  /**
   * Asserts the pressence of the submitted tag for the specified application card
   * @param {CardSectionName} cardSectionName to find
   * @param {String} programName to find
   * @returns {Locator} Locator to the eligible tag
   */
  async expectSubmittedTag(
    cardSectionName: CardSectionName,
    programName: string,
  ) {
    await expect(
      this.getSubmittedTagLocator(cardSectionName, programName),
    ).toContainText('Submitted on')
  }
}

/**
 * @readonly
 * @enum {string}
 * List of heading names used for different groups of cards used in this test
 * suite. This is used in relation to what the user sees on the `/programs` page
 */
export enum CardSectionName {
  MyApplications = 'My applications',
  ProgramsAndServices = 'Programs and services',
}

/**
 * @private
 * @readonly
 * @enum {string}
 * List of button label text used within different groups of cards.
 */
enum CardSectionApplyButtonText {
  Edit = 'Edit',
  ViewAndApply = 'View and apply',
}
