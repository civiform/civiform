import {expect} from './civiform_fixtures'
import {ElementHandle, Page, Locator} from '@playwright/test'
import {readFileSync} from 'fs'
import {
  clickAndWaitForModal,
  dismissModal,
  waitForAnyModal,
  waitForAnyModalLocator,
  waitForPageJsLoad,
  waitForHtmxReady,
} from './wait'
import {BASE_URL, TEST_CIVIC_ENTITY_SHORT_NAME} from './config'
import {AdminProgramStatuses} from './admin_program_statuses'
import {AdminProgramImage} from './admin_program_image'
import {extractEmailsForRecipient} from '.'

/**
 * JSON object representing downloaded application. It can be retrieved by
 * program admins. To see all fields check buildJsonApplication() method in
 * JsonExporter.java.
 */
export interface DownloadedApplication {
  program_name: string
  program_version_id: number
  applicant_id: number
  application_id: number
  language: string
  create_time: string
  submitter_email: string
  submit_time: string
  // Applicant answers as a map of question name to answer data.
  application: {
    [questionName: string]: {
      [questionField: string]: unknown
    }
  }
}

/**
 * List of fields in the program form. This list is not exhaustive, as fields
 * are added when needed by a test.
 */
export enum FormField {
  APPLICATION_STEPS,
  CONFIRMATION_MESSAGE,
  LONG_DESCRIPTION,
  NOTIFICATION_PREFERENCES,
  PROGRAM_CATEGORIES,
  PROGRAM_ELIGIBILITY,
  PROGRAM_EXTERNAL_LINK,
}

export enum ProgramType {
  DEFAULT = 'CiviForm program',
  PRE_SCREENER = 'Pre-screener',
  EXTERNAL = 'External program',
}

export enum ProgramVisibility {
  HIDDEN = 'Hide from applicants.',
  PUBLIC = 'Publicly visible',
  TI_ONLY = 'Trusted intermediaries only',
  SELECT_TI = 'Visible to selected trusted intermediaries only',
  DISABLED = 'Disabled',
}

export enum Eligibility {
  IS_GATING = 'Only allow residents to submit applications if they meet all eligibility requirements',
  IS_NOT_GATING = "Allow residents to submit applications even if they don't meet eligibility requirements",
}

export enum PredicateType {
  ELIGIBILITY = 'eligibility',
  VISIBILITY = 'visibility',
}

export enum ProgramCategories {
  CHILDCARE = 'Childcare',
  ECONOMIC = 'Economic',
  EDUCATION = 'Education',
  EMPLOYMENT = 'Employment',
  FOOD = 'Food',
  GENERAL = 'General',
  HEALTHCARE = 'Healthcare',
  HOUSING = 'Housing',
  INTERNET = 'Internet',
  TRAINING = 'Training',
  TRANSPORTATION = 'Transportation',
  UTILITIES = 'Utilities',
}

export enum ProgramLifecycle {
  DRAFT = 'Draft',
  ACTIVE = 'Active',
}

export enum ProgramAction {
  EDIT = 'Edit',
  PUBLISH = 'Publish',
  SHARE = 'Share link',
  VIEW = 'View',
  VIEW_APPLICATIONS = 'Applications',
}

export enum ProgramExtraAction {
  VIEW_APPLICATIONS = 'Applications',
  EDIT = 'Edit',
  EXPORT = 'Export program',
  MANAGE_ADMINS = 'Manage program admins',
  MANAGE_APPLICATIONS = 'Manage application statuses',
  MANAGE_TRANSLATIONS = 'Manage translations',
}

/**
 * List of buttons that are displayed in the program information header. This
 * list is not exhaustive, as fields are added when needed by a test.
 */
export enum ProgramHeaderButton {
  PREVIEW_AS_APPLICANT = 'Preview as applicant',
  DOWNLOAD_PDF_PREVIEW = 'Download PDF preview',
}

export enum NotificationPreference {
  EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS = 'Send Program Admins an email notification every time an application is submitted',
}

/**
 * Our support classes try do excessive amounts of navigation in an attempt
 * to aid the test writer. This make trying to do a simple action on a page
 * often return to the list page and drill back into the expected location
 * prior to performing the simple action. This adds excessive time to a
 * test.
 *
 * This enum is to allow for a targeted way to skip some of that navigation
 * if you are already on the page where you expect to be without fully rewriting
 * a lot of code or duplicating existing function which will add extra confusion
 * on which method to call.
 *
 * Functions that use this should always set the default value to {@link DEFAULT} so that
 * the existing functionality is maintained.
 */
export enum NavigationOption {
  /**
   * Existing behavior. Support methods go out of there way to make sure they are on
   * the exact part of the page before executing the action. This may add extra
   * navigation steps.
   */
  PERFORM_DETAILED_NAVIGATION,
  /**
   * Support methods assume you are already where you expect to be and immediately perform
   * the action
   */
  SKIP_EXCESSIVE_NAVIGATION,
  /**
   * Default option points to {@link PERFORM_DETAILED_NAVIGATION}, the way the old code
   * works. This will allow changing the default at a later point if needed.
   */
  DEFAULT = PERFORM_DETAILED_NAVIGATION,
}

export interface QuestionSpec {
  name: string
  isOptional?: boolean
}

export interface BlockSpec {
  name?: string
  description?: string
  questions?: QuestionSpec[]
}

export function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/ /g, '-')
    .replace(/[^a-zA-Z0-9-]/g, '')
}
function findApplicantId(str: string): string {
  return str.replace(/\D/g, '')
}

