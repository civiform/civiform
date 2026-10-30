import {test, expect} from '../support/civiform_fixtures'
import {
  ApplicantQuestions,
  AdminPrograms,
  disableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  testUserDisplayName,
  validateAccessibility,
  validateScreenshot,
} from '../support'
import {Page} from '@playwright/test'
import {ProgramCategories, ProgramVisibility} from '../support/admin_programs'
import {BASE_URL} from '../support/config'

test.describe('applicant program index page with images', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(
      page,
      'ADMIN_UI_MIGRATION_J2HTML_TO_THYMELEAF_SC_ENABLED',
    )
  })

  test('shows program with wide image and removes image from card when in My Applications', async ({
    page,
    adminPrograms,
    adminProgramImage,
    applicantQuestions,
  }) => {
    const programName = 'Wide Image Program'
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.legacySetImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await validateScreenshot(page, 'program-image-wide')
    await validateAccessibility(page)

    await test.step('Fill out part of the program application', async () => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickSubmitApplication()
      await applicantQuestions.gotoApplicantHomePage()
    })

    await test.step('Expect the program card to not show the image when in My Applications section', async () => {
      await expect(page.locator('.cf-application-card img')).toBeHidden()
    })
  })

  test('shows program with tall image', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    const programName = 'Tall Image Program'
    await loginAsAdmin(page)
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.legacySetImageFileAndSubmit(
      'src/assets/program-summary-image-tall.png',
    )
    await adminPrograms.publishAllDrafts()
    await logout(page)

    await validateScreenshot(page, 'program-image-tall')
  })

  test('shows program with image and status', async ({
    page,
    adminPrograms,
    adminProgramStatuses,
    adminProgramImage,
    applicantQuestions,
  }) => {
    const programName = 'Image And Status Program'
    await loginAsAdmin(page)

    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.legacySetImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )

    const approvedStatusName = 'Approved'
    await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    await adminProgramStatuses.createStatus(approvedStatusName)
    await adminPrograms.publishProgram(programName)
    await adminPrograms.expectActiveProgram(programName)
    await logout(page)

    await submitApplicationAndApplyStatus(
      page,
      programName,
      approvedStatusName,
      adminPrograms,
      applicantQuestions,
    )

    await loginAsTestUser(page)

    await test.step('verify no available programs info alert appears', async () => {
      await expect(
        page.getByText(
          'You have started or submitted an application for all programs that are available at this time.',
        ),
      ).toBeVisible()
    })

    // Verify program card shows both the Accepted status and image
    await validateScreenshot(page, 'program-image-with-status')
  })

  // This test puts programs with different specs in the different sections of the homepage
  // to verify that different card formats appear correctly next to each other and across sections.
  test('shows programs with and without images in all sections', async ({
    page,
    adminPrograms,
    adminProgramStatuses,
    adminProgramImage,
    adminQuestions,
    applicantQuestions,
  }) => {
    test.slow()

    const programNameInProgressImage = 'In Progress Program [Image]'
    const approvedStatusName = 'Approved'

    await test.step('create program with image as admin', async () => {
      await loginAsAdmin(page)
      const preScreenerFormProgramName = 'Benefits finder'
      await adminPrograms.addPreScreener(
        preScreenerFormProgramName,
        'short program description',
        ProgramVisibility.PUBLIC,
      )

      await adminPrograms.addProgram(programNameInProgressImage)
      await adminQuestions.addTextQuestion({
        questionName: 'first-q',
        questionText: 'first question text',
      })
      await adminPrograms.addProgramBlockUsingSpec(programNameInProgressImage, {
        description: 'first block',
        questions: [{name: 'first-q'}],
      })

      await adminPrograms.goToProgramImagePage(programNameInProgressImage)
      await adminProgramImage.legacySetImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('start application to program', async () => {
      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(programNameInProgressImage)
      await applicantQuestions.answerTextQuestion('first answer')
      await applicantQuestions.clickContinue()
      await applicantQuestions.gotoApplicantHomePage()
      await logout(page)
    })

    await test.step('program with image and status', async () => {
      const programNameSubmittedWithImageAndStatus =
        'Submitted Program [Image and Status]'
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programNameSubmittedWithImageAndStatus)
      await adminPrograms.goToProgramImagePage(
        programNameSubmittedWithImageAndStatus,
      )
      await adminProgramImage.legacySetImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )
      await adminPrograms.gotoDraftProgramManageStatusesPage(
        programNameSubmittedWithImageAndStatus,
      )
      await adminProgramStatuses.createStatus(approvedStatusName)
      await adminPrograms.publishProgram(programNameSubmittedWithImageAndStatus)
      await adminPrograms.expectActiveProgram(
        programNameSubmittedWithImageAndStatus,
      )
      await logout(page)

      await submitApplicationAndApplyStatus(
        page,
        programNameSubmittedWithImageAndStatus,
        approvedStatusName,
        adminPrograms,
        applicantQuestions,
      )
    })

    await test.step('submit basic program', async () => {
      const programNameSubmittedBasic = 'Submitted Program [Basic]'
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programNameSubmittedBasic)
      await adminPrograms.publishProgram(programNameSubmittedBasic)
      await adminPrograms.expectActiveProgram(programNameSubmittedBasic)
      await logout(page)

      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(programNameSubmittedBasic)
      await applicantQuestions.submitFromReviewPage()
      await logout(page)
    })

    await test.step('submit program with status', async () => {
      const programNameSubmittedWithStatus = 'Submitted Program [Status]'
      await loginAsAdmin(page)

      await adminPrograms.addProgram(programNameSubmittedWithStatus)
      await adminPrograms.gotoDraftProgramManageStatusesPage(
        programNameSubmittedWithStatus,
      )
      await adminProgramStatuses.createStatus(approvedStatusName)
      await adminPrograms.publishProgram(programNameSubmittedWithStatus)
      await adminPrograms.expectActiveProgram(programNameSubmittedWithStatus)
      await logout(page)

      await submitApplicationAndApplyStatus(
        page,
        programNameSubmittedWithStatus,
        approvedStatusName,
        adminPrograms,
        applicantQuestions,
      )
    })

    await test.step('submit image on a new row', async () => {
      const programNameSubmittedImage = 'Submitted Program [Image]'
      await loginAsAdmin(page)

      await adminPrograms.addProgram(programNameSubmittedImage)
      await adminPrograms.goToProgramImagePage(programNameSubmittedImage)
      await adminProgramImage.legacySetImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )
      await adminPrograms.publishAllDrafts()
      await logout(page)

      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(programNameSubmittedImage)
      await applicantQuestions.submitFromReviewPage()
      await logout(page)
    })

    await test.step('basic not started program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram('Not Started Program [Basic]')
    })

    await test.step('basic not started program with image', async () => {
      const programNameNotStartedImage = 'Not Started Program [Image]'
      await adminPrograms.addProgram(programNameNotStartedImage)
      await adminPrograms.goToProgramImagePage(programNameNotStartedImage)
      await adminProgramImage.legacySetImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step('verify homepage', async () => {
      await loginAsTestUser(page)
      await validateScreenshot(page, 'program-image-all-types')
      // accessibility fails
      // await validateAccessibility(page)
    })
  })

  test('External program cards', async ({
    page,
    adminPrograms,
    applicantQuestions,
  }) => {
    const externalProgramAName = 'External Program A'
    const externalProgramALink = `${BASE_URL}/programs`
    const externalProgramBName = 'External Program B'
    const externalProgramBLink = `${BASE_URL}/programs#simulated_difference`

    await test.step('add external programs', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addExternalProgram(
        externalProgramAName,
        /* shortDescription= */ 'description',
        externalProgramALink,
        ProgramVisibility.PUBLIC,
      )
      await adminPrograms.addExternalProgram(
        externalProgramBName,
        /* shortDescription= */ 'description',
        externalProgramBLink,
        ProgramVisibility.PUBLIC,
      )
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step("'Programs and Services' section includes cards for the external programs", async () => {
      await applicantQuestions.expectProgramsinCorrectSections(
        {
          expectedProgramsInMyApplicationsSection: [],
          expectedProgramsInProgramsAndServicesSection: [
            externalProgramAName,
            externalProgramBName,
          ],
          expectedProgramsInRecommendedSection: [],
          expectedProgramsInOtherProgramsSection: [],
        },
        /* filtersOn= */ false,
      )

      // Button for external program card has a different text.
      const externalProgramCard = page.locator('.cf-application-card', {
        has: page.getByText(externalProgramAName),
      })
      await expect(
        externalProgramCard.getByRole('button', {
          name: 'View External Program A in new tab',
        }),
      ).toBeVisible()

      await validateAccessibility(page)
    })

    await test.step('card for external program A opens a modal', async () => {
      await applicantQuestions.clickApplyProgramButton(externalProgramAName)

      // Verify external program modal is visible
      const modal = page.getByRole('dialog')
      await expect(
        modal.getByRole('heading', {
          name: 'This will open a different website',
        }),
      ).toBeVisible()
      await expect(
        modal.getByText(
          "To go to the program's website where you can get more details and apply, click Continue",
        ),
      ).toBeVisible()
      const continueButton = modal.getByRole('link', {name: 'Continue'})
      await expect(continueButton).toBeVisible()
      await expect(modal.getByRole('button', {name: 'Go back'})).toBeVisible()
    })

    await test.step("accepting external program A modal redirects to the program's external site", async () => {
      const modal = page.getByRole('dialog')
      const continueButton = modal.getByRole('link', {name: 'Continue'})

      const pagePromise = page.context().waitForEvent('page')
      await continueButton.click()
      const newPage = await pagePromise
      await newPage.waitForLoadState()
      expect(newPage.url()).toMatch(externalProgramALink)

      await newPage.close()
    })

    await test.step('go back to the applicant home page', async () => {
      await page.goto(BASE_URL)
    })

    await test.step('card for external program B opens a modal', async () => {
      await applicantQuestions.clickApplyProgramButton(externalProgramBName)

      // We don't need to check each modal element's visibility since
      // previous step verified them
      const modal = page.getByRole('dialog')
      const continueButton = modal.getByRole('link', {name: 'Continue'})
      await expect(continueButton).toBeVisible()
    })

    await test.step("accepting external program B modal redirects to the program's external site", async () => {
      const modal = page.getByRole('dialog')
      const continueButton = modal.getByRole('link', {name: 'Continue'})

      const pagePromise = page.context().waitForEvent('page')
      await continueButton.click()
      const newPage = await pagePromise
      await newPage.waitForLoadState()
      expect(newPage.url()).toMatch(externalProgramBLink)

      await newPage.close()
    })
  })

  test('External program cards are included in program filters', async ({
    page,
    adminPrograms,
    applicantQuestions,
    seeding,
  }) => {
    const externalProgramName = 'External Program'
    const externalProgramLink = `${BASE_URL}/programs`

    await test.step('seed categories', async () => {
      // The program filtering feature requires seeding the categories. This
      // will add two programs: "Comprehensive Sample Program" and "Minimal
      // Sample Program".
      await seeding.seedProgramsAndCategories()
      await page.goto('/')
    })

    await test.step("add an external program with 'Education' category", async () => {
      await loginAsAdmin(page)
      await adminPrograms.addExternalProgram(
        externalProgramName,
        /* shortDescription= */ 'description',
        externalProgramLink,
        ProgramVisibility.PUBLIC,
      )
      await adminPrograms.selectProgramCategories(
        externalProgramName,
        [ProgramCategories.EDUCATION],
        /* isActive= */ false,
      )

      await adminPrograms.publishAllDrafts()
      await logout(page)
    })

    await test.step("filter programs by 'Education' category", async () => {
      await loginAsTestUser(page)
      await applicantQuestions.filterProgramsByCategory('Education')
    })

    await test.step("external program card appeasrs in the 'Recommended' section", async () => {
      await applicantQuestions.expectProgramsinCorrectSections(
        {
          expectedProgramsInMyApplicationsSection: [],
          expectedProgramsInProgramsAndServicesSection: [],
          expectedProgramsInRecommendedSection: [externalProgramName],
          expectedProgramsInOtherProgramsSection: [
            'Comprehensive Sample Program',
            'Minimal Sample Program',
          ],
        },
        /* filtersOn= */ true,
      )
    })

    await test.step('clicking on external program card opens a modal', async () => {
      await applicantQuestions.clickApplyProgramButton(externalProgramName)

      const modal = page.getByRole('dialog')
      const continueButton = modal.getByRole('link', {name: 'Continue'})
      await expect(continueButton).toBeVisible()
    })

    await test.step("selecting 'go back' closes the modal", async () => {
      const modal = page.getByRole('dialog')
      const goBackButton = modal.getByRole('button', {name: 'Go back'})
      await expect(goBackButton).toBeVisible()
      await goBackButton.click()
      await expect(modal).toBeHidden()
    })

    await test.step('trigger the external program modal again', async () => {
      await applicantQuestions.clickApplyProgramButton(externalProgramName)

      const modal = page.getByRole('dialog')
      const continueButton = modal.getByRole('link', {name: 'Continue'})
      await expect(continueButton).toBeVisible()
    })

    await test.step('clicking outside the modal closes the modal', async () => {
      const modalWrapper = page
        .locator('.usa-modal-wrapper')
        .filter({hasText: 'This will open a different website'})
      const wrapperBox = await modalWrapper.boundingBox()
      // Click in the wrapper top left corner, which should  be outside the
      // actual amodal
      if (wrapperBox) {
        await page.mouse.click(wrapperBox.x, wrapperBox.y)
      }

      const modal = page.getByRole('dialog')
      await expect(modal).toBeHidden()
    })

    await test.step('trigger the external program modal again', async () => {
      await applicantQuestions.clickApplyProgramButton(externalProgramName)

      const modal = page.getByRole('dialog')
      const continueButton = modal.getByRole('link', {name: 'Continue'})
      await expect(continueButton).toBeVisible()
    })

    await test.step("selecting 'continue' redirects to the program's external site", async () => {
      const modal = page.getByRole('dialog')
      const continueButton = modal.getByRole('link', {name: 'Continue'})

      const pagePromise = page.context().waitForEvent('page')
      await continueButton.click()
      const newPage = await pagePromise
      await newPage.waitForLoadState()
      expect(newPage.url()).toMatch(externalProgramLink)

      await newPage.close()
    })
  })

  async function submitApplicationAndApplyStatus(
    page: Page,
    programName: string,
    statusName: string,
    adminPrograms: AdminPrograms,
    applicantQuestions: ApplicantQuestions,
  ) {
    // Submit an application as a test user.
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.submitFromReviewPage()
    await logout(page)

    // Set a status as a program admin
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    const modal = await adminPrograms.setStatusOptionAndAwaitModal(statusName)
    await adminPrograms.confirmStatusUpdateModal(modal)
    await page.getByRole('link', {name: 'Back'}).click()
    await logout(page)
  }
})
