import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  createBrowserContext,
  dropTables,
  loginAsAdmin,
  loginAsGuest,
  logout,
  seedCanonicalQuestions,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'
import {BASE_URL} from './support/config'

describe('file upload applicant flow', () => {
  const ctx = createBrowserContext(/* clearDb= */ false)

  beforeAll(async () => {
    await dropTables(ctx.page)
    await seedCanonicalQuestions(ctx.page)
    await ctx.page.goto(BASE_URL)
  })

  describe('single file upload question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single file upload'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['file-upload-test-q'],
        programName,
      )

      await logout(ctx.page)
    })

    it('validate screenshot', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(ctx.page, 'file')
    })

    it('validate screenshot with errors', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickUpload()

      await validateScreenshot(ctx.page, 'file-errors')
    })

    it('does not show errors initially', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion('file key')
      const error = await ctx.page.$('.cf-fileupload-error')
      expect(await error?.isHidden()).toEqual(true)
    })

    it('does not show skip button for required question', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      expect(await ctx.page.$('#fileupload-skip-button')).toBeNull()
    })

    it('with valid file does submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      const fileContent = 'some sample text'
      await applicantQuestions.answerFileUploadQuestion(fileContent)
      await applicantQuestions.clickUpload()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage()
      expect(downloadedFileContent).toEqual(fileContent)
      await applicantQuestions.submitFromReviewPage()
    })

    it('with no file does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickUpload()

      const error = await ctx.page.$('.cf-fileupload-error')
      expect(await error?.isHidden()).toEqual(false)
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(ctx.page)
    })
  })

  // Optional file upload.
  describe('optional file upload question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for optional file upload'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

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

      await logout(ctx.page)
    })

    it('with missing file can be skipped', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      // Initially clicking upload with no file provided generates
      // an error. Then we click skip to ensure that the question
      // is optional.
      await applicantQuestions.clickUpload()
      const error = await ctx.page.$('.cf-fileupload-error')
      expect(await error?.isHidden()).toEqual(false)
      await applicantQuestions.clickSkip()

      await applicantQuestions.submitFromReviewPage()
    })

    it('can be skipped', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickSkip()
      await applicantQuestions.submitFromReviewPage()
    })
  })
})
