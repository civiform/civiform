import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  createBrowserContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('address applicant flow', () => {
  const ctx = createBrowserContext(/* clearDb= */ false)

  describe('single required address question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single address'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['address-test-q'],
        programName,
      )

      await logout(ctx.page)
    })

    it('validate screenshot', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(ctx.page, 'address')
    })

    it('validate screenshot with errors', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(ctx.page, 'address-errors')
    })

    it('does not show errors initially', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
      )
      let error = ctx.page.locator('.cf-address-street-1-error')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-city-error')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-state-error')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(true)
    })

    it('with valid address does submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with empty address does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '')
      await applicantQuestions.clickNext()

      let error = ctx.page.locator('.cf-address-street-1-error')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-city-error')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-state-error')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(false)
    })

    it('with invalid address does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        'notazipcode',
      )
      await applicantQuestions.clickNext()

      const error = ctx.page.locator('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(false)
    })
  })

  describe('multiple address questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple addresses'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-a-q',
      })
      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-b-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['address-test-a-q', 'address-test-b-q'],
        programName,
      )

      await logout(ctx.page)
    })

    it('with valid addresses does submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        0,
      )
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with first invalid does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '', 0)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = ctx.page.locator('.cf-address-street-1-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-city-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-state-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-zip-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)

      // Second question has no errors.
      error = ctx.page.locator('.cf-address-street-1-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-city-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-state-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-zip-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
    })

    it('with second invalid does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        0,
      )
      await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
      await applicantQuestions.clickNext()

      // First question has no errors.
      let error = ctx.page.locator('.cf-address-street-1-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-city-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-state-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = ctx.page.locator('.cf-address-zip-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)

      // Second question has errors.
      error = ctx.page.locator('.cf-address-street-1-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-city-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-state-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-zip-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(ctx.page)
    })
  })

  // One optional address followed by one required address.
  describe('optional address question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for optional address'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-optional-q',
      })
      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-required-q',
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['address-test-required-q'],
        'address-test-optional-q',
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(ctx.page)
    })

    it('with valid required address does submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with invalid optional address does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        '',
        '',
        '',
        '',
        0,
      )
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = ctx.page.locator('.cf-address-city-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-state-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = ctx.page.locator('.cf-address-zip-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
    })

    describe('with invalid required address', () => {
      beforeEach(async () => {
        await loginAsGuest(ctx.page)
        await selectApplicantLanguage(ctx.page, 'English')

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
        await applicantQuestions.clickNext()
      })

      it('does not submit', async () => {
        // Second question has errors.
        let error = ctx.page.locator('.cf-address-street-1-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = ctx.page.locator('.cf-address-city-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = ctx.page.locator('.cf-address-state-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = ctx.page.locator('.cf-address-zip-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
      })

      it('optional has no errors', async () => {
        // First question has no errors.
        let error = ctx.page.locator('.cf-address-street-1-error >> nth=0')
        expect(await error.isHidden()).toEqual(true)
        error = ctx.page.locator('.cf-address-city-error >> nth=0')
        expect(await error.isHidden()).toEqual(true)
        error = ctx.page.locator('.cf-address-state-error >> nth=0')
        expect(await error.isHidden()).toEqual(true)
        error = ctx.page.locator('.cf-address-zip-error >> nth=0')
        expect(await error.isHidden()).toEqual(true)
      })
    })
  })
})
