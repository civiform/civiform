import {
  createTestContext,
  enableFeatureFlag,
  disableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'

describe('admin program view page', () => {
  const ctx = createTestContext()

  it('view active program, without draft and after creating draft', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)

    const programName = 'Apc program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoViewActiveProgramPage(programName)
    await validateScreenshot(page, 'program-read-only-view')
    await adminPrograms.gotoAdminProgramsPage()
    await validateScreenshot(page, 'program-list-only-one-active-program')
    await adminPrograms.createNewVersion(programName)

    await adminPrograms.gotoViewActiveProgramPage(programName)
    await adminPrograms.gotoAdminProgramsPage()
    await validateScreenshot(page, 'program-list-active-and-draft-program')
  })

  it('view draft program has edit image button if images flag on', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Draft Program'
    await adminPrograms.addProgram(programName)

    await validateScreenshot(page, 'program-draft-view-images-flag-on')
  })

  it('view draft program has no edit image button if images flag off', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await disableFeatureFlag(page, 'program_card_images')

    const programName = 'Draft Program'
    await adminPrograms.addProgram(programName)

    await validateScreenshot(page, 'program-draft-view-images-flag-off')
  })

  it('view program with universal questions', async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'universal_questions')

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

  it('view program, view multiple blocks, then start editing', async () => {
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

  it('view program, view multiple blocks, then start editing with long screen name and description', async () => {
    const {page, adminPrograms, adminQuestions} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'esri_address_correction_enabled')

    const programName = 'Apc program'
    await adminQuestions.addAddressQuestion({questionName: 'address-q'})
    await adminQuestions.addDateQuestion({questionName: 'date-q'})
    await adminQuestions.addEmailQuestion({questionName: 'email-q'})

    await adminPrograms.addProgram(programName)
    await adminPrograms.addProgramBlock(
      programName,
      'screen 2 extra looooooooooooooooooooooooooooooooooooooo' +
      'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
      'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
      'oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
      'ooooooooooooooooooooooooçooooooooooooooooooooooooooooooooooooooooooooo' +
      'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooong description',
      [],
    )
    await adminPrograms.editProgramBlock(
      programName,
      'dummy loooooooooooooooooooooooooooooooooooooooooooooooo' +
      'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
      'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
      'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
      'ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo' +
      'oooooooooooooooooooooooooooooooooooooooooooooooooooooooong description',
      ['address-q', 'date-q', 'email-q'],
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

    await validateScreenshot(page, 'view-program-block-2-with-long-description')

    await adminPrograms.gotoViewActiveProgramPageAndStartEditing(programName)
    await adminPrograms.expectProgramBlockEditPage(programName)

    await validateScreenshot(page, 'view-program-start-editing-with-long-description')
  })
})
