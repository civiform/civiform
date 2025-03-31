import {test, expect} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'

test.describe('admin program view page', () => {
  test('view active program shows read only view', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Active Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoViewActiveProgramPage(programName)
    await validateScreenshot(page, 'program-read-only-view')
  })

  test('view program details shows program categories', async ({
    page,
    adminPrograms,
    seeding,
  }) => {
    const programName = 'Active Program'
    await enableFeatureFlag(page, 'program_filtering_enabled')

    await seeding.seedProgramsAndCategories()

    await test.step('login as admin', async () => {
      await page.goto('/')
      await loginAsAdmin(page)
    })

    await test.step('create and publish new program', async () => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.publishAllDrafts()
    })

    await test.step('expect categories to be none on details page', async () => {
      await adminPrograms.gotoViewActiveProgramPage(programName)
      await expect(page.getByText('Categories: None')).toBeVisible()
    })

    await test.step('add two categories', async () => {
      await page.getByRole('button', {name: 'Edit program'}).click()
      await page.getByRole('button', {name: 'Edit program details'}).click()

      await page.getByText('Internet').check()
      await page.getByText('Education').check()
      await adminPrograms.submitProgramDetailsEdits()
    })

    await test.step('expect to see the two categories on details page', async () => {
      await expect(
        page.getByText('Categories: Education, Internet'),
      ).toBeVisible()
    })
  })

  test('view draft program', async ({page, adminPrograms}) => {
    await loginAsAdmin(page)

    const programName = 'Draft Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoEditDraftProgramPage(programName)

    await validateScreenshot(page, 'program-draft-view')
  })

  test('view program with universal questions', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Program with universal questions'
    await adminQuestions.addTextQuestion({
      questionName: 'nonuniversal-text',
      universal: false,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'universal-text',
      universal: true,
    })
    await adminQuestions.addAddressQuestion({
      questionName: 'universal-address',
      universal: true,
    })

    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      'universal-text',
      'nonuniversal-text',
      'universal-address',
    ])
    await adminPrograms.publishAllDrafts()

    await adminPrograms.gotoViewActiveProgramPage(programName)
    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'universal-text',
      true,
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'nonuniversal-text',
      false,
    )
    await adminPrograms.expectQuestionCardUniversalBadgeState(
      'universal-address',
      true,
    )
    await validateScreenshot(page, 'program-view-universal-questions')
  })

  test('view program, view multiple blocks, then start editing with extra long screen name and description', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Apc program'
    await adminQuestions.addAddressQuestion({questionName: 'address-q'})

    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName, 'screen 2 description', [])

    await adminPrograms.editProgramBlockUsingSpec(programName, {
      name:
        'Screen 2 ooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo',
      description:
        'dummy description oooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo',
      questions: [
        {
          name: 'address-q',
        },
      ],
    })

    await adminPrograms.publishAllDrafts()

    await adminPrograms.gotoViewActiveProgramPage(programName)

    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.gotoToBlockInReadOnlyProgram('2')
    await adminPrograms.expectReadOnlyProgramBlock('2')

    await adminPrograms.expectQuestionCardWithLabel(
      'address-q',
      'required question',
    )
    await validateScreenshot(
      page,
      'view-program-block-2-long-screen-name-and-description',
    )

    await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
    await adminPrograms.expectProgramBlockEditPage(programName)

    await validateScreenshot(
      page,
      'view-program-start-editing-extra-long-screen-name-and-description',
    )
  })

  test('view program, view multiple blocks, then start editing', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Apc program'
    await adminQuestions.addAddressQuestion({questionName: 'address-q'})
    await adminQuestions.addDateQuestion({questionName: 'date-q'})
    await adminQuestions.addEmailQuestion({questionName: 'email-q'})

    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName, 'screen 2 description', [])
    await adminPrograms.editProgramBlock(programName, 'dummy description', [
      'address-q',
      'date-q',
      'email-q',
    ])
    await adminPrograms.publishAllDrafts()

    await adminPrograms.gotoViewActiveProgramPage(programName)

    await adminPrograms.gotoToBlockInReadOnlyProgram('1')
    await adminPrograms.expectReadOnlyProgramBlock('1')
    await adminPrograms.gotoToBlockInReadOnlyProgram('2')
    await adminPrograms.expectReadOnlyProgramBlock('2')

    await adminPrograms.expectQuestionCardWithLabel(
      'address-q',
      'required question',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      'address-q',
      'address correction: disabled',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      'date-q',
      'required question',
    )
    await adminPrograms.expectQuestionCardWithLabel(
      'email-q',
      'required question',
    )

    await validateScreenshot(page, 'view-program-block-2')

    await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
    await adminPrograms.expectProgramBlockEditPage(programName)

    await validateScreenshot(page, 'view-program-start-editing')
  })
})
