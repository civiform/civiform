import {Page, Locator} from '@playwright/test'

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
   * Get the card group locator for the desired application status card group name
   * @param {ApplicationStatusCardGroupName} applicationStatusCardGroupName to find
   * @returns {Locator} Locator to the card group
   */
  getCardGroupLocator(
    applicationStatusCardGroupName: ApplicationStatusCardGroupName,
  ): Locator {
    return this.page.getByRole('region', {name: applicationStatusCardGroupName})
  }

  /**
   * Get the locator for program card heading in the desired group
   * @param {ApplicationStatusCardGroupName} applicationStatusCardGroupName to find
   * @param {String} programName to find
   * @returns {Locator} Locator for program card in the desired group
   */
  getCardLocator(
    applicationStatusCardGroupName: ApplicationStatusCardGroupName,
    programName: string,
  ): Locator {
    return this.getCardGroupLocator(applicationStatusCardGroupName).getByRole(
      'listitem',
      {name: programName},
    )
  }

  /**
   * Get the locator for program card heading in the desired group
   * @param {ApplicationStatusCardGroupName} applicationStatusCardGroupName to find
   * @param {String} programName to find
   * @returns {Locator} Locator for program card in the desired group
   */
  getCardHeadingLocator(
    applicationStatusCardGroupName: ApplicationStatusCardGroupName,
    programName: string,
  ): Locator {
    return this.getCardGroupLocator(applicationStatusCardGroupName).getByRole(
      'heading',
      {name: programName},
    )
  }

  /**
   * Clicks the apply button for the specified program within the specified card group
   *
   * @async
   * @param {ApplicationStatusCardGroupName} applicationStatusCardGroupName to find
   * @param {String} programName to apply to
   */
  async clickApplyButton(
    applicationStatusCardGroupName: ApplicationStatusCardGroupName,
    programName: string,
  ): Promise<void> {
    await this.getCardLocator(applicationStatusCardGroupName, programName)
      .getByRole('link', {
        name: this.getCardGroupApplyButtonText(applicationStatusCardGroupName),
      })
      .click()
  }

  /**
   * @private
   * Get the label text for the specific card group
   *
   * @param {ApplicationStatusCardGroupName} applicationStatusCardGroupName to find
   * @returns {String} String of the context specific apply button label text
   */
  private getCardGroupApplyButtonText(
    applicationStatusCardGroupName: ApplicationStatusCardGroupName,
  ): ApplicationStatusApplyButtonText {
    return applicationStatusCardGroupName ==
      ApplicationStatusCardGroupName.MyApplications
      ? ApplicationStatusApplyButtonText.Edit
      : ApplicationStatusApplyButtonText.ViewAndApply
  }

  /**
   * Get the eligibility tag for the desired application status card group name
   * @param {ApplicationStatusCardGroupName} applicationStatusCardGroupName to find
   * @returns {Locator} Locator to the eligible tag
   */
  getCardEligibleTagLocator(
    applicationStatusCardGroupName: ApplicationStatusCardGroupName,
  ): Locator {
    return this.getCardGroupLocator(applicationStatusCardGroupName).locator(
      '.cf-eligible-tag',
    )
  }

  /**
   * Get the not eligibility tag for the desired application status card group name
   * @param {ApplicationStatusCardGroupName} applicationStatusCardGroupName to find
   * @returns {Locator} Locator to the not eligible tag
   */
  getCardNotEligibleTagLocator(
    applicationStatusCardGroupName: ApplicationStatusCardGroupName,
  ): Locator {
    return this.getCardGroupLocator(applicationStatusCardGroupName).locator(
      '.cf-not-eligible-tag',
    )
  }
}

/**
 * @readonly
 * @enum {string}
 * List of heading names used for different groups of application cards used in this test
 * suite. This is used in relation to what the user sees on the `/programs` page
 */
export enum ApplicationStatusCardGroupName {
  MyApplications = 'My applications',
  ProgramsAndServices = 'Programs and services',
}

/**
 * @private
 * @readonly
 * @enum {string}
 * List of button label text used within different groups of application cards.
 */
enum ApplicationStatusApplyButtonText {
  Edit = 'Edit',
  ViewAndApply = 'View and apply',
}
