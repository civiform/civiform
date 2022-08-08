import { AdminPredicates, AdminPrograms, AdminQuestions, ApplicantQuestions, endSession, loginAsAdmin, loginAsProgramAdmin, loginAsTestUser, logout, selectApplicantLanguage, startSession, userDisplayName, validateScreenshot, } from './support'

describe('create and edit predicates', () => {
  it('add a hide predicate', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    // Add a program with two screens
    await adminQuestions.addTextQuestion({ questionName: 'hide-predicate-q' })
    await adminQuestions.addTextQuestion({
      questionName: 'hide-other-q',
      description: 'desc',
      questionText: 'conditional question',
    })

    const programName = 'create hide predicate'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'first screen', [
      'hide-predicate-q',
    ])
    await adminPrograms.addProgramBlock(programName, 'screen with predicate', [
      'hide-other-q',
    ])

    // Edit predicate for second block
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Screen 2')
    const adminPredicates = new AdminPredicates(page)
    await adminPredicates.addPredicate(
      'hide-predicate-q',
      'hidden if',
      'text',
      'is equal to',
      'hide me'
    )
    await adminPredicates.expectVisibilityConditionEquals(
      'Screen 2 is hidden if hide-predicate-q\'s text is equal to "hide me"'
    )
    await validateScreenshot(page)

    // Publish the program
    await adminPrograms.publishProgram(programName)

    // Switch to the applicant view and apply to the program
    await logout(page)
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')
    const applicant = new ApplicantQuestions(page)
    await applicant.applyProgram(programName)

    // Initially fill out the first screen so that the next screen will be shown
    await applicant.answerTextQuestion('show me')
    await applicant.clickNext()

    // Fill out the second screen
    await applicant.answerTextQuestion('will be hidden and not submitted')
    await applicant.clickNext()

    // We should be on the review page, with an answer to Screen 2's question
    expect(await page.innerText('#application-summary')).toContain(
      'conditional question'
    )

    // Return to the first screen and answer it so that the second screen is hidden
    await page.click('text=Edit') // first screen edit
    await applicant.answerTextQuestion('hide me')
    await applicant.clickNext()

    // We should be on the review page
    expect(await page.innerText('#application-summary')).not.toContain(
      'conditional question'
    )
    await applicant.submitFromReviewPage(programName)

    // Visit the program admin page and assert the hidden question does not show
    await logout(page)
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)
    await adminPrograms.viewApplicationForApplicant(userDisplayName())

    const applicationText = await adminPrograms
      .applicationFrame()
      .locator('#application-view')
      .innerText()
    expect(applicationText).not.toContain('Screen 2')
    await validateScreenshot(page)

    await endSession(browser)
  })

  it('add a show predicate', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    // Add a program with two screens
    await adminQuestions.addTextQuestion({ questionName: 'show-predicate-q' })
    await adminQuestions.addTextQuestion({
      questionName: 'show-other-q',
      description: 'desc',
      questionText: 'conditional question',
    })

    const programName = 'create show predicate'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'first screen', [
      'show-predicate-q',
    ])
    await adminPrograms.addProgramBlock(programName, 'screen with predicate', [
      'show-other-q',
    ])

    // Edit predicate for second screen
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Screen 2')
    const adminPredicates = new AdminPredicates(page)
    await adminPredicates.addPredicate(
      'show-predicate-q',
      'shown if',
      'text',
      'is equal to',
      'show me'
    )
    await adminPredicates.expectVisibilityConditionEquals(
      'Screen 2 is shown if show-predicate-q\'s text is equal to "show me"'
    )
    await validateScreenshot(page)

    // Publish the program
    await adminPrograms.publishProgram(programName)

    // Switch to the applicant view and apply to the program
    await logout(page)
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')
    const applicant = new ApplicantQuestions(page)
    await applicant.applyProgram(programName)

    // Initially fill out the first screen so that the next screen will be hidden
    await applicant.answerTextQuestion('hide next screen')
    await applicant.clickNext()

    // We should be on the review page, with no Screen 2 questions shown. We should
    // be able to submit the application
    expect(await page.innerText('#application-summary')).not.toContain(
      'conditional question'
    )
    expect((await page.innerText('.cf-submit-button')).toLowerCase()).toContain(
      'submit'
    )
    await validateScreenshot(page)

    // Return to the first screen and answer it so that the second screen is shown
    await page.click('text=Edit') // first screen edit
    await applicant.answerTextQuestion('show me')
    await applicant.clickNext()

    // The second screen should now appear, and we must fill it out
    await applicant.answerTextQuestion('hello world!')
    await applicant.clickNext()

    // We should be on the review page
    expect(await page.innerText('#application-summary')).toContain(
      'conditional question'
    )
    await applicant.submitFromReviewPage(programName)

    // Visit the program admin page and assert the conditional question is shown
    await logout(page)
    await loginAsProgramAdmin(page)
    await adminPrograms.viewApplications(programName)

    await adminPrograms.viewApplicationForApplicant(userDisplayName())
    expect(
      await adminPrograms
        .applicationFrame()
        .locator('#application-view')
        .innerText()
    ).toContain('Screen 2')
    await validateScreenshot(page)

    await endSession(browser)
  })

  it('every right hand type evaluates correctly', async () => {
    const { browser, page } = await startSession()

    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    // DATE, STRING, LONG, LIST_OF_STRINGS, LIST_OF_LONGS
    await adminQuestions.addNameQuestion({ questionName: 'single-string' })
    await adminQuestions.addTextQuestion({ questionName: 'list of strings' })
    await adminQuestions.addNumberQuestion({ questionName: 'single-long' })
    await adminQuestions.addNumberQuestion({ questionName: 'list of longs' })
    await adminQuestions.addDateQuestion({ questionName: 'predicate-date' })
    await adminQuestions.addCheckboxQuestion({
      questionName: 'both sides are lists',
      options: ['dog', 'rabbit', 'cat'],
    })
    await adminQuestions.addTextQuestion({
      questionName: 'depends on previous',
    })

    const programName = 'test all predicate types'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName, 'string', [
      'single-string',
    ])
    await adminPrograms.addProgramBlock(programName, 'list of strings', [
      'list of strings',
    ])
    await adminPrograms.addProgramBlock(programName, 'long', ['single-long'])
    await adminPrograms.addProgramBlock(programName, 'list of longs', [
      'list of longs',
    ])
    await adminPrograms.addProgramBlock(programName, 'date', ['predicate-date'])
    await adminPrograms.addProgramBlock(programName, 'two lists', [
      'both sides are lists',
    ])
    await adminPrograms.addProgramBlock(programName, 'last', [
      'depends on previous',
    ])

    // Simple string predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Screen 2')
    const adminPredicates = new AdminPredicates(page)
    await adminPredicates.addPredicate(
      'single-string',
      'shown if',
      'first name',
      'is not equal to',
      'hidden'
    )

    // Single string one of a list of strings
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Screen 3')
    await adminPredicates.addPredicate(
      'list of strings',
      'shown if',
      'text',
      'is one of',
      'blue, green'
    )

    // Simple long predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Screen 4')
    await adminPredicates.addPredicate(
      'single-long',
      'shown if',
      'number',
      'is equal to',
      '42'
    )

    // Single long one of a list of longs
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Screen 5')
    await adminPredicates.addPredicate(
      'list of longs',
      'shown if',
      'number',
      'is one of',
      '123, 456'
    )

    // Date predicate
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Screen 6')
    await adminPredicates.addPredicate(
      'predicate-date',
      'shown if',
      'date',
      'is earlier than',
      '2021-01-01'
    )

    // Lists of strings on both sides (multi-option question checkbox)
    await adminPrograms.goToEditBlockPredicatePage(programName, 'Screen 7')
    await adminPredicates.addPredicate(
      'both sides are lists',
      'shown if',
      'selections',
      'contains any of',
      'dog,cat'
    )

    await adminPrograms.publishProgram(programName)

    // Switch to applicant view - if they answer each question according to the predicate,
    // the next screen will be shown.
    await logout(page)
    await loginAsTestUser(page)
    await selectApplicantLanguage(page, 'English')
    const applicant = new ApplicantQuestions(page)
    await applicant.applyProgram(programName)

    await applicant.answerNameQuestion('show', 'next', 'screen')
    await applicant.clickNext()
    await applicant.answerTextQuestion('blue')
    await applicant.clickNext()
    await applicant.answerNumberQuestion('42')
    await applicant.clickNext()
    await applicant.answerNumberQuestion('123')
    await applicant.clickNext()
    await applicant.answerDateQuestion('1998-09-04')
    await applicant.clickNext()
    await applicant.answerCheckboxQuestion(['cat'])
    await applicant.clickNext()
    await applicant.answerTextQuestion('last one!')
    await applicant.clickNext()

    // We should now be on the summary page
    await validateScreenshot(page)
    await applicant.submitFromReviewPage(programName)
    await endSession(browser)
  })
})
