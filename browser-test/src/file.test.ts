import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  loginAsTestUser,
  logout,
  resetSession,
  selectApplicantLanguage,
  startSession,
} from './support'

describe('file upload applicant flow', () => {
  let pageObject

  beforeAll(async () => {
    const { page } = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single file upload question', () => {
    let applicantQuestions
    const programName = 'test program for single file upload'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['file-upload-test-q'],
        programName
      )

      await logout(pageObject)
    })

    it('does not show errors initially', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion('file key')
      const error = await pageObject.$('.cf-fileupload-error')
      expect(await error.isHidden()).toEqual(true)
    })

    it('does not show skip button for required question', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      expect(await pageObject.$('#fileupload-skip-button')).toBeNull()
    })

    it('with valid file does submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion('file key')
      await applicantQuestions.clickUpload()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with no file does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickUpload()

      const error = await pageObject.$('.cf-fileupload-error')
      expect(await error.isHidden()).toEqual(false)
    })
  })

  describe('file upload shared between programs', () => {
 
    let applicantQuestions
    const programName1 = 'test program1 for single file upload'
    const programName2 = 'test program2 for single file upload'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['file-upload-test-q'],
        programName1
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['file-upload-test-q'],
        programName2
      )
      await adminPrograms.publishAllPrograms()
      await logout(pageObject)
    })
      it('test file uploads for 2 programs',async () => {
      await loginAsTestUser(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName1)
      await applicantQuestions.answerFileUploadQuestion('file key')
      await applicantQuestions.clickUpload()

      await applicantQuestions.submitFromReviewPage(programName1)

      await applicantQuestions.applyProgram(programName2)
      await applicantQuestions.answerFileUploadQuestion('file key')
      await applicantQuestions.clickUpload()
      await applicantQuestions.submitFromReviewPage(programName2)
      
      await logout(pageObject)
      await loginAsProgramAdmin(page)
      await adminPrograms.viewApplications(programName2)
      const fileContent  = getUploadedFile()
      await logout(pageObject)
    })

  })


  // Optional file upload.
  describe('optional file upload question', () => {
    let applicantQuestions
    const programName = 'test program for optional file upload'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-optional-q',
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        [],
        'file-upload-test-optional-q'
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with missing file can be skipped', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      // Initially clicking upload with no file provided generates
      // an error. Then we click skip to ensure that the question
      // is optional.
      await applicantQuestions.clickUpload()
      const error = await pageObject.$('.cf-fileupload-error')
      expect(await error.isHidden()).toEqual(false)
      await applicantQuestions.clickSkip()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('can be skipped', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickSkip()
      await applicantQuestions.submitFromReviewPage(programName)
    })
  })
})
