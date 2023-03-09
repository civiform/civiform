import {
  createTestContext,
  isHermeticTestEnvironment,
  loginAsAdmin,
  loginAsGuest,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  selectApplicantLanguage,
  testUserDisplayName,
  waitForPageJsLoad,
  validateScreenshot,
} from './support'

describe('Program admin review of submitted applications', () => {
  const ctx = createTestContext()

  it('all major steps', async () => {
    const {page, adminQuestions, adminPrograms, applicantQuestions} = ctx

    await loginAsAdmin(page)

    await adminQuestions.addDateQuestion({questionName: 'date-q'})
    await adminQuestions.addEmailQuestion({questionName: 'email-q'})
    await adminQuestions.addDropdownQuestion({
      questionName: 'ice-cream-q',
      options: ['chocolate', 'banana', 'black raspberry'],
    })
    await adminQuestions.addCheckboxQuestion({
      questionName: 'favorite-trees-q',
      options: ['oak', 'maple', 'pine', 'cherry'],
    })
    await adminQuestions.addCheckboxQuestion({
      questionName: 'favorite-rats-q',
      options: ['sewage', 'laboratory', 'bubonic', 'giant'],
    })
    await adminQuestions.addCheckboxQuestion({
      questionName: 'scared-of-q',
      options: ['dogs', 'bees', 'spiders', 'the dark', 'clowns'],
    })
    await adminQuestions.addCurrencyQuestion({
      questionName: 'monthly-income-q',
    })
    await adminQuestions.addAddressQuestion({questionName: 'address-q'})
    await adminQuestions.addFileUploadQuestion({
      questionName: 'fileupload-q',
    })
    await adminQuestions.addNameQuestion({questionName: 'name-q'})
    await adminQuestions.addNumberQuestion({questionName: 'number-q'})
    await adminQuestions.addTextQuestion({questionName: 'text-q'})
    await adminQuestions.addRadioButtonQuestion({
      questionName: 'radio-q',
      options: ['one', 'two', 'three'],
    })
    await adminQuestions.addStaticQuestion({questionName: 'first-static-q'})
    await adminQuestions.addStaticQuestion({questionName: 'second-static-q'})

    const programName = 'A shiny new program'
    await adminPrograms.addProgram(programName)

    await adminPrograms.editProgramBlock(programName, 'block description', [
      'date-q',
      'address-q',
      'name-q',
      'radio-q',
      'email-q',
    ])
    await adminPrograms.addProgramBlock(programName, 'another description', [
      'ice-cream-q',
      'favorite-trees-q',
      'number-q',
      'text-q',
    ])
    await adminPrograms.addProgramBlock(programName, 'third description', [
      'fileupload-q',
    ])
    await adminPrograms.addProgramBlock(programName, 'fourth description', [
      'scared-of-q',
      'favorite-rats-q',
      'first-static-q',
    ])
    await adminPrograms.addProgramBlock(programName, 'fifth description', [
      'second-static-q',
      'monthly-income-q',
    ])

    // Intentionally add an empty block to ensure that empty blocks do not
    // prevent applicants from being able to submit applications.
    await adminPrograms.addProgramBlock(programName, 'empty block')

    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.expectDraftProgram(programName)

    await adminPrograms.publishProgram(programName)
    await adminPrograms.expectActiveProgram(programName)

    await adminQuestions.expectActiveQuestionExist('ice-cream-q')
    await adminQuestions.expectActiveQuestionExist('favorite-trees-q')
    await adminQuestions.expectActiveQuestionExist('favorite-rats-q')
    await adminQuestions.expectActiveQuestionExist('scared-of-q')
    await adminQuestions.expectActiveQuestionExist('address-q')
    await adminQuestions.expectActiveQuestionExist('name-q')
    await adminQuestions.expectActiveQuestionExist('date-q')
    await adminQuestions.expectActiveQuestionExist('number-q')
    await adminQuestions.expectActiveQuestionExist('text-q')
    await adminQuestions.expectActiveQuestionExist('radio-q')
    await adminQuestions.expectActiveQuestionExist('email-q')
    await adminQuestions.expectActiveQuestionExist('first-static-q')
    await adminQuestions.expectActiveQuestionExist('second-static-q')
    await adminQuestions.expectActiveQuestionExist('monthly-income-q')

    await adminQuestions.goToViewQuestionPage('date-q')

    await logout(page)
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')

    await applicantQuestions.validateHeader('en-US')

    // fill 1st application block.
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.answerAddressQuestion('', '', '', '', '')
    await applicantQuestions.answerNameQuestion('', '', '')
    await applicantQuestions.answerRadioButtonQuestion('two')
    await applicantQuestions.answerDateQuestion('2021-05-10')
    await applicantQuestions.answerEmailQuestion('test1@gmail.com')
    await applicantQuestions.clickNext()

    // Application doesn't progress because of name and address question errors.
    // Verify that address error messages are visible.
    expect(await page.innerText('.cf-address-street-1-error:visible')).toEqual(
      'Please enter valid street name and number.',
    )
    expect(await page.innerText('.cf-address-city-error:visible')).toEqual(
      'Please enter city.',
    )
    expect(await page.innerText('.cf-address-state-error:visible')).toEqual(
      'Please enter state.',
    )
    expect(await page.innerText('.cf-address-zip-error:visible')).toEqual(
      'Please enter valid 5-digit ZIP code.',
    )

    // Verify that name question error messages are visible.
    expect(await page.innerText('.cf-name-first-error:visible')).toEqual(
      'Please enter your first name.',
    )
    expect(await page.innerText('.cf-name-last-error:visible')).toEqual(
      'Please enter your last name.',
    )

    // Fix the address and name questions and submit.
    await applicantQuestions.answerNameQuestion('Queen', 'Hearts', 'of')
    await applicantQuestions.answerAddressQuestion(
      '1234 St',
      'Unit B',
      'Sim',
      'WA',
      '54321',
    )
    await applicantQuestions.clickNext()

    // fill 2nd application block.
    await applicantQuestions.answerDropdownQuestion('banana')
    await applicantQuestions.answerCheckboxQuestion(['cherry', 'pine'])
    await applicantQuestions.answerNumberQuestion('42')
    await applicantQuestions.answerTextQuestion('some text')
    await applicantQuestions.clickNext()

    // fill 3rd application block.
    await applicantQuestions.answerFileUploadQuestion('file key')
    await applicantQuestions.clickNext()

    // fill 4th application block.
    await applicantQuestions.answerCheckboxQuestion(['clowns'])
    await applicantQuestions.answerCheckboxQuestion(['sewage'])
    await applicantQuestions.seeStaticQuestion('static question text')
    await applicantQuestions.clickNext()

    // verify we can see static question on 5th block.
    await applicantQuestions.seeStaticQuestion('static question text')
    await applicantQuestions.answerCurrencyQuestion('1234.56')
    await applicantQuestions.clickNext()

    // submit
    await applicantQuestions.submitFromReviewPage()

    await logout(page)
    await loginAsProgramAdmin(page)

    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    await adminPrograms.expectApplicationAnswers(
      'Screen 1',
      'address-q',
      '1234 St',
    )
    await adminPrograms.expectApplicationAnswers('Screen 1', 'name-q', 'Queen')

    // TODO: display the string values of selects instead of integer IDs
    // https://github.com/seattle-uat/civiform/issues/778
    await adminPrograms.expectApplicationAnswers('Screen 1', 'radio-q', '2')
    await adminPrograms.expectApplicationAnswers(
      'Screen 1',
      'date-q',
      '05/10/2021',
    )
    await adminPrograms.expectApplicationAnswers(
      'Screen 1',
      'email-q',
      'test1@gmail.com',
    )

    await adminPrograms.expectApplicationAnswers('Screen 2', 'ice-cream-q', '2')
    await adminPrograms.expectApplicationAnswers(
      'Screen 2',
      'favorite-trees-q',
      'pine; cherry',
    )

    await adminPrograms.expectApplicationAnswers('Screen 2', 'number-q', '42')
    await adminPrograms.expectApplicationAnswers(
      'Screen 2',
      'text-q',
      'some text',
    )
    await adminPrograms.expectApplicationAnswerLinks('Screen 3', 'fileupload-q')

    await logout(page)
    await loginAsAdmin(page)
    await adminQuestions.createNewVersion('favorite-trees-q')
    await adminQuestions.gotoQuestionEditPage('favorite-trees-q')
    await page.click('#question-settings button:has-text("Delete"):visible')
    await page.click('text=Update')
    await adminPrograms.publishProgram(programName)

    await logout(page)
    await loginAsProgramAdmin(page)

    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(testUserDisplayName())
    await adminPrograms.expectApplicationAnswers(
      'Screen 2',
      'favorite-trees-q',
      'pine; cherry',
    )
    // Expect CF logo to route ProgramAdmins back to their homepage
    await page.click('text=CF')
    await waitForPageJsLoad(page)

    await validateScreenshot(page, 'applications-page')

    await page.click('text=Reporting')

    // The reporting page is not deterministic outside the hermetic testing environment
    // so don't validate the screenshot for it when running staging probers.
    if (isHermeticTestEnvironment()) {
      await validateScreenshot(page, 'reporting-page')
    }

    await page.click(`text=${programName.replaceAll(' ', '-').toLowerCase()}`)

    if (isHermeticTestEnvironment()) {
      await validateScreenshot(page, 'program-specific-reporting-page')
    }
  })

  it('program applications listed most recent first', async () => {
    const {page, adminQuestions, adminPrograms, applicantQuestions} = ctx

    // Create a simple one question program application.
    await loginAsAdmin(page)

    await adminQuestions.addTextQuestion({questionName: 'fruit-text-q'})
    const programName = 'Fruit program'
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['fruit-text-q'],
      programName,
    )

    await logout(page)

    // Submit applications from different users.
    const answers = ['apple', 'banana', 'cherry', 'durian']
    for (const answer of answers) {
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion(answer)
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()

      await logout(page)
    }

    // Expect applications to be presented in reverse chronological order to program admin.
    await loginAsProgramAdmin(page)

    await adminPrograms.viewApplications(programName)
    for (let i = 0; i < answers.length; i++) {
      await page.click(
        `:nth-match(.cf-admin-application-card, ${i + 1}) a:text("View")`,
      )
      await adminPrograms.waitForApplicationFrame()

      // TODO(https://github.com/seattle-uat/civiform/issues/2018):
      //   make this more robust so an explicit wait time is not needed.
      await page.waitForTimeout(2000)

      await adminPrograms.expectApplicationAnswers(
        'Screen 1',
        'fruit-text-q',
        answers[answers.length - i - 1],
      )
    }
  })
})
