import {
  createTestContext,
  enableFeatureFlag,
  disableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'

describe('admin program view page', () => {
  const ctx = createTestContext()

  it('view active program shows read only view', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)

    const programName = 'Active Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoViewActiveProgramPage(programName)
    await validateScreenshot(page, 'program-read-only-view')
  })

  it('program list has current image if images flags on', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Images Flag On Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()

    await validateScreenshot(page, 'program-list-with-image-flag-on')
  })

  it('program list does not show current image if images flags off', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    // Enable the flag to set a program image
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'Images Flag Off Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.goToProgramImagePage(programName)
    await adminProgramImage.setImageFileAndSubmit(
      'src/assets/program-summary-image-wide.png',
    )
    await adminPrograms.publishAllDrafts()

    // Disable the flag then check the program list page
    await disableFeatureFlag(page, 'program_card_images')
    await adminPrograms.gotoAdminProgramsPage()

    await validateScreenshot(page, 'program-list-with-image-flag-off')
  })

  it('program with no image', async () => {
    const {page, adminPrograms} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

    const programName = 'No Image Program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.publishAllDrafts()
    await adminPrograms.gotoAdminProgramsPage()

    await validateScreenshot(page, 'program-list-no-image')
  })

  it('program with new image in draft', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

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
    // and an empty rectangle is shown in the Active row.
    await validateScreenshot(page, 'program-list-with-new-draft-image')
  })

  it('program with different active and draft image', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

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

    await validateScreenshot(
      page,
      'program-list-with-different-active-and-draft-images',
    )
  })

  it('program with same active and draft image', async () => {
    const {page, adminPrograms, adminProgramImage} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'program_card_images')

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

    // Verify that the current image is shown twice, in the Active row and Draft row
    await validateScreenshot(
      page,
      'program-list-with-same-active-and-draft-image',
    )
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

  it('view program, view multiple blocks, then start editing with extra long screen name and description', async () => {
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
})