export class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  /**
   * @param isProgramDisabled If true, go to the disabled programs page rather than the main programs page.
   */
  async gotoAdminProgramsPage(isProgramDisabled = false) {
    await this.page.click('nav :text("Programs")')
    if (isProgramDisabled) {
      await this.page.click('a:has-text("Disabled")')
    }
    await this.expectAdminProgramsPage()
    await waitForPageJsLoad(this.page)
  }

  async expectAdminProgramsPage() {
    await expect(
      this.page.getByRole('heading', {name: 'Program dashboard'}),
    ).toBeVisible()
    await expect(
      this.page.getByRole('heading', {
        name:
          'Create, edit and publish programs in ' +
          TEST_CIVIC_ENTITY_SHORT_NAME,
      }),
    ).toBeVisible()
  }

  async expectProgramExist(programName: string, description: string) {
    await this.gotoAdminProgramsPage()
    const tableInnerText = await this.page.innerText('main')

    expect(tableInnerText).toContain(programName)
    expect(tableInnerText).toContain(description)
  }

  async expectApplicationHasStatusString(
    applicant: string,
    statusString: string,
  ) {
    await expect(this.getRowLocator(applicant)).toContainText(`${statusString}`)
  }

  async expectApplicationStatusDoesntContain(
    applicant: string,
    statusString: string,
  ) {
    await expect(this.getRowLocator(applicant)).not.toContainText(statusString)
  }

  /**
   * Creates a disabled program with given name.
   */
  async addDisabledProgram(programName: string) {
    await this.addProgram(programName, {
      description: 'program description',
      shortDescription: 'short program description',
      visibility: ProgramVisibility.DISABLED,
    })
  }

  async getApplicationId() {
    const htmlElement = await this.page
      .locator('.cf-application-id')
      .innerText()
    return findApplicantId(htmlElement)
  }

  /**
   * Creates a pre-screener with the given parameters
   *
   * @param {boolean} programName - Name of the program
   * @param {string} shortDescription - Short description of the program
   * @param {ProgramVisibility} programVisibility - Visibility of the program
   */
  async addPreScreener(
    programName: string,
    shortDescription: string,
    programVisibility: ProgramVisibility,
  ) {
    // Only add values for fields that are required. Disabled
    // fields must have an empty or undefined value, since disabled elements
    // are readonly and cannot be edited
    return this.addProgram(programName, {
      description: '',
      shortDescription,
      visibility: programVisibility,
      adminDescription: '',
      programType: ProgramType.PRE_SCREENER,
      confirmationMessage: '',
      applicationSteps: [],
    })
  }

  /**
   * Creates an external program with the given parameters
   *
   * @param {string} programName - Name of the program
   * @param {string} shortDescription - Short description of the program
   * @param {string} externalLink - Link to the external program
   * @param {ProgramVisibility} programVisibility - Visibility of the program
   */
  async addExternalProgram(
    programName: string,
    shortDescription: string,
    externalLink: string,
    programVisibility: ProgramVisibility,
  ) {
    // Only add values for fields that are required. Disabled fields must not
    // have an empty or undefined value, since disabled elements are readonly
    // and cannot be edited
    return this.addProgram(programName, {
      description: '',
      shortDescription,
      externalLink,
      visibility: programVisibility,
      adminDescription: '',
      programType: ProgramType.EXTERNAL,
      confirmationMessage: '',
      applicationSteps: [],
    })
  }

  /**
   * Creates a program with given name. Optional fields check for value
   * existence before adding it to the form. Disabled fields must not receive a
   * value, since disabled elements are readonly and cannot be edited
   *
   * @param {boolean} submitNewProgram - If true, the new program will be submitted
   * to the database and then the admin will be redirected to the next page in the
   * program creation flow. If false, the new program information will be filled in
   * but *not* submitted to the database and the current page will still be the
   * program creation page.
   */
  async addProgram(
    programName: string,
    {
      description = 'program description',
      shortDescription = 'short program description',
      externalLink = null,
      visibility = ProgramVisibility.PUBLIC,
      adminDescription = 'admin description',
      programType = ProgramType.DEFAULT,
      selectedTI = 'none',
      confirmationMessage = 'This is the _custom confirmation message_ with markdown\n' +
        '[This is a link](https://www.example.com)\n' +
        'This is a list:\n' +
        '* Item 1\n' +
        '* Item 2\n' +
        '\n' +
        'There are some empty lines below this that should be preserved\n' +
        '\n' +
        '\n' +
        'This link should be autodetected: https://www.example.com\n',
      eligibility = undefined,
      submitNewProgram = true,
      applicationSteps = [{title: 'title', description: 'description'}],
    }: {
      description?: string
      shortDescription?: string
      externalLink?: string | null
      visibility?: ProgramVisibility
      adminDescription?: string
      programType?: ProgramType
      selectedTI?: string
      confirmationMessage?: string
      eligibility?: Eligibility
      submitNewProgram?: boolean
      applicationSteps?: {title: string; description: string}[]
    } = {},
  ) {
    await this.gotoAdminProgramsPage()
    await this.page.click('#new-program-button')
    await waitForPageJsLoad(this.page)

    // program slug must be in url-compatible form so we slugify the program name
    await this.page.fill('#program-slug', slugify(programName))
    await this.page.fill('#program-description-textarea', adminDescription)
    await this.page.fill('#program-display-name-input', programName)
    await this.page.fill(
      '#program-display-short-description-input',
      shortDescription,
    )

    // Program type selector varies with the EXTERNAL_PROGRAM_CARDS feature.
    // When enabled, form has program type options. Otherwise, form has a
    // pre-screener checkbox.
    // IMPORTANT: Select the program type first since some of the next fields
    // are disabled based on the program type.
    const hasProgramTypeOptions = await this.page
      .getByTestId('program-type-options')
      .isVisible()
    if (hasProgramTypeOptions) {
      await this.selectProgramType(programType)
    } else if (programType === ProgramType.PRE_SCREENER) {
      await this.clickPreScreenerFormToggle()
    }

    if (eligibility) {
      await this.chooseEligibility(eligibility)
    }

    await this.page.check(`label:has-text("${visibility}")`)
    if (visibility == ProgramVisibility.SELECT_TI) {
      await this.page.check(`label:has-text("${selectedTI}")`)
    }

    // The external link field is disabled for default programs and pre-screeners,
    // so only fill it if a value is provided.
    if (externalLink !== null) {
      await this.page.fill('#program-external-link-input', externalLink)
    }

    if (description.length > 0) {
      await this.page.fill('#program-display-description-textarea', description)
    }

    if (confirmationMessage.length) {
      await this.page.fill(
        '#program-confirmation-message-textarea',
        confirmationMessage,
      )
    }

    if (programType === ProgramType.DEFAULT) {
      for (let i = 0; i < applicationSteps.length; i++) {
        const indexPlusOne = i + 1
        await this.page
          .getByRole('textbox', {name: `Step ${indexPlusOne} title`})
          .fill(applicationSteps[i].title)
        await this.page
          .getByRole('textbox', {name: `Step ${indexPlusOne} description`})
          .fill(applicationSteps[i].description)
      }
    }

    if (submitNewProgram) {
      await this.submitProgramDetailsEdits()
    }
  }

  /**
     * Verifies whether the given form field is disabled.
     *
     * @param formField - The specific form field type to verify (from FormField enum)

    * @throws Will throw an error if the elements' states don't match the expected disabled state
    * @throws Will throw an error if an invalid or unsupported form field type is provided
    */
  async expectFormFieldDisabled(formField: FormField) {
    switch (formField) {
      case FormField.APPLICATION_STEPS: {
        for (let i = 0; i < 5; i++) {
          const indexPlusOne = i + 1
          const stepTitle = this.page.getByRole('textbox', {
            name: `Step ${indexPlusOne} title`,
          })
          const stepDescription = this.page.getByRole('textbox', {
            name: `Step ${indexPlusOne} description`,
          })
          await expect(stepTitle).toBeDisabled()
          expect(await stepTitle.getAttribute('readonly')).not.toBeNull()
          await expect(stepDescription).toBeDisabled()
          expect(await stepDescription.getAttribute('readonly')).not.toBeNull()
          if (indexPlusOne == 1) {
            const titleRequiredIndicator =
              this.getRequiredIndicatorFor('apply-step-1-title')
            const descriptionRequiredIndicator = this.getRequiredIndicatorFor(
              'apply-step-1-description',
            )
            await expect(titleRequiredIndicator).toBeHidden()
            await expect(descriptionRequiredIndicator).toBeHidden()
          }
        }
        break
      }

      case FormField.CONFIRMATION_MESSAGE: {
        const confirmationMessage = this.getConfirmationMessageField()
        await expect(confirmationMessage).toBeDisabled()
        expect(
          await confirmationMessage.getAttribute('readonly'),
        ).not.toBeNull()
        break
      }

      case FormField.LONG_DESCRIPTION: {
        const longDescription = this.getLongDescriptionField()
        await expect(longDescription).toBeDisabled()
        expect(await longDescription.getAttribute('readonly')).not.toBeNull()
        break
      }

      case FormField.NOTIFICATION_PREFERENCES: {
        const notificationPreferences =
          this.getNotificationsPreferenceCheckbox()
        await expect(notificationPreferences).toBeDisabled()
        await expect(notificationPreferences).not.toBeChecked()
        break
      }

      case FormField.PROGRAM_CATEGORIES: {
        for (const categoryName of Object.values(ProgramCategories)) {
          const category = this.page.getByRole('checkbox', {
            name: categoryName,
          })
          await expect(category).toBeDisabled()
          await expect(category).not.toBeChecked()
        }
        break
      }

      case FormField.PROGRAM_ELIGIBILITY: {
        for (const eligibilityName of Object.values(Eligibility)) {
          const option = this.page.getByRole('radio', {
            name: eligibilityName,
          })
          await expect(option).toBeDisabled()
          await expect(option).not.toBeChecked()
        }
        break
      }

      case FormField.PROGRAM_EXTERNAL_LINK: {
        const externalLink = this.getExternalLinkField()
        await expect(externalLink).toBeDisabled()
        expect(await externalLink.getAttribute('readonly')).not.toBeNull()
        const requiredIndicator = this.getRequiredIndicatorFor(
          'program-external-link-input',
        )
        await expect(requiredIndicator).toBeHidden()
        break
      }

      default:
        throw new Error(
          `Unsupported form field type: ${String(formField)}. Please add handling for this field type.`,
        )
    }
  }

  /**
   * Verifies whether the given form field is enabled.
   *
   * @param formField - The specific form field type to verify (from FormField enum)
   * @param programType - Optional program type to specify context (from ProgramType enum).
   *                      Not all form fields require a program type for verification.
   *
   * @throws Will throw an error if the elements' states don't match the expected enabled state
   * @throws Will throw an error if an invalid or unsupported form field type is provided
   */
  async expectFormFieldEnabled(
    formField: FormField,
    programType?: ProgramType,
  ) {
    switch (formField) {
      case FormField.APPLICATION_STEPS: {
        for (let i = 0; i < 5; i++) {
          const indexPlusOne = i + 1
          const stepTitle = this.page.getByRole('textbox', {
            name: `Step ${indexPlusOne} title`,
          })
          const stepDescription = this.page.getByRole('textbox', {
            name: `Step ${indexPlusOne} description`,
          })
          await expect(stepTitle).toBeEnabled()
          expect(await stepTitle.getAttribute('readonly')).toBeNull()
          await expect(stepDescription).toBeEnabled()
          expect(await stepDescription.getAttribute('readonly')).toBeNull()
          if (indexPlusOne == 1) {
            const titleRequiredIndicator =
              this.getRequiredIndicatorFor('apply-step-1-title')
            const descriptionRequiredIndicator = this.getRequiredIndicatorFor(
              'apply-step-1-description',
            )
            await expect(titleRequiredIndicator).toBeVisible()
            await expect(descriptionRequiredIndicator).toBeVisible()
          }
        }
        break
      }

      case FormField.CONFIRMATION_MESSAGE: {
        const confirmationMessage = this.getConfirmationMessageField()
        await expect(confirmationMessage).toBeEnabled()
        expect(await confirmationMessage.getAttribute('readonly')).toBeNull()
        break
      }

      case FormField.LONG_DESCRIPTION: {
        const longDescription = this.getLongDescriptionField()
        await expect(longDescription).toBeEnabled()
        expect(await longDescription.getAttribute('readonly')).toBeNull()
        break
      }

      case FormField.NOTIFICATION_PREFERENCES: {
        const notificationPreferences =
          this.getNotificationsPreferenceCheckbox()
        await expect(notificationPreferences).toBeEnabled()
        break
      }

      case FormField.PROGRAM_CATEGORIES: {
        for (const categoryName of Object.values(ProgramCategories)) {
          const category = this.page.getByRole('checkbox', {
            name: categoryName,
          })
          await expect(category).toBeEnabled()
        }
        break
      }

      case FormField.PROGRAM_ELIGIBILITY: {
        for (const eligibilityName of Object.values(Eligibility)) {
          const option = this.page.getByRole('radio', {
            name: eligibilityName,
          })
          await expect(option).toBeEnabled()
        }
        break
      }

      case FormField.PROGRAM_EXTERNAL_LINK: {
        const externalLink = this.getExternalLinkField()
        await expect(externalLink).toBeEnabled()
        expect(await externalLink.getAttribute('readonly')).toBeNull()
        // The external link field is only required for external programs,
        // so only check for the required indicator for that program type.
        if (programType && programType === ProgramType.EXTERNAL) {
          const requiredIndicator = this.getRequiredIndicatorFor(
            'program-external-link-input',
          )
          await expect(requiredIndicator).toBeVisible()
        }
        break
      }

      default:
        throw new Error(
          `Unsupported form field type: ${String(formField)}. Please add handling for this field type.`,
        )
    }
  }

  /**
   * Verifies whether the program type has its corresponding field disabled.
   *
   * @param formField - The specific program type field to verify (from ProgramType enum)
   */
  async expectProgramTypeDisabled(programType: ProgramType) {
    const programTypeOption = this.getProgramTypeOption(programType)
    await expect(programTypeOption).toBeDisabled()
  }

  /**
   * Verifies whether the program type has its corresponding field enabled.
   *
   * @param formField - The specific program type field to verify (from ProgramType enum)
   */
  async expectProgramTypeEnabled(programType: ProgramType) {
    const programTypeOption = this.getProgramTypeOption(programType)
    await expect(programTypeOption).toBeEnabled()
  }

  /**
   * Verifies a program card has the given actions visible
   *
   * @param programName - Name of the program
   * @param lifecycle - Lifecycle of the program
   * @param actions - Actions that should be visible on the card
   * @param extraActions - Extra actions that should be visible on the extra
   * actions dropdown
   */
  async expectProgramActionsVisible(
    programName: string,
    lifecycle: ProgramLifecycle,
    actions: ProgramAction[],
    extraActions: ProgramExtraAction[],
  ) {
    for (const action of actions) {
      const actionButton = this.getProgramAction(programName, lifecycle, action)
      await expect(actionButton).toBeVisible()
    }

    if (extraActions.length === 0) {
      return
    }

    await this.getProgramExtraActionsButton(programName, lifecycle).click()
    for (const action of extraActions) {
      const actionButton = this.getProgramExtraAction(
        programName,
        lifecycle,
        action,
      )
      await expect(actionButton).toBeVisible()
    }
  }

  /**
   * Verifies a program card has the given actions hidden
   *
   * @param programName - Name of the program
   * @param lifecycle - Lifecycle of the program
   * @param actions - Actions that should be hidden on the card
   * @param extraActions - Extra actions that should be hidden on the extra
   * actions dropdown
   */
  async expectProgramActionsHidden(
    programName: string,
    lifecycle: ProgramLifecycle,
    actions: ProgramAction[],
    extraActions: ProgramExtraAction[],
  ) {
    for (const action of actions) {
      const actionButton = this.getProgramAction(programName, lifecycle, action)
      await expect(actionButton).toBeHidden()
    }

    if (extraActions.length === 0) {
      return
    }

    await this.getProgramExtraActionsButton(programName, lifecycle).click()
    for (const action of extraActions) {
      const actionButton = this.getProgramExtraAction(
        programName,
        lifecycle,
        action,
      )
      await expect(actionButton).toBeHidden()
    }
  }

  /**
   * Verifies whether the program header button is hidden
   *
   * @param headerButton - The specific button to verify (from ProgramHeaderButton enum)
   */
  async expectProgramHeaderButtonHidden(headerButton: ProgramHeaderButton) {
    const button = this.page.getByRole('button', {name: headerButton})
    await expect(button).toBeHidden()
  }

  /**
   * Verifies whether the block panel in the program block view is hidden
   */
  async expectBlockPanelHidden() {
    const blockPanel = this.page.getByTestId('block-panel')
    await expect(blockPanel).toBeHidden()
  }

  async submitProgramDetailsEdits() {
    await this.page.click('#program-update-button')
    await waitForPageJsLoad(this.page)
  }

  async expectProgramDetailsSaveAndContinueButton() {
    await expect(
      this.page.getByRole('button', {name: 'Save and continue to next step'}),
    ).toBeVisible()
  }

  async editProgram(
    programName: string,
    visibility = ProgramVisibility.PUBLIC,
    selectedTI = 'none',
  ) {
    await this.gotoAdminProgramsPage()
    await this.page.click('button :text("View")')
    await this.page.click('#header_edit_button')
    await this.page.click('#header_edit_button')
    await waitForPageJsLoad(this.page)

    await this.page.check(`label:has-text("${visibility}")`)
    if (visibility == ProgramVisibility.SELECT_TI) {
      await this.page.check(`label:has-text("${selectedTI}")`)
    }

    await this.submitProgramDetailsEdits()
  }

  async programNames(disabled = false) {
    await this.gotoAdminProgramsPage(disabled)
    const titles = this.page.locator('.cf-admin-program-card .cf-program-title')
    return titles.allTextContents()
  }

  /**
   * Expects a specific program block to be selected inside the read only view
   * that is used to view the configuration of an active program.
   */
  async expectReadOnlyProgramBlock(blockId: string) {
    // The block info shows us we are viewing a block.
    expect(this.page.locator('id=block-info-display-' + blockId)).not.toBeNull()
    // The absence of one of the edit buttons ensures it is the read only view.
    expect(
      await this.page.locator('id=block-description-modal-button').count(),
    ).toEqual(0)
  }

  /**
   * Expects a question card with a specified text label in it.
   */
  async expectQuestionCardWithLabel(questionName: string, label: string) {
    expect(
      await this.page
        .locator(
          this.withinQuestionCardSelectorInProgramView(
            questionName,
            `p:has-text("${label}")`,
          ),
        )
        .count(),
    ).toBe(1)
  }

  /**
   * Expects a question card either with or without a universal question badge.
   */
  async expectQuestionCardUniversalBadgeState(
    questionName: string,
    universal: boolean,
  ) {
    expect(
      await this.page
        .locator(
          this.withinQuestionCardSelectorInProgramView(
            questionName,
            '.cf-universal-badge',
          ),
        )
        .count(),
    ).toBe(universal ? 1 : 0)
  }

  // Question card within a program edit or read only page
  questionCardSelectorInProgramView(questionName: string) {
    return `.cf-program-question:has(:text("Admin ID: ${questionName}"))`
  }

  // Question card within a program edit page
  withinQuestionCardSelectorInProgramView(
    questionName: string,
    selector: string,
  ) {
    return this.questionCardSelectorInProgramView(questionName) + ' ' + selector
  }

  /**
   * Selects a button in the extra rows dropdown for a program
   *
   * @param programName - Name of the program
   * @param lifecycle - Lifecycle of the program
   * @param extraRow - Extra row to select
   */
  async selectProgramExtraAction(
    programName: string,
    lifecycle: ProgramLifecycle,
    extraRow: ProgramExtraAction,
  ) {
    await this.getProgramExtraActionsButton(programName, lifecycle).click()
    await this.getProgramExtraAction(programName, lifecycle, extraRow).click()
  }

  async gotoDraftProgramManageStatusesPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.selectProgramExtraAction(
      programName,
      ProgramLifecycle.DRAFT,
      ProgramExtraAction.MANAGE_APPLICATIONS,
    )

    await waitForPageJsLoad(this.page)
    const adminProgramStatuses = new AdminProgramStatuses(this.page)
    await adminProgramStatuses.expectProgramManageStatusesPage(programName)
  }

  async gotoDraftProgramManageTranslationsPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.selectProgramExtraAction(
      programName,
      ProgramLifecycle.DRAFT,
      ProgramExtraAction.MANAGE_TRANSLATIONS,
    )
    await waitForPageJsLoad(this.page)
    await this.expectProgramManageTranslationsPage(programName)
  }

  async gotoActiveProgramManageTranslationsPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectActiveProgram(programName)
    await this.selectProgramExtraAction(
      programName,
      ProgramLifecycle.ACTIVE,
      ProgramExtraAction.MANAGE_TRANSLATIONS,
    )
    await waitForPageJsLoad(this.page)
    await this.expectProgramManageTranslationsPage(programName)
  }

  async goToProgramImagePage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.gotoEditDraftProgramPage(programName)
    await this.page.click('button:has-text("Edit program image")')
    await this.expectProgramImagePage()
  }

  /**
   * Opens the manage program page by clicking on a program's card extra action.
   * Admin must be a CiviForm admin, otherwise the extra action won't be visible
   *
   * @param programName - Name of the program
   */
  async gotoManageProgramAdminsPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)

    await this.selectProgramExtraAction(
      programName,
      ProgramLifecycle.DRAFT,
      ProgramExtraAction.MANAGE_ADMINS,
    )
    await waitForPageJsLoad(this.page)
    await this.expectManageProgramAdminsPage()
  }

  /**
   * Opens the export program page by clicking on a program's card extra action.
   * Admin must be a CiviForm admin, otherwise the extra action won't be visible
   *
   * @param programName - Name of the program
   */
  async goToExportProgramPage(
    programName: string,
    lifecycle: ProgramLifecycle,
  ) {
    await this.gotoAdminProgramsPage()
    await this.selectProgramExtraAction(
      programName,
      lifecycle,
      ProgramExtraAction.EXPORT,
    )
    await waitForPageJsLoad(this.page)
  }

  async setProgramEligibility(programName: string, eligibility: Eligibility) {
    await this.goToProgramDescriptionPage(programName)
    await this.chooseEligibility(eligibility)
    await this.submitProgramDetailsEdits()
  }

  async chooseEligibility(eligibility: Eligibility) {
    await this.page.check(`label:has-text("${eligibility}")`)
  }

  getEligibilityIsGatingInput() {
    return this.page.locator(`label:has-text("${Eligibility.IS_GATING}")`)
  }

  getEligibilityIsNotGatingInput() {
    return this.page.locator(`label:has-text("${Eligibility.IS_NOT_GATING}")`)
  }

  async expectEmailNotificationPreferenceIsChecked(isChecked: boolean) {
    await expect(
      this.page.getByRole('checkbox', {
        name: NotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS,
      }),
    ).toBeChecked({checked: isChecked})
  }

  async setEmailNotificationPreferenceCheckbox(checked: boolean) {
    const checkbox = this.page.getByRole('checkbox', {
      name: NotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS,
    })
    const isCurrentlyChecked = await checkbox.isChecked()

    if (isCurrentlyChecked !== checked) {
      // Note: We click on the label instead of directly interacting with the checkbox
      // because USWDS styling hides the actual checkbox input and styles the label to
      // look like a checkbox. The actual input element is visually hidden or positioned
      // off-screen, making it inaccessible to Playwright's direct interactions.
      await this.page
        .locator('label[for="notification-preferences-email"]')
        .click()
    }
  }

  async expectLoginOnlyProgramIsChecked(isChecked: boolean) {
    await expect(
      this.page.getByRole('checkbox', {
        name: 'Require applicants to log in to apply to this program',
      }),
    ).toBeChecked({checked: isChecked})
  }

  async setProgramToLoginOnly(checked: boolean) {
    const checkbox = this.page.getByRole('checkbox', {
      name: 'Require applicants to log in to apply to this program',
    })
    const isCurrentlyChecked = await checkbox.isChecked()

    if (isCurrentlyChecked !== checked) {
      // Note: We click on the label instead of directly interacting with the checkbox
      // because USWDS styling hides the actual checkbox input and styles the label to
      // look like a checkbox. The actual input element is visually hidden or positioned
      // off-screen, making it inaccessible to Playwright's direct interactions.
      await this.page.locator('label[for="login-only-applications"]').click()
    }
  }

  /**
   * Opens the export program page by clicking on a program's card action or
   * extra action (depending on the program lifecycle)
   * Admin must be a CiviForm admin, otherwise the extra action won't be visible
   *
   * @param programName - Name of the program
   */
  async gotoEditDraftProgramPage(
    programName: string,
    isProgramDisabled: boolean = false,
    lifecycle: ProgramLifecycle = ProgramLifecycle.DRAFT,
  ) {
    await this.gotoAdminProgramsPage(isProgramDisabled)

    if (lifecycle === ProgramLifecycle.ACTIVE) {
      await this.expectActiveProgram(programName)
      await this.selectProgramExtraAction(
        programName,
        lifecycle,
        ProgramExtraAction.EDIT,
      )
    } else {
      await this.expectDraftProgram(programName)
      await this.getProgramAction(
        programName,
        lifecycle,
        ProgramAction.EDIT,
      ).click()
    }

    await waitForPageJsLoad(this.page)
    await this.expectProgramBlockEditPage(programName)
  }

  /**
   * Opens the view program page by clicking on a program's card action
   * Admin must be a CiviForm admin, otherwise the extra action won't be visible
   *
   * @param programName - Name of the program
   */
  async gotoViewActiveProgramPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectActiveProgram(programName)
    await this.getProgramAction(
      programName,
      ProgramLifecycle.ACTIVE,
      ProgramAction.VIEW,
    ).click()
    await waitForPageJsLoad(this.page)
    await this.expectProgramBlockReadOnlyPage(programName)
  }

  async gotoViewActiveProgramPageAndStartEditing(programName: string) {
    await this.gotoViewActiveProgramPage(programName)
    await this.page.click('button:has-text("Edit")')
    await waitForPageJsLoad(this.page)
  }

  async goToBlockInProgram(programName: string, blockName: string) {
    await this.gotoEditDraftProgramPage(programName)
    // Click on the block to edit
    await this.page.click(`a:has-text("${blockName}")`)
    await waitForPageJsLoad(this.page)
  }

  async gotoToBlockInReadOnlyProgram(blockId: string) {
    await this.page.click('#block_list_item_' + blockId)
    await waitForPageJsLoad(this.page)
  }

  async goToEditBlockVisibilityPredicatePage(
    programName: string,
    blockName: string,
    expandedFormLogicEnabled: boolean = false,
  ) {
    await this.goToBlockInProgram(programName, blockName)

    // Click on the edit predicate button
    await this.page.click('#cf-edit-visibility-predicate')
    await waitForPageJsLoad(this.page)
    if (expandedFormLogicEnabled) {
      await this.expectEditPredicatePage(PredicateType.VISIBILITY)
    } else {
      await this.expectEditVisibilityPredicatePage(blockName)
    }
  }

  async goToEditBlockEligibilityPredicatePage(
    programName: string,
    blockName: string,
    expandedFormLogicEnabled: boolean = false,
  ) {
    await this.goToBlockInProgram(programName, blockName)

    // Click on the edit predicate button
    await this.page.click('#cf-edit-eligibility-predicate')
    await waitForPageJsLoad(this.page)
    await waitForHtmxReady(this.page)
    if (expandedFormLogicEnabled) {
      await this.expectEditPredicatePage(PredicateType.ELIGIBILITY)
    } else {
      await this.expectEditEligibilityPredicatePage(blockName)
    }
  }

  async goToProgramDescriptionPage(
    programName: string,
    lifecycle: ProgramLifecycle = ProgramLifecycle.DRAFT,
  ) {
    await this.gotoEditDraftProgramPage(
      programName,
      /* isProgramDisabled= */ false,
      lifecycle,
    )
    await this.page.click('button:has-text("Edit program details")')
    await waitForPageJsLoad(this.page)
  }

  async expectDraftProgram(programName: string) {
    await expect(
      this.getProgramCard(programName, ProgramLifecycle.DRAFT),
    ).toBeVisible()
  }

  async expectDoesNotHaveDraftProgram(programName: string) {
    await expect(
      this.getProgramCard(programName, ProgramLifecycle.DRAFT),
    ).toBeHidden()
  }

  async expectActiveProgram(programName: string) {
    await expect(
      this.getProgramCard(programName, ProgramLifecycle.ACTIVE),
    ).toBeVisible()
  }

  async expectProgramEditPage(programName = '') {
    expect(await this.page.innerText('h1')).toContain(
      `Edit program: ${programName}`,
    )
  }

  async expectProgramManageTranslationsPage(programName: string) {
    expect(await this.page.innerText('h1')).toContain(
      `Manage program translations: ${programName}`,
    )
  }

  async expectProgramImagePage() {
    const adminProgramImage = new AdminProgramImage(this.page)
    await adminProgramImage.expectProgramImagePage()
  }

  async expectManageProgramAdminsPage() {
    expect(await this.page.innerText('h1')).toContain(
      'Manage admins for program',
    )
  }

  async expectProgramSettingsPage() {
    expect(await this.page.innerText('h1')).toContain('settings')
  }

  async expectAddProgramAdminErrorToast() {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain(
      "as a Program Admin because they haven't previously logged into" +
        ' CiviForm. Have the user log in, then add them as a Program Admin. After' +
        " they've been added, they will need refresh their browser see the programs" +
        " they've been assigned to.",
    )
    expect(toastMessages).toContain('Error: ')
  }

  async expectDisplayNameErrorToast() {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain(
      'A public display name for the program is required.',
    )
    expect(toastMessages).toContain('Error: ')
  }

  async expectEditVisibilityPredicatePage(blockName: string) {
    expect(await this.page.innerText('h1')).toContain(
      'Visibility condition for ' + blockName,
    )
  }

  async expectEditEligibilityPredicatePage(blockName: string) {
    expect(await this.page.innerText('h1')).toContain(
      'Eligibility condition for ' + blockName,
    )
  }

  async expectEditPredicatePage(predicateType: PredicateType) {
    await expect(
      this.page.getByText('Edit ' + predicateType + ' conditions'),
    ).toBeVisible()
  }

  async expectSuccessToast(successToastMessage: string) {
    const toastContainer = await this.page.innerHTML('#toast-container')

    expect(toastContainer).toContain('bg-cf-toast-success')
    expect(toastContainer).toContain(successToastMessage)
  }

  async expectProgramBlockEditPage(programName = '') {
    expect(await this.page.innerText('id=program-title')).toContain(programName)
    expect(await this.page.innerText('id=block-edit-form')).not.toBeNull()
    expect(await this.page.innerText('h1')).toContain('Add a question')
  }

  async expectProgramBlockReadOnlyPage(programName = '') {
    expect(await this.page.innerText('id=program-title')).toContain(programName)
    // The only element for editing should be one top level button
    await expect(this.page.locator('#header_edit_button')).toBeVisible()
    expect(await this.page.locator('id=block-edit-form').count()).toEqual(0)
  }

  // Removes questions from given block in program.
  async removeQuestionFromProgram(
    programName: string,
    blockName: string,
    questionNames: string[] = [],
    navigationOption: NavigationOption = NavigationOption.DEFAULT,
  ) {
    if (navigationOption == NavigationOption.PERFORM_DETAILED_NAVIGATION) {
      await this.goToBlockInProgram(programName, blockName)
    }

    for (const questionName of questionNames) {
      await this.page.click(
        this.withinQuestionCardSelectorInProgramView(
          questionName,
          'button:has-text("Delete")',
        ),
      )
    }
  }

  // Edit a question from the program screen
  async editQuestion(questionName: string) {
    await this.page.click(
      this.withinQuestionCardSelectorInProgramView(
        questionName,
        'a:has-text("Edit")',
      ),
    )
  }

  /**
   * Edit basic block details and required questions
   * @deprecated prefer using {@link #editProgramBlockUsingSpec} instead.
   */
  async editProgramBlock(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[] = [],
  ) {
    await this.editProgramBlockUsingSpec(programName, {
      description: blockDescription,
      questions: questionNames.map((questionName) => {
        return {
          name: questionName,
        }
      }),
    })
  }

  /**
   * Edit basic block details and required and optional questions. Cannot handle more than one optional question.
   * @deprecated prefer using {@link #editProgramBlockUsingSpec} instead. Be aware that
   * editProgramBlockWithOptional always puts the optional question first, whereas
   * editProgramBlockUsingSpec orders questions according to the question array.
   */
  async editProgramBlockWithOptional(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[],
    optionalQuestionName: string,
  ) {
    const optionalQuestion: QuestionSpec = {
      name: optionalQuestionName,
      isOptional: true,
    }
    const nonOptionalQuestions: QuestionSpec[] = questionNames.map(
      (questionName) => ({name: questionName}),
    )

    await this.editProgramBlockUsingSpec(programName, {
      description: blockDescription,
      questions: [optionalQuestion].concat(nonOptionalQuestions),
    })
  }

  /**
   * Edit basic block details and questions
   * @param programName Name of the program to edit
   * @param block Block information
   */
  async editProgramBlockUsingSpec(programName: string, block: BlockSpec) {
    await this.gotoEditDraftProgramPage(programName)

    await clickAndWaitForModal(this.page, 'block-description-modal')

    if (block.name !== undefined) {
      await this.page.fill('#block-name-input', block.name)
    }

    await this.page.fill('textarea', block.description || 'screen description')

    await this.page.click('#update-block-button:not([disabled])')

    for (const question of block.questions ?? []) {
      await this.addQuestionFromQuestionBank(question.name)

      if (question.isOptional) {
        await this.page
          .getByTestId(`question-admin-name-${question.name}`)
          .locator(':is(button:has-text("optional"))')
          .click()
      }
    }
  }

  /**
   * Add questions to specified block. You must already be on the admin program edit block page,
   * but if you are this is a significantly faster way to add multiple questions since it does
   * not return to the program list page each time and navigate back to the block.
   * @param {BlockSpec} block Block information
   */
  async addQuestionsToProgramBlock(block: BlockSpec) {
    await this.page.click(`a:has-text("${block.name}")`)
    await waitForPageJsLoad(this.page)

    for (const question of block.questions || []) {
      await this.addQuestionFromQuestionBank(question.name)

      if (question.isOptional) {
        await this.page
          .getByTestId(`question-admin-name-${question.name}`)
          .locator(':is(button:has-text("optional"))')
          .click()
      }
    }
  }

  async launchRemoveProgramBlockModal(programName: string, blockName: string) {
    await this.goToBlockInProgram(programName, blockName)
    await clickAndWaitForModal(this.page, 'block-delete-modal')
  }

  async removeProgramBlock(programName: string, blockName: string) {
    await this.launchRemoveProgramBlockModal(programName, blockName)
    await this.page.click('#delete-block-button')
    await waitForPageJsLoad(this.page)
    await this.gotoAdminProgramsPage()
  }

  async removeCurrentBlock() {
    await clickAndWaitForModal(this.page, 'block-delete-modal')
    await this.page.click('#delete-block-button')
    await waitForPageJsLoad(this.page)
  }

  private async waitForQuestionBankAnimationToFinish() {
    // Animation is 150ms. Give some extra overhead to avoid flakiness on slow CPU.
    // This is currently called over 300 times which adds up.
    // https://tailwindcss.com/docs/transition-property

    // eslint-disable-next-line playwright/no-wait-for-timeout
    await this.page.waitForTimeout(250)
  }

  async openQuestionBank() {
    await this.page.click('button:has-text("Add a question")')
    await this.waitForQuestionBankAnimationToFinish()
  }

  async closeQuestionBank() {
    await this.page.click('button.cf-close-question-bank-button')
    await this.waitForQuestionBankAnimationToFinish()
  }

  async addQuestionFromQuestionBank(questionName: string) {
    await this.openQuestionBank()
    await this.page
      .locator(
        `.cf-question-bank-element[data-adminname="${questionName}"] button:has-text("Add")`,
      )
      .click()
    await waitForPageJsLoad(this.page)
    // After question was added question bank is still open. Close it first.
    await this.closeQuestionBank()
    // Make sure the question is successfully added to the screen.
    await expect(
      this.page.locator(
        `div.cf-program-question p:has-text("Admin ID: ${questionName}")`,
      ),
    ).toBeVisible()
  }

  async questionBankNames(universal = false): Promise<string[]> {
    const loc = '.cf-question-bank-element:visible .cf-question-title'
    const titles = this.page.locator(
      universal
        ? '#question-bank-universal ' + loc
        : '#question-bank-nonuniversal ' + loc,
    )
    return titles.allTextContents()
  }

  /**
   * Creates a new program block with the given questions, all marked as required.
   *
   * @deprecated prefer using {@link #addProgramBlockUsingSpec} instead.
   */
  async addProgramBlock(
    programName: string,
    blockDescription = 'screen description',
    questionNames: string[] = [],
  ) {
    return await this.addProgramBlockUsingSpec(programName, {
      description: blockDescription,
      questions: questionNames.map((questionName) => ({name: questionName})),
    })
  }

  /**
   * Creates a new program block as defined by {@link BlockSpec}.
   *
   * Prefer this method over {@link #addProgramBlock}.
   *
   * @param {string} programName Name of the program
   * @param {BlockSpec} block Desired block settings
   * @param {boolean} isProgramDisabled Defaults to false. Flag to determine if the program status is disabled or not
   * @param {boolean} editBlockScreenDetails Defaults to true. If true the block name and description will be updated; if false they will not.
   */
  async addProgramBlockUsingSpec(
    programName: string,
    block: BlockSpec,
    isProgramDisabled: boolean = false,
    editBlockScreenDetails: boolean = true,
  ) {
    await this.gotoEditDraftProgramPage(programName, isProgramDisabled)
    return await this.addProgramBlockUsingSpecWhenAlreadyOnEditDraftPage(
      block,
      editBlockScreenDetails,
    )
  }

  /**
   * Creates a new program block as defined by {@link BlockSpec}.
   * You must already be on the admin program edit block page, but if you are this is a significantly
   * faster way to add multiple questions since it does not return to the program list page each time and navigate back to the block.
   * @param {BlockSpec} block Desired block settings
   * @param {boolean} editBlockScreenDetails Defaults to true. If true the block name and description will be updated; if false they will not.
   */
  async addProgramBlockUsingSpecWhenAlreadyOnEditDraftPage(
    block: BlockSpec,
    editBlockScreenDetails: boolean = true,
  ) {
    await this.page.click('#add-block-button')
    await waitForPageJsLoad(this.page)

    if (editBlockScreenDetails) {
      await clickAndWaitForModal(this.page, 'block-description-modal')

      // Only update the block name if a name was provided. Otherwise, keep the default (which should be something like "Block 1")
      if (block.name !== undefined) {
        await this.page.fill('#block-name-input', block.name)
      }

      await this.page.fill(
        'textarea',
        block.description || 'screen description',
      )
      await this.page.click('#update-block-button:not([disabled])')
      // Wait for submit and redirect back to this page.
      await this.page.waitForURL(this.page.url())
      await waitForPageJsLoad(this.page)
    }

    for (const question of block.questions ?? []) {
      await this.addQuestionFromQuestionBank(question.name)
      if (question.isOptional) {
        const optionalToggle = this.page
          .locator(this.questionCardSelectorInProgramView(question.name))
          .getByRole('button', {name: 'optional'})
        await optionalToggle.click()
      }
    }

    return await this.page.locator('#block-name-input').inputValue()
  }

  async clickEditBridgeDefinitionButton() {
    await this.page
      .getByRole('button', {name: 'Edit Bridge Definition'})
      .click()
    await waitForPageJsLoad(this.page)
  }

  async addProgramRepeatedBlock(
    programName: string,
    enumeratorBlockName: string,
    blockDescription = 'screen description',
    questionNames: string[] = [],
  ) {
    await this.gotoEditDraftProgramPage(programName)

    await this.page.click(`text=${enumeratorBlockName}`)
    await waitForPageJsLoad(this.page)
    await this.page.click('#create-repeated-block-button')
    await waitForPageJsLoad(this.page)

    await clickAndWaitForModal(this.page, 'block-description-modal')
    await this.page.fill('#block-description-textarea', blockDescription)
    await this.page.click('#update-block-button:not([disabled])')

    for (const questionName of questionNames) {
      await this.addQuestionFromQuestionBank(questionName)
    }
  }

  async publishProgram(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.expectDraftProgram(programName)
    await this.publishAllDrafts()
    await this.expectActiveProgram(programName)
  }

  private static PUBLISH_ALL_MODAL_TITLE =
    'Do you want to publish all draft programs?'

  publishAllProgramsModalLocator() {
    return this.page.locator(
      `.cf-modal:has-text("${AdminPrograms.PUBLISH_ALL_MODAL_TITLE}")`,
    )
  }

  async publishAllDrafts() {
    await this.gotoAdminProgramsPage()
    const modal = await this.openPublishAllDraftsModal()
    const confirmHandle = (await modal.$(
      'button:has-text("Publish all draft programs and questions")',
    ))!
    await confirmHandle.click()

    await waitForPageJsLoad(this.page)
  }

  async openPublishAllDraftsModal() {
    await this.page.click('button:has-text("Publish all drafts")')
    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain(
      AdminPrograms.PUBLISH_ALL_MODAL_TITLE,
    )
    return modal
  }

  async expectProgramReferencesModalContains({
    expectedQuestionsContents,
    expectedProgramsContents,
  }: {
    expectedQuestionsContents: string[]
    expectedProgramsContents: string[]
  }) {
    const modal = await this.openPublishAllDraftsModal()

    const editedQuestions = await modal.$$(
      '.cf-admin-publish-references-question li',
    )
    const editedQuestionsContents = await Promise.all(
      editedQuestions.map((editedQuestion) => editedQuestion.innerText()),
    )
    expect(editedQuestionsContents).toEqual(expectedQuestionsContents)

    const editedPrograms = await modal.$$(
      '.cf-admin-publish-references-program li',
    )
    const editedProgramsContents = await Promise.all(
      editedPrograms.map((editedProgram) => editedProgram.innerText()),
    )
    expect(editedProgramsContents).toEqual(expectedProgramsContents)

    await dismissModal(this.page)
  }

  async createNewVersion(programName: string, isProgramDisabled = false) {
    await this.gotoAdminProgramsPage(isProgramDisabled)
    await this.expectActiveProgram(programName)

    await this.selectProgramExtraAction(
      programName,
      ProgramLifecycle.ACTIVE,
      ProgramExtraAction.EDIT,
    )
    await waitForPageJsLoad(this.page)

    await this.page.click('button:has-text("Edit program details")')
    await waitForPageJsLoad(this.page)

    await this.submitProgramDetailsEdits()
    await this.gotoAdminProgramsPage(isProgramDisabled)
    await this.expectDraftProgram(programName)
  }

  /**
   * Opens the applications page by clicking on a program's card action.
   * Opens the extra actions dropdown if present so the action is visible.
   *
   * @param programName - Name of the program
   */
  async viewApplications(programName: string) {
    // Navigate back to the main page for the program admin.
    await this.page.goto(BASE_URL)
    await waitForPageJsLoad(this.page)

    await this.expectActiveProgram(programName)
    const extraActions = this.getProgramExtraActionsButton(
      programName,
      ProgramLifecycle.ACTIVE,
    )
    if (await extraActions.isVisible()) {
      await extraActions.click()
    }
    await this.getProgramAction(
      programName,
      ProgramLifecycle.ACTIVE,
      ProgramAction.VIEW_APPLICATIONS,
    ).click()
    await waitForPageJsLoad(this.page)
  }

  async expectApplicationCount(expectedCount: number) {
    await expect(this.page.locator('.cf-admin-application-row')).toHaveCount(
      expectedCount,
    )
  }

  getRowLocator(applicantName: string): Locator {
    return this.page.locator(
      `.cf-admin-application-row:has-text("${applicantName}")`,
    )
  }

  selectQuestionWithinBlock(question: string) {
    return `.cf-program-question:has-text("${question}")`
  }

  selectWithinQuestionWithinBlock(question: string, selector: string) {
    return this.selectQuestionWithinBlock(question) + ' ' + selector
  }

  public static readonly ANY_STATUS_APPLICATION_FILTER_OPTION =
    'Any application status'
  public static readonly NO_STATUS_APPLICATION_FILTER_OPTION =
    'Only applications without a status'

  async filterProgramApplications({
    fromDate = '',
    untilDate = '',
    searchFragment = '',
    applicationStatusOption = '',
    clickFilterButton = true,
  }: {
    fromDate?: string
    untilDate?: string
    searchFragment?: string
    applicationStatusOption?: string
    clickFilterButton?: boolean
  }) {
    await this.page.getByRole('textbox', {name: 'from'}).fill(fromDate)
    await this.page.getByRole('textbox', {name: 'until'}).fill(untilDate)
    await this.page.fill('input[name="search"]', searchFragment)
    if (applicationStatusOption) {
      await this.page.selectOption('label:has-text("Application status")', {
        label: applicationStatusOption,
      })
    }

    if (clickFilterButton) {
      await this.page.click('button:has-text("Filter")')
    }

    await waitForPageJsLoad(this.page)
  }

  async clearFilterProgramApplications() {
    await this.page.click('a:has-text("Clear")')
    await waitForPageJsLoad(this.page)
  }

  selectApplicationBlock(blockName: string) {
    return `.cf-admin-application-block-card:has-text("${blockName}")`
  }

  selectWithinApplicationBlock(blockName: string, selector: string) {
    return this.selectApplicationBlock(blockName) + ' ' + selector
  }

  async viewApplicationForApplicant(applicantName: string) {
    await this.page.getByRole('link', {name: applicantName}).click()
    await waitForPageJsLoad(this.page)
  }

  async expectApplicationAnswers(
    blockName: string,
    questionName: string,
    answer: string,
  ) {
    const blockText = await this.page
      .locator(this.selectApplicationBlock(blockName))
      .innerText()

    expect(blockText).toContain(questionName)
    expect(blockText).toContain(answer)
  }

  async expectApplicationAnswerLinks(blockName: string, questionName: string) {
    await expect(
      this.page.locator(this.selectApplicationBlock(blockName)),
    ).toContainText(questionName)

    expect(
      this.page
        .locator(this.selectWithinApplicationBlock(blockName, 'a'))
        .getAttribute('href'),
    ).not.toBeNull()
  }

  async isStatusSelectorVisible(): Promise<boolean> {
    return this.page.locator(this.statusSelector()).isVisible()
  }

  async getStatusOption(): Promise<string> {
    return this.page.locator(this.statusSelector()).inputValue()
  }

  async expectStatusSelection(status: string) {
    await expect(this.page.locator(this.statusSelector())).toHaveValue(status)
  }

  /**
   * Selects the provided status option and then awaits the confirmation dialog.
   */
  async setStatusOptionAndAwaitModal(status: string): Promise<Locator> {
    await this.page.locator(this.statusSelector()).selectOption(status)
    return waitForAnyModalLocator(this.page)
  }
  /**
   * Clicks the confirm button in the status update confirmation dialog
   */
  async confirmStatusUpdateModal(modal: Locator) {
    const confirmButton = modal.getByText('Confirm')
    await confirmButton.click()
    await waitForPageJsLoad(this.page)
  }

  async expectUpdateStatusToast() {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain('Application status updated')
  }

  private statusSelector() {
    return '.cf-program-admin-status-selector label:has-text("Status:")'
  }

  async isEditNoteVisible(): Promise<boolean> {
    return this.page.locator(this.editNoteSelector()).isVisible()
  }

  /**
   * Returns the content of the note modal when viewing an application.
   */
  async getNoteContent() {
    await this.page.locator(this.editNoteSelector()).click()

    const editModal = await waitForAnyModal(this.page)
    const noteContentArea = (await editModal.$('textarea'))!
    return noteContentArea.inputValue()
  }

  /**
   * Clicks the edit note button, and returns the modal.
   */
  async awaitEditNoteModal(): Promise<ElementHandle<HTMLElement>> {
    await this.page.locator(this.editNoteSelector()).click()

    return await waitForAnyModal(this.page)
  }

  /**
   * Clicks the edit note button, sets the note content to the provided text,
   * and confirms the dialog.
   */
  async editNote(noteContent: string) {
    const editModal = await this.awaitEditNoteModal()
    const noteContentArea = (await editModal.$('textarea'))!
    await noteContentArea.fill(noteContent)

    const saveButton = (await editModal.$('text=Save'))!
    await saveButton.click()
    await waitForPageJsLoad(this.page)
  }

  private editNoteSelector() {
    return 'button:has-text("Edit note")'
  }

  async expectNoteUpdatedToast() {
    await expect(this.page.locator('#toast-container')).toContainText(
      'Application note updated',
    )
  }

  async getJson(applyFilters: boolean): Promise<DownloadedApplication[]> {
    await this.page.getByRole('button', {name: 'Download'}).click()
    await waitForAnyModal(this.page)

    if (applyFilters) {
      await this.page.check('text="Current results"')
    } else {
      await this.page.check('text="All data"')
    }
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download JSON"'),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }

    return JSON.parse(readFileSync(path, 'utf8')) as DownloadedApplication[]
  }

  async getApplicationPdf() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.locator('button:has-text("Export to PDF")').click(),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  async getProgramPdf() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.getByRole('button', {name: 'Download PDF preview'}).click(),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  async getCsv(applyFilters: boolean) {
    await this.page.getByRole('button', {name: 'Download'}).click()
    await waitForAnyModal(this.page)

    if (applyFilters) {
      await this.page.check('text="Current results"')
    } else {
      await this.page.check('text="All data"')
    }
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download CSV"'),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  async getDemographicsCsv() {
    await clickAndWaitForModal(this.page, 'download-demographics-csv-modal')
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(
        '#download-demographics-csv-modal button:has-text("Download demographic data (CSV)")',
      ),
    ])
    await dismissModal(this.page)
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  /*
   * Creates a program, ads the specified questions to it and publishes it.
   * To use this method, questions must have been previously created for example by using one of the helper methods in admin_questions.ts.
   * <BR>Example:
   * <BR> adminQuestions.addAddressQuestion({questionName: 'address-q'})
   */
  async addAndPublishProgramWithQuestions(
    questionNames: string[],
    programName: string,
  ) {
    await this.addProgram(programName)
    await this.editProgramBlock(programName, 'dummy description', questionNames)
    await this.publishProgram(programName)
  }

  getAddressCorrectionToggle() {
    return this.page.locator('input[name=addressCorrectionEnabled]')
  }

  getAddressCorrectionToggleByName(questionName: string) {
    return this.page
      .locator('.cf-program-question')
      .filter({hasText: questionName})
      .locator('input[name=addressCorrectionEnabled]')
  }

  getAddressCorrectionHelpTextByName(questionName: string) {
    return this.page
      .locator('.cf-program-question')
      .filter({hasText: questionName})
      .locator(
        ':is(span:has-text("Enabling \'address correction\' will check"))',
      )
  }
  async clickAddressCorrectionToggle() {
    const responsePromise = this.page.waitForResponse((response) => {
      // The setAddressCorrectionEnabled action either redirects to the edit page or returns an error, in which case the response url remains the same.
      return response.url().includes('edit')
    })
    await this.page.click(':is(button:has-text("Address correction"))')
    await responsePromise
    await this.page.waitForLoadState()
  }

  async clickAddressCorrectionToggleByName(questionName: string) {
    const toggleLocator = this.getAddressCorrectionToggleByName(questionName)
    const responsePromise = this.page.waitForResponse((response) => {
      // The setAddressCorrectionEnabled action either redirects to the edit page or returns an error, in which case the response url remains the same.
      return response.url().includes('edit')
    })

    await toggleLocator
      .locator('..')
      .locator('button:has-text("Address correction")')
      .click()
    await responsePromise
    await this.page.waitForLoadState()
  }

  getPreScreenerFormToggle() {
    return this.page.getByRole('checkbox', {
      name: 'Set program as pre-screener',
    })
  }

  getProgramTypeOption(programType: string): Locator {
    return this.page.getByRole('radio', {
      name: programType,
    })
  }

  getExternalLinkField(): Locator {
    return this.page.getByRole('textbox', {
      name: 'Link to program website',
    })
  }

  getRequiredIndicatorFor(labelId: string): Locator {
    return this.page.locator(`label[for="${labelId}"] span.usa-hint--required`)
  }

  getLongDescriptionField(): Locator {
    return this.page.getByRole('textbox', {
      name: 'Long program description',
    })
  }

  getNotificationsPreferenceCheckbox(): Locator {
    return this.page.getByRole('checkbox', {
      name:
        'Send Program Admins an email notification every time an ' +
        'application is submitted',
    })
  }

  getConfirmationMessageField(): Locator {
    return this.page.getByRole('textbox', {
      name:
        'A custom message that will be shown on the confirmation page ' +
        'after an application has been submitted. You can use this ' +
        'message to explain next steps of the application process and/or ' +
        'highlight other programs to apply for.',
    })
  }

  getProgramCard(programName: string, lifecycle: string): Locator {
    return this.page
      .locator('div.cf-admin-program-card')
      .filter({has: this.page.getByText(programName, {exact: true})})
      .filter({
        has: this.page.locator(
          `[data-lifecycle-stage=${lifecycle.toLowerCase()}]`,
        ),
      })
  }

  getProgramAction(
    programName: string,
    lifecycle: string,
    action: ProgramAction,
  ) {
    const programCard = this.getProgramCard(programName, lifecycle)
    return programCard.getByRole('button', {
      name: action,
    })
  }

  getProgramExtraActionsButton(
    programName: string,
    lifecycle: ProgramLifecycle,
  ): Locator {
    const programCard = this.getProgramCard(programName, lifecycle)
    return programCard
      .locator(`[data-lifecycle-stage=${lifecycle.toLowerCase()}]`)
      .locator('.cf-with-dropdown')
  }

  getProgramExtraAction(
    programName: string,
    lifecycle: ProgramLifecycle,
    action: ProgramExtraAction,
  ): Locator {
    const programCard = this.getProgramCard(programName, lifecycle)
    return programCard.getByRole('button', {
      name: action,
    })
  }

  /**
   * Verifies that the program type radio button checked correspond to
   * `programTypeSelected`. This should only be used when EXTERNAL_PROGRAM_CARDS
   * feature is enabled.
   */
  async expectProgramTypeSelected(programTypeSelected: ProgramType) {
    for (const programType of Object.values(ProgramType)) {
      const programTypeOption = this.getProgramTypeOption(programType)

      if (programType === programTypeSelected) {
        await expect(programTypeOption).toBeChecked()
      } else {
        await expect(programTypeOption).not.toBeChecked()
      }
    }
  }

  /**
   * Selects the program type radio button for `programType`. This should only
   * be used when EXTERNAL_PROGRAM_CARDS feature is enabled.
   */
  async selectProgramType(programType: ProgramType) {
    // Note: We click on the label instead of directly interacting with the button
    // because USWDS styling hides the actual button input and styles the label to
    // look like a checkbox. The actual input element is visually hidden or positioned
    // off-screen, making it inaccessible to Playwright's direct interactions.
    let programId
    switch (programType) {
      case ProgramType.DEFAULT:
        programId = 'default-program-option'
        break
      case ProgramType.PRE_SCREENER:
        programId = 'pre-screener-program-option'
        break
      case ProgramType.EXTERNAL:
        programId = 'external-program-option'
        break
    }
    await this.page.locator('label[for="' + programId + '"]').click()
  }

  // TODO(#10363): Migrate callers to use selectProgramType(programType) once
  // EXTERNAL_PROGRAM_CARDS is enabled by default.
  async clickPreScreenerFormToggle() {
    // Note: We click on the label instead of directly interacting with the checkbox
    // because USWDS styling hides the actual checkbox input and styles the label to
    // look like a checkbox. The actual input element is visually hidden or positioned
    // off-screen, making it inaccessible to Playwright's direct interactions.
    await this.page.locator('label[for="pre-screener-checkbox"]').click()
  }

  /**
   * Selects categories for a program.
   *
   * @param programName - Name of the program
   * @param categories - Categories to select for the program
   * @param isActive - Whether the program is on active mode
   */
  async selectProgramCategories(
    programName: string,
    categories: ProgramCategories[],
    isActive: boolean,
  ) {
    if (isActive) {
      await this.gotoViewActiveProgramPageAndStartEditing(programName)
    } else {
      await this.gotoEditDraftProgramPage(programName)
    }

    await this.page.getByRole('button', {name: 'Edit program details'}).click()
    for (const category of categories) {
      await this.page.getByText(category).click()
    }
    await this.submitProgramDetailsEdits()
  }

  async isPaginationVisibleForApplicationTable(): Promise<boolean> {
    const applicationListDiv = this.page.getByTestId('application-table')
    return applicationListDiv.locator('.usa-pagination').isVisible()
  }

  async expectEmailSent(
    numEmailsBefore: number,
    userEmail: string,
    emailBody: string,
    programName: string,
  ) {
    const emailsAfter = await extractEmailsForRecipient(this.page, userEmail)
    expect(emailsAfter.length).toEqual(numEmailsBefore + 1)
    const sentEmail = emailsAfter[emailsAfter.length - 1]
    expect(sentEmail.Subject).toEqual(
      `[Test Message] An update on your application ${programName}`,
    )
    expect(sentEmail.Body.text_part).toContain(emailBody)
  }
}
