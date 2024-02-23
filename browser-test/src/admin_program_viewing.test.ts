import {
  createTestContext,
  enableFeatureFlag,
  disableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'

describe('admin program view page', () => {
  const ctx = createTestContext()

  test('view active program shows read only view', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)

    const programName = 'Active Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoViewActiveProgramPage(programName)
    await validateScreenshot(page, 'program-read-only-view')
  })

  test('view draft program has edit image button if images flag on', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Draft Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoEditDraftProgramPage(programName)

    await validateScreenshot(page, 'program-draft-view-images-flag-on')
  })

  test('view draft program has no edit image button if images flag off', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await disableFeatureFlag(page, 'program_card_images')

    const programName = 'Draft Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoEditDraftProgramPage(programName)

    await validateScreenshot(page, 'program-draft-view-images-flag-off')
  })

  test('view program with universal questions', async () => {
    const {page, adminPrograms, adminQuestions} = ctx
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

  test('view program, view multiple blocks, then start editing with extra long screen name and description', async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Apc program'
    await adminQuestions.addAddressQuestion({questionName: 'address-q'})

    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(programName, 'screen 2 description', [])
    await adminPrograms.editProgramBlockWithBlockName(
      programName,
      'Screen 2 ooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo',
      'dummy description oooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
        'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo',
      ['address-q'],
    )
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

  test('view program, view multiple blocks, then start editing', async () => {
    const {page, adminPrograms, adminQuestions} = ctx
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
