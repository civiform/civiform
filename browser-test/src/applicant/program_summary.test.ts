import {expect, test} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
} from '../support'
import {SAMPLE_QUESTIONS} from '../support/seeding'

test.describe('Applicant navigation flow', () => {
  const programName = 'Test program for summary page'

  test.describe('navigation with five blocks', () => {
    const programDescription = 'Test description'
    const programShortDescription = 'Test short description'

    test.beforeEach(async ({page, adminPrograms, seeding}) => {
      await seeding.seedQuestions()
      await loginAsAdmin(page)

      await adminPrograms.addProgram(programName, {
        description: programDescription,
        shortDescription: programShortDescription,
      })
      await adminPrograms.editProgramBlock(programName, 'first description', [
        SAMPLE_QUESTIONS.date,
        SAMPLE_QUESTIONS.email,
      ])
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: 'second description',
        questions: [{name: SAMPLE_QUESTIONS.staticContent, isOptional: false}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: 'third description',
        questions: [{name: SAMPLE_QUESTIONS.address, isOptional: false}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: 'fourth description',
        questions: [{name: SAMPLE_QUESTIONS.radioButton, isOptional: true}],
      })
      await adminPrograms.addProgramBlockUsingSpec(programName, {
        description: 'fifth description',
        questions: [
          {name: SAMPLE_QUESTIONS.phone, isOptional: false},
          {name: SAMPLE_QUESTIONS.currency, isOptional: true},
        ],
      })

      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })

    test('Verify program summary page', async ({page, applicantQuestions}) => {
      await test.step('Apply to program', async () => {
        await applicantQuestions.applyProgram(programName)

        await applicantQuestions.answerMemorableDateQuestion(
          '2021',
          '11 - November',
          '1',
        )
        await applicantQuestions.answerEmailQuestion('test1@gmail.com')
        await applicantQuestions.clickContinue()
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
        )
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerRadioButtonQuestion('Spring')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerPhoneQuestion('4256373270')
        await applicantQuestions.clickContinue()
      })

      await test.step('Verify program summary page', async () => {
        await applicantQuestions.expectTitle(
          page,
          'Program application summary — Test program for summary page',
        )
        await expect(page.getByText(programName)).toBeVisible()
        await expect(page.getByText(programShortDescription)).toBeVisible()

        await validateAccessibility(page)
      })

      await test.step('Verify program summary page renders right to left correctly', async () => {
        await selectApplicantLanguage(page, 'ar')
        await validateScreenshot(page, 'program-summary-right-to-left', {
          fullPage: false,
          mobileScreenshot: true,
        })
      })
    })

    test('shows error toast with incomplete submission', async ({
      page,
      applicantQuestions,
    }) => {
      // Clicking "Apply" navigates to the first block edit page
      await applicantQuestions.applyProgram(programName)

      // Go to the review page
      await applicantQuestions.clickBack()

      // The UI correctly won't let us submit because the application isn't complete.
      // To fake submitting an incomplete application add a submit button and click it.
      // Note the form already triggers for the submit action.
      // A clearer way to set this up would be to have two browser contexts but that isn't doable in our setup.
      await page.evaluate(() => {
        const buttonEl = document.createElement('button')
        buttonEl.id = 'test-form-submit'
        buttonEl.type = 'submit'
        const formEl = document.querySelector('.cf-debounced-form')!
        formEl.appendChild(buttonEl)
      })
      const submitButton = page.locator('#test-form-submit')
      await submitButton.click()

      await validateToastMessage(
        page,
        "Error: There's been an update to the application",
      )
    })
  })

  test('Click to download file', async ({
    page,
    adminPrograms,
    applicantQuestions,
    seeding,
  }) => {
    const programName = 'Test program for single file upload'
    const fileName = 'foo.pdf'
    const payload = 'some sample text'
    const fileContent =
      '%PDF-1.4\n' +
      '1 0 obj\n' +
      '<< /Type /Catalog >>\n' +
      'endobj\n' +
      'trailer\n' +
      '<<>>\n' +
      '%%EOF\n' +
      payload

    await test.step('As admin, set up program', async () => {
      await seeding.seedQuestions()
      await loginAsAdmin(page)

      await adminPrograms.addAndPublishProgramWithQuestions(
        [SAMPLE_QUESTIONS.fileUpload],
        programName,
      )

      await logout(page)
    })

    await test.step('Upload file', async () => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerFileUploadQuestion(payload, fileName)
      await applicantQuestions.clickContinue()
      await applicantQuestions.gotoApplicantHomePage()
    })

    await test.step('Download file', async () => {
      await applicantQuestions.applyProgram(
        programName,
        /* showProgramOverviewPage= */ false,
      )

      await expect(page.getByText(fileName)).toBeVisible()

      const downloadedFileContent =
        await applicantQuestions.downloadSingleQuestionFromReviewPage(fileName)
      expect(downloadedFileContent).toEqual(fileContent)
    })
  })
})

test.describe('guest cannot see program summary page for login only program', () => {
  const programName = 'loginonly'

  test.beforeEach(async ({page, adminPrograms, seeding}) => {
    await test.step('create a new program', async () => {
      await seeding.seedQuestions()
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)

      await adminPrograms.editProgramBlockUsingSpec(programName, {
        description: 'First block',
        questions: [{name: SAMPLE_QUESTIONS.name, isOptional: false}],
      })
      await adminPrograms.goToProgramDescriptionPage(programName)
      await adminPrograms.setProgramToLoginOnly(true)
      await adminPrograms.submitProgramDetailsEdits()
      await adminPrograms.publishAllDrafts()
      await logout(page)
    })
  })

  test('guest user on landing in the review page, only sees the alert', async ({
    page,
    applicantQuestions,
  }) => {
    let reviewPageURL: string

    await test.step('logged in user navigates to review page', async () => {
      await loginAsTestUser(page)
      await page.goto(`/programs/${programName}`)
      await page
        .getByRole('link', {name: 'Start an application'})
        .first()
        .click()
      await applicantQuestions.answerNameQuestion('test', 'user')
      await applicantQuestions.clickContinue()

      await applicantQuestions.expectTitle(
        page,
        'Program application summary — loginonly',
      )

      reviewPageURL = page.url()
      await logout(page)
    })

    await test.step('guest user tries to navigate to review page', async () => {
      await page.goto(reviewPageURL)
      await expect(page.getByText('What is your name?')).toBeHidden()
      await expect(
        page.getByRole('heading', {
          name: 'You must log in to apply for this program',
        }),
      ).toBeVisible()
      await expect(
        page.getByText(
          'Please log in or create an account to continue with this application.',
        ),
      ).toBeVisible()
      await expect(page.getByRole('button', {name: 'Log in'})).toBeVisible()
    })
  })
})
