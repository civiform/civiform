import {
  createTestContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('address applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('single required address question', () => {
    const programName = 'Test program for single address'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['address-test-q'],
        programName,
      )

      await logout(page)
    })

    it('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'address')
    })

    it('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'address-errors')
    })

    it('does not show errors initially', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
      )
      let error = page.locator('.cf-address-street-1-error')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-city-error')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-state-error')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(true)
    })

    it('with valid address does submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with empty address does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '')
      await applicantQuestions.clickNext()

      let error = page.locator('.cf-address-street-1-error')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-city-error')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-state-error')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(false)
    })

    it('with invalid address does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        'notazipcode',
      )
      await applicantQuestions.clickNext()

      const error = page.locator('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(false)
    })
  })

  describe('multiple address questions', () => {
    const programName = 'Test program for multiple addresses'

    beforeAll(async () => {
      const {page, adminPrograms, adminQuestions} = ctx
      await loginAsAdmin(page)

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

      await logout(page)
    })

    it('with valid addresses does submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        0,
      )
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '', 0)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = page.locator('.cf-address-street-1-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-city-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-state-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-zip-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)

      // Second question has no errors.
      error = page.locator('.cf-address-street-1-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-city-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-state-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-zip-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
    })

    it('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        0,
      )
      await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
      await applicantQuestions.clickNext()

      // First question has no errors.
      let error = page.locator('.cf-address-street-1-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-city-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-state-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = page.locator('.cf-address-zip-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)

      // Second question has errors.
      error = page.locator('.cf-address-street-1-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-city-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-state-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-zip-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
    })

    it('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  // One optional address followed by one required address.
  describe('optional address question', () => {
    const programName = 'Test program for optional address'

    beforeAll(async () => {
      const {page, adminPrograms, adminQuestions} = ctx
      await loginAsAdmin(page)

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
      await adminPrograms.publishAllPrograms()

      await logout(page)
    })

    it('with valid required address does submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with invalid optional address does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

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
        'WA',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = page.locator('.cf-address-city-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-state-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = page.locator('.cf-address-zip-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
    })

    describe('with invalid required address', () => {
      beforeEach(async () => {
        const {page, applicantQuestions} = ctx
        await loginAsGuest(page)
        await selectApplicantLanguage(page, 'English')

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
        await applicantQuestions.clickNext()
      })

      it('does not submit', async () => {
        const {page} = ctx
        // Second question has errors.
        let error = page.locator('.cf-address-street-1-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = page.locator('.cf-address-city-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = page.locator('.cf-address-state-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = page.locator('.cf-address-zip-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
      })

      it('optional has no errors', async () => {
        const {page} = ctx
        // First question has no errors.
        let error = page.locator('.cf-address-street-1-error >> nth=0')
        expect(await error.isHidden()).toEqual(true)
        error = page.locator('.cf-address-city-error >> nth=0')
        expect(await error.isHidden()).toEqual(true)
        error = page.locator('.cf-address-state-error >> nth=0')
        expect(await error.isHidden()).toEqual(true)
        error = page.locator('.cf-address-zip-error >> nth=0')
        expect(await error.isHidden()).toEqual(true)
      })
    })
  })
})
