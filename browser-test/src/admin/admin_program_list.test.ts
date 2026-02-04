import {test, expect} from '../support/civiform_fixtures'
import {
  AdminPrograms,
  isLocalDevEnvironment,
  loginAsAdmin,
  loginAsProgramAdmin,
  logout,
  validateScreenshot,
} from '../support'
import {
  ProgramAction,
  ProgramCategories,
  ProgramExtraAction,
  ProgramLifecycle,
  ProgramVisibility,
} from '../support/admin_programs'

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

    await test.step('check that program visibility is displayed', async () => {
      await expect(
        programCard.getByText('Visibility state: Public'),
      ).toBeVisible()
    })

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
    const programShortDescription = 'A short description'

    await test.step('create new program', async () => {
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName, {
        description: programLongDescription,
        shortDescription: programShortDescription,
      })
    })

    await test.step('check that short description is shown', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      const firstProgramCard = page.locator('.cf-admin-program-card').first()
      const firstProgramDesc = firstProgramCard.locator(
        '.cf-program-description',
      )
      await expect(
        firstProgramDesc.getByText(programShortDescription),
      ).toBeVisible()
      await expect(
        firstProgramDesc.locator(`text=${programLongDescription}`),
      ).toHaveCount(0) // long description should not be shown
    })
  })

  test('view program with categories', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    const programName = 'Program with Categories'
    const programLongDescription =
      'A very very very very very very long description'
    const programShortDescription = 'A very short description'

    await seeding.seedProgramsAndCategories()

    await test.step('create new program', async () => {
      await page.goto('/')
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName, {
        description: programLongDescription,
        shortDescription: programShortDescription,
      })
    })

    await test.step('check that categories show as "None"', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      const firstProgramCard = page.locator('.cf-admin-program-card').first()
      await expect(firstProgramCard.getByText('Categories: None')).toBeVisible()
    })

    await test.step('add two categories', async () => {
      await adminPrograms.selectProgramCategories(
        programName,
        [ProgramCategories.INTERNET, ProgramCategories.EDUCATION],
        /* isActive= */ false,
      )
    })

    await test.step('check that selected categories show on program card', async () => {
      await adminPrograms.gotoAdminProgramsPage()
      const programCard = page.locator(
        '.cf-admin-program-card:has-text("Program with Categories")',
      )
      await expect(
        programCard.getByText('Categories: Education, Internet'),
      ).toBeVisible()
      await validateScreenshot(programCard, 'program-list-with-categories', {
        fullPage: false,
      })
    })
  })

  test('view programs under two tabs - in use and disabled', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const activeElementClassList =
      'text-blue-600 hover:text-blue-500 inline-flex items-center m-2 border-blue-400 border-b-2'
    const inactiveElementClassList =
      'text-blue-600 hover:text-blue-500 inline-flex items-center m-2'
    const publicProgram = 'List test public program'
    const disabledProgram = 'List test disabled program'
    await adminPrograms.addProgram(publicProgram)
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

  test('shows program type indicator in card', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const program = 'Program'
    const preScreenerProgram = 'Pre screener program'
    const externalProgram = 'External'
    await adminPrograms.addProgram(program)

    await adminPrograms.addPreScreener(
      preScreenerProgram,
      'short program description',
      ProgramVisibility.PUBLIC,
    )
    await adminPrograms.addExternalProgram(
      externalProgram,
      'short program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
    )

    // Pre-screener program should always be first. Then, order is by last modified.
    await expectProgramListElements(adminPrograms, [
      preScreenerProgram,
      externalProgram,
      program,
    ])

    const firstProgramCard = page.locator('.cf-admin-program-card').first()
    await expect(firstProgramCard.getByText('Pre-screener')).toBeVisible()

    const secondProgramCard = page.locator('.cf-admin-program-card').nth(1)
    await expect(secondProgramCard.getByText('External program')).toBeVisible()
  })

  test('shows information about universal questions when at least one universal question is set', async ({
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

  test('external program does not show information about universal questions', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    // Create an external program and a universal question
    await adminPrograms.addExternalProgram(
      'External program',
      'short program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
    )
    await adminQuestions.addTextQuestion({
      questionName: 'text question',
      universal: true,
    })

    await adminPrograms.gotoAdminProgramsPage()

    const programCard = page.locator('.cf-admin-program-card').first()
    await expect(programCard).not.toContainText('universal questions')
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

  test('publishing an external program shows a modal without conditional warning about universal questions', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    // Create an external program and a universal question
    const externalProgramName = 'External Program'
    await adminPrograms.addExternalProgram(
      externalProgramName,
      'short program description',
      'https://usa.gov',
      ProgramVisibility.PUBLIC,
    )
    await adminQuestions.addTextQuestion({
      questionName: 'text question',
      universal: true,
    })

    // Trigger the publish modal for the external program
    await adminPrograms.gotoAdminProgramsPage()
    const publishButton = adminPrograms.getProgramAction(
      externalProgramName,
      ProgramLifecycle.DRAFT,
      ProgramAction.PUBLISH,
    )
    await publishButton.click()

    // Verify modal does not show universal question warning, since they are not
    //  applicable to external programs
    const publishExternalProgramModal = '#publish-modal-external-program'
    expect(await page.innerText(publishExternalProgramModal)).toContain(
      'Are you sure you want to publish External Program?',
    )
    expect(await page.innerText(publishExternalProgramModal)).not.toContain(
      'Warning: This program does not use all recommended universal questions.',
    )
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
    await expect(page.getByText('Import existing program')).toBeVisible()
  })

  test('external program card actions', async ({page, adminPrograms}) => {
    const externalProgram = 'External'

    await test.step('add external program as a CiviForm admin', async () => {
      await loginAsAdmin(page)

      await adminPrograms.addExternalProgram(
        externalProgram,
        'short program description',
        'https://usa.gov',
        ProgramVisibility.PUBLIC,
      )
      await expectProgramListElements(adminPrograms, [externalProgram])
    })

    await test.step(
      'verify program card for external program on draft mode ' +
        'in CiviForm admin panel',
      async () => {
        await adminPrograms.expectProgramActionsVisible(
          externalProgram,
          ProgramLifecycle.DRAFT,
          [ProgramAction.PUBLISH, ProgramAction.EDIT],
          [ProgramExtraAction.MANAGE_TRANSLATIONS],
        )
        await adminPrograms.expectProgramActionsHidden(
          externalProgram,
          ProgramLifecycle.DRAFT,
          [],
          [
            ProgramExtraAction.MANAGE_ADMINS,
            ProgramExtraAction.MANAGE_APPLICATIONS,
            ProgramExtraAction.EXPORT,
          ],
        )
      },
    )

    await test.step(
      'verify program card for external program on active mode ' +
        'in CiviForm admin panel',
      async () => {
        await adminPrograms.publishProgram(externalProgram)
        await adminPrograms.expectProgramActionsVisible(
          externalProgram,
          ProgramLifecycle.ACTIVE,
          [ProgramAction.VIEW],
          [ProgramExtraAction.EDIT],
        )
        await adminPrograms.expectProgramActionsHidden(
          externalProgram,
          ProgramLifecycle.ACTIVE,
          [ProgramAction.SHARE],
          [
            ProgramExtraAction.MANAGE_ADMINS,
            ProgramExtraAction.VIEW_APPLICATIONS,
            ProgramExtraAction.EXPORT,
          ],
        )

        await logout(page)
      },
    )

    await test.step(
      'verify program card for external program on active mode ' +
        'in Program admin panel',
      async () => {
        await loginAsProgramAdmin(page)

        // All actions for program admins are hidden
        await adminPrograms.expectProgramActionsVisible(
          externalProgram,
          ProgramLifecycle.ACTIVE,
          [],
          [],
        )
        await adminPrograms.expectProgramActionsHidden(
          externalProgram,
          ProgramLifecycle.ACTIVE,
          [ProgramAction.SHARE, ProgramAction.VIEW_APPLICATIONS],
          [],
        )
      },
    )
  })
})
