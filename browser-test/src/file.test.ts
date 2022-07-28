import {Page} from 'playwright'
import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  logout,
  dropTables,
  seedCanonicalQuestions,
  resetSession,
  selectApplicantLanguage,
  startSession,
  validateAccessibility,
} from './support'

describe('file upload applicant flow', () => {
  let pageObject: Page

  beforeAll(async () => {
    const {page} = await startSession()
    await dropTables(page)
    await seedCanonicalQuestions(page)
    await resetSession(page)
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single file upload question', () => {
    let applicantQuestions: ApplicantQuestions
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
        programName,
      )

      await logout(pageObject)
    })

    it('does not show errors initially', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion('file key')
      const error = await pageObject.$('.cf-fileupload-error')
      expect(await error?.isHidden()).toEqual(true)
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
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)
      await applicantQuestions.clickUpload()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual(fileContent)
      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with no file does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickUpload()

      const error = await pageObject.$('.cf-fileupload-error')
      expect(await error?.isHidden()).toEqual(false)
    })

    // TODO(#2988) Enable test once a11y issues are fixed.
    // it('has no accessiblity violations', async () => {
    //   await loginAsGuest(pageObject)
    //   await selectApplicantLanguage(pageObject, 'English')

    //   await applicantQuestions.applyProgram(programName)

    //   await validateAccessibility(pageObject)
    // })
  })

  // Optional file upload.
  describe('optional file upload question', () => {
    let applicantQuestions: ApplicantQuestions
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
        'file-upload-test-optional-q',
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
      expect(await error?.isHidden()).toEqual(false)
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
