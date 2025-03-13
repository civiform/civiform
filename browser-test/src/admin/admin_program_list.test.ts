import {test, expect} from '../support/civiform_fixtures'
import {
  AdminPrograms,
  enableFeatureFlag,
  isLocalDevEnvironment,
  seedProgramsAndCategories,
  loginAsAdmin,
  validateScreenshot,
} from '../support'
import {ProgramVisibility} from '../support/admin_programs'

test.describe('Program list page.', () => {
  test('view draft program', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoAdminProgramsPage()

    const programCard = page.locator('.cf-admin-program-card').first()
    await expect(programCard.getByText('Draft')).toBeVisible()
    await expect(programCard.getByText('Active')).toBeHidden()
  })

  test('view active program', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()

    const programCard = page.locator('.cf-admin-program-card').first()
    await expect(programCard.getByText('Draft')).toBeHidden()
    await expect(programCard.getByText('Active')).toBeVisible()

    // full page screenshot
    await validateScreenshot(page, 'program-list-one-active-program')
  })

  test('view program with active and draft versions', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()

    await adminPrograms.createNewVersion(programName)
    await adminPrograms.gotoAdminProgramsPage()

    const programCard = page.locator('.cf-admin-program-card').first()
    await expect(programCard.getByText('Draft')).toBeVisible()
    await expect(programCard.getByText('Active')).toBeVisible()
  })

  test('view program with description', async ({page, adminPrograms}) => {
    const programName = 'Program With Short Description'
    const programLongDescription =
      'A very very very very very very long description'
    const programShortDescription =
      'A short description with some __markdown__ and a [link](https://www.example.com)'
    const programShortDescriptionWithoutMarkdown =
      'A short description with some markdown and a link'

    await test.step('create new program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(
        programName,
        programLongDescription,
        programShortDescription,
      )
    })

    await test.step('check that long description is shown when North Star flag is off', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      const firstProgramCard = page.locator('.cf-admin-program-card').first()
      const firstProgramDesc = firstProgramCard.locator(
        '.cf-program-description',
      )
      await expect(
        firstProgramDesc.getByText(programLongDescription),
      ).toBeVisible()
      await expect(
        firstProgramDesc.locator(
          `text=${programShortDescriptionWithoutMarkdown}`,
        ),
      ).toHaveCount(0) // short description should not be shown
    })

    await enableFeatureFlag(page, 'north_star_applicant_ui')

    await test.step('check that short description stripped of markdown is shown when North Star flag is on', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      const firstProgramCard = page.locator('.cf-admin-program-card').first()
      const firstProgramDesc = firstProgramCard.locator(
        '.cf-program-description',
      )
      await expect(
        firstProgramDesc.getByText(programShortDescriptionWithoutMarkdown),
      ).toBeVisible()
      await expect(
        firstProgramDesc.locator(`text=${programLongDescription}`),
      ).toHaveCount(0) // long description should not be shown
    })
  })

  test('view program with categories', async ({page, adminPrograms}) => {
    await enableFeatureFlag(page, 'program_filtering_enabled')
    const programName = 'Program with Categories'
    const programLongDescription =
      'A very very very very very very long description'
    const programShortDescription = 'A very short description'

    await test.step('seed categories', async () => {
      await seedProgramsAndCategories(page)
      await page.goto('/')
    })

    await test.step('create new program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(
        programName,
        programLongDescription,
        programShortDescription,
      )
    })

    await test.step('check that categories show as "None"', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      const firstProgramCard = page.locator('.cf-admin-program-card').first()
      await expect(firstProgramCard.getByText('Categories: None')).toBeVisible()
    })
    await test.step('check that program visibility is displayed', async () => {
      const firstProgramCard = page.locator('.cf-admin-program-card').first()
      await expect(
        firstProgramCard.getByText('Visibility state: Public'),
      ).toBeVisible()
    })

    await test.step('add two categories', async () => {
      await adminPrograms.gotoEditDraftProgramPage(programName)
      await page.getByRole('button', {name: 'Edit program details'}).click()

      await page.getByText('Internet').check()
      await page.getByText('Education').check()
      await adminPrograms.submitProgramDetailsEdits()
    })

    await test.step('check that selected categories show on program card', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      const programCard = page.locator(
        '.cf-admin-program-card:has-text("Program with Categories")',
      )
      await expect(
        programCard.getByText('Categories: Education, Internet'),
      ).toBeVisible()
      await validateScreenshot(
        programCard,
        'program-list-with-categories',
        false,
      )
    })
  })

  test('view programs under two tabs - in use and disabled', async ({
    page,
    adminPrograms,
  }) => {
    await enableFeatureFlag(page, 'disabled_visibility_condition_enabled')
    await loginAsAdmin(page)

    const activeElementClassList =
      'text-blue-600 hover:text-blue-500 inline-flex items-center m-2 border-blue-400 border-b-2'
    const inactiveElementClassList =
      'text-blue-600 hover:text-blue-500 inline-flex items-center m-2'
    const publicProgram = 'List test public program'
    const disabledProgram = 'List test disabled program'
    await adminPrograms.addProgram(publicProgram)
    await expect(page.locator('a:has-text("Disabled")')).toBeHidden()

    await adminPrograms.addDisabledProgram(disabledProgram)

    // in use programs
    await expectProgramListElements(adminPrograms, [publicProgram])
    await expect(page.getByRole('link', {name: 'In use'})).toHaveClass(
      activeElementClassList,
    )
    await expect(page.getByRole('link', {name: 'Disabled'})).toHaveClass(
      inactiveElementClassList,
    )

    // disabled programs
    await expectProgramListElements(
      adminPrograms,
      [disabledProgram],
      /* isProgramDisabled = */ true,
    )
    await expect(page.getByRole('link', {name: 'In use'})).toHaveClass(
      inactiveElementClassList,
    )
    await expect(page.getByRole('link', {name: 'Disabled'})).toHaveClass(
      activeElementClassList,
    )
  })

  test('sorts by last updated, preferring draft over active', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programOne = 'List test program one'
    const programTwo = 'List test program two'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.addProgram(programTwo)

    // Most recently added program is on top.
    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    // Publish all programs, order should be maintained.
    await adminPrograms.publishAllDrafts()
    await expectProgramListElements(adminPrograms, [programTwo, programOne])

    // Now create a draft version of the previously last program. After,
    // it should be on top.
    await adminPrograms.createNewVersion(programOne)
    await expectProgramListElements(adminPrograms, [programOne, programTwo])

    // Now create a new program, which should be on top.
    const programThree = 'List test program three'
    await adminPrograms.addProgram(programThree)
    await expectProgramListElements(adminPrograms, [
      programThree,
      programOne,
      programTwo,
    ])
  })

  test('shows which program is the common intake when enabled', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programOne = 'List test program one'
    const programTwo = 'List test program two'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.addProgram(
      programTwo,
      'program description',
      'short program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
      'admin description',
      /* isCommonIntake= */ true,
    )

    await expectProgramListElements(adminPrograms, [programTwo, programOne])
    const firstProgramCard = page.locator('.cf-admin-program-card').first()

    await expect(firstProgramCard.getByText('Pre-screener')).toBeVisible()
  })

  test('shows information about universal questions when the flag is enabled and at least one universal question is set', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    // Create a program and question that is not universal
    const programOne = 'program one'
    await adminPrograms.addProgram(programOne)
    const nameQuestion = 'name'
    await adminQuestions.addNameQuestion({
      questionName: nameQuestion,
      universal: false,
    })
    await adminPrograms.gotoEditDraftProgramPage(programOne)
    await adminPrograms.addQuestionFromQuestionBank(nameQuestion)

    await adminPrograms.gotoAdminProgramsPage()
    expect(await page.innerText('.cf-admin-program-card')).not.toContain(
      'universal questions',
    )

    // Create a universal question
    const textQuestion = 'text'
    await adminQuestions.addTextQuestion({
      questionName: textQuestion,
      universal: true,
    })
    await adminPrograms.gotoAdminProgramsPage()
    expect(await page.innerText('.cf-admin-program-card')).toContain(
      'universal questions',
    )
  })

  async function expectProgramListElements(
    adminPrograms: AdminPrograms,
    expectedPrograms: string[],
    isProgramDisabled: boolean = false,
  ) {
    if (expectedPrograms.length === 0) {
      throw new Error('expected at least one program')
    }
    const programListNames = await adminPrograms.programNames(isProgramDisabled)
    expect(programListNames).toEqual(expectedPrograms)
  }

  test('publishes a single program', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programOne = 'list-test-program-one'
    await adminPrograms.addProgram(programOne)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.createNewVersion(programOne)
    await adminPrograms.expectDraftProgram(programOne)

    await page.click(`#publish-modal-${programOne}-button`)
    await page.click(`#publish-modal-${programOne} .cf-modal-close`)

    // Draft not published because modal was dismissed.
    await adminPrograms.expectDraftProgram(programOne)

    await page.click(`#publish-modal-${programOne}-button`)
    await page.click(`#publish-modal-${programOne} button[type="submit"]`)

    // Program was published.
    await adminPrograms.expectDoesNotHaveDraftProgram(programOne)
    await adminPrograms.expectActiveProgram(programOne)
  })

  test('publishing a single program shows a modal with conditional warning about universal questions', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    // Create a program and question that is not universal
    const programOne = 'program one'
    await adminPrograms.addProgram(programOne)
    const nameQuestion = 'name'
    await adminQuestions.addNameQuestion({
      questionName: nameQuestion,
      universal: false,
    })
    await adminPrograms.gotoEditDraftProgramPage(programOne)
    await adminPrograms.addQuestionFromQuestionBank(nameQuestion)

    const publishProgramOneModalButton = '#publish-modal-program-one-button'
    const publishProgramOneModal = '#publish-modal-program-one'

    await adminPrograms.gotoAdminProgramsPage()
    await page.click(publishProgramOneModalButton)
    expect(await page.innerText(publishProgramOneModal)).toContain(
      'Are you sure you want to publish program one and all of its draft questions?',
    )
    // Warning should not show because there are no universal questions
    expect(await page.innerText(publishProgramOneModal)).not.toContain(
      'Warning: This program does not use all recommended universal questions.',
    )

    await validateScreenshot(
      page.locator(publishProgramOneModal),
      'publish-single-program-modal-no-warning',
    )
    // Dismiss the modal
    await adminQuestions.clickSubmitButtonAndNavigate('Cancel')

    // Create a universal question
    const textQuestion = 'text'
    await adminQuestions.addTextQuestion({
      questionName: textQuestion,
      universal: true,
    })
    await adminPrograms.gotoAdminProgramsPage()
    await page.click(publishProgramOneModalButton)
    // Warning should show because there is a universal question that is not used in this program
    expect(await page.innerText(publishProgramOneModal)).toContain(
      'Warning: This program does not use all recommended universal questions.',
    )
    await validateScreenshot(
      page.locator(publishProgramOneModal),
      'publish-single-program-modal-with-warning',
    )
    // Dismiss the modal
    await adminQuestions.clickSubmitButtonAndNavigate('Cancel')

    // Add the universal question to the program
    await adminPrograms.gotoEditDraftProgramPage(programOne)
    await adminPrograms.addQuestionFromQuestionBank(textQuestion)
    await adminPrograms.gotoAdminProgramsPage()
    await page.click(publishProgramOneModalButton)
    // Warning should not show because the program uses all universal questions
    expect(await page.innerText(publishProgramOneModal)).not.toContain(
      'Warning: This program does not use all recommended universal questions.',
    )
    await adminQuestions.clickSubmitButtonAndNavigate('Publish program')

    // Program was published.
    await adminPrograms.expectDoesNotHaveDraftProgram(programOne)
    await adminPrograms.expectActiveProgram(programOne)
  })

  test('program list has current image', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Images Flag On Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()

    const programCard = page.locator('.cf-admin-program-card').first()
    await validateScreenshot(programCard, 'program-list')
  })

  test('program list with new image in draft', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await loginAsAdmin(page)

    // Start the program as having no image
    const programName = 'New Image Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()

    // Set a new image on the new draft program
    await adminPrograms.createNewVersion(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-tall.png',
    )
    await adminPrograms.gotoAdminProgramsPage()

    // Verify that the new image is shown in the Draft row
    // and a gray placeholder image icon is shown in the Active row.
    const programCard = page.locator('.cf-admin-program-card').first()
    await validateScreenshot(programCard, 'program-list-with-new-draft-image')
  })

  // This test is flaky in staging prober tests, so only run it locally and on
  // GitHub actions. See issue #6624 for more details.
  if (isLocalDevEnvironment()) {
    test('program list with different active and draft image', async ({
      page,
      adminPrograms,
      adminProgramImage,
    }) => {
      await loginAsAdmin(page)

      const programName = 'Different Images Program'
      await adminPrograms.addProgram(programName)
      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-wide.png',
      )
      await adminPrograms.publishAllDrafts()

      // Set a new image on the new draft program
      await adminPrograms.createNewVersion(programName)
      await adminPrograms.goToProgramImagePage(programName)
      await adminProgramImage.setImageFileAndSubmit(
        'src/assets/program-summary-image-tall.png',
      )
      await adminPrograms.gotoAdminProgramsPage()

      const programCard = page.locator('.cf-admin-program-card').first()
      await validateScreenshot(
        programCard,
        'program-list-with-different-active-and-draft-images',
      )
    })
  }

  test('program list with same active and draft image', async ({
    page,
    adminPrograms,
    adminProgramImage,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Same Image Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()

    // Create a new draft version of the program, but don't edit the image
    await adminPrograms.createNewVersion(programName)
    await adminPrograms.gotoAdminProgramsPage()

    // Verify that the current image is shown twice, in both the Active row and Draft row
    const programCard = page.locator('.cf-admin-program-card').first()
    await validateScreenshot(
      programCard,
      'program-list-with-same-active-and-draft-image',
    )
  })

  test('program list shows Import existing program link', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoAdminProgramsPage()
    await page.getByText('Import existing program').isVisible()
  })
})
