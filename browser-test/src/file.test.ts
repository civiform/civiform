import {
  createTestContext,
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
  const ctx = createTestContext(/* clearDb= */ false)

  beforeAll(async () => {
    const {page} = ctx
    await dropTables(page)
    await seedCanonicalQuestions(page)
    await page.goto(BASE_URL)
  })

  describe('single file upload question', () => {
    const programName = 'test-program-for-single-file-upload'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addFileUploadQuestion({
        questionName: 'file-upload-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['file-upload-test-q'],
        programName,
      )

      await logout(page)
    })

    it('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'file')
    })

    it('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickUpload()

      await validateScreenshot(page, 'file-errors')
    })

    it('does not show errors initially', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion('file key')
      const error = await page.$('.cf-fileupload-error')
      expect(await error?.isHidden()).toEqual(true)
    })

    it('does not show skip button for required question', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      expect(await page.$('#fileupload-skip-button')).toBeNull()
    })

    it('with valid file does submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

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
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickUpload()

      const error = await page.$('.cf-fileupload-error')
      expect(await error?.isHidden()).toEqual(false)
    })

    it('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  // Optional file upload.
  describe('optional file upload question', () => {
    const programName = 'test-program-for-optional-file-upload'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

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
      await adminPrograms.publishAllPrograms()

      await logout(page)
    })

    it('with missing file can be skipped', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      // Initially clicking upload with no file provided generates
      // an error. Then we click skip to ensure that the question
      // is optional.
      await applicantQuestions.clickUpload()
      const error = await page.$('.cf-fileupload-error')
      expect(await error?.isHidden()).toEqual(false)
      await applicantQuestions.clickSkip()

      await applicantQuestions.submitFromReviewPage()
    })

    it('can be skipped', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickSkip()
      await applicantQuestions.submitFromReviewPage()
    })
  })
})
