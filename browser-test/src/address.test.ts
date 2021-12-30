import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  logout,
  resetSession,
  selectApplicantLanguage,
  startSession,
} from './support'

describe('address applicant flow', () => {
  let pageObject

  beforeAll(async () => {
    const { page } = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single required address question', () => {
    let applicantQuestions
    const programName = 'test program for single address'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['address-test-q'],
        programName
      )

      await logout(pageObject)
    })

    it('does not show errors initially', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321'
      )
      let error = await pageObject.$('.cf-address-street-1-error')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-city-error')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-state-error')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(true)
    })

    it('with valid address does submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321'
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with empty address does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '')
      await applicantQuestions.clickNext()

      let error = await pageObject.$('.cf-address-street-1-error')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-city-error')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-state-error')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(false)
    })

    it('with invalid address does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        'notazipcode'
      )
      await applicantQuestions.clickNext()

      let error = await pageObject.$('.cf-address-zip-error')
      expect(await error.isHidden()).toEqual(false)
    })
  })

  describe('multiple address questions', () => {
    let applicantQuestions
    const programName = 'test program for multiple addresses'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-a-q',
      })
      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-b-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['address-test-a-q', 'address-test-b-q'],
        programName
      )

      await logout(pageObject)
    })

    it('with valid addresses does submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        0
      )
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        1
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with first invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '', 0)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        1
      )
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = await pageObject.$('.cf-address-street-1-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-city-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-state-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-zip-error >> nth=0')
      expect(await error.isHidden()).toEqual(false)

      // Second question has no errors.
      error = await pageObject.$('.cf-address-street-1-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-city-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-state-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-zip-error >> nth=1')
      expect(await error.isHidden()).toEqual(true)
    })

    it('with second invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        0
      )
      await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
      await applicantQuestions.clickNext()

      // First question has no errors.
      let error = await pageObject.$('.cf-address-street-1-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-city-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-state-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)
      error = await pageObject.$('.cf-address-zip-error >> nth=0')
      expect(await error.isHidden()).toEqual(true)

      // Second question has errors.
      error = await pageObject.$('.cf-address-street-1-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-city-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-state-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
      error = await pageObject.$('.cf-address-zip-error >> nth=1')
      expect(await error.isHidden()).toEqual(false)
    })
  })

  // One optional address followed by one required address.
  describe('optional address question', () => {
    let applicantQuestions
    const programName = 'test program for optional address'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

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
        'address-test-optional-q'
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with valid required address does submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'Ames',
        '54321',
        1
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    describe('with invalid required address', () => {
      beforeEach(async () => {
        await loginAsGuest(pageObject)
        await selectApplicantLanguage(pageObject, 'English')

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
        await applicantQuestions.clickNext()
      })

      it('does not submit', async () => {
        // Second question has errors.
        let error = await pageObject.$('.cf-address-street-1-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = await pageObject.$('.cf-address-city-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = await pageObject.$('.cf-address-state-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
        error = await pageObject.$('.cf-address-zip-error >> nth=1')
        expect(await error.isHidden()).toEqual(false)
      })

      it('optional has no errors', async () => {
        await loginAsGuest(pageObject);
        await selectApplicantLanguage(pageObject, 'English');

        await applicantQuestions.applyProgram(programName);
        await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1);
        await applicantQuestions.clickNext();

        // First question has no errors.
        let error = await pageObject.$('.cf-address-street-1-error >> nth=0');
        expect(await error.isHidden()).toEqual(true);
        error = await pageObject.$('.cf-address-city-error >> nth=0');
        expect(await error.isHidden()).toEqual(true);
        error = await pageObject.$('.cf-address-state-error >> nth=0');
        expect(await error.isHidden()).toEqual(true);
        error = await pageObject.$('.cf-address-zip-error >> nth=0');
        expect(await error.isHidden()).toEqual(true);
      })
    })
  })
})
