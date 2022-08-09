import { Page } from 'playwright'
import { AdminPrograms, AdminQuestions, ApplicantQuestions, loginAsAdmin, loginAsGuest, logout, resetSession, selectApplicantLanguage, startSession, validateAccessibility, validateScreenshot, waitForPageJsLoad, } from './support'

describe('End to end enumerator test', () => {
  const programName = 'ete enumerator program'
  let pageObject: Page

  beforeAll(async () => {
    const { page } = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  it('Create nested enumerator and repeated questions as admin', async () => {
    await loginAsAdmin(pageObject)
    const adminQuestions = new AdminQuestions(pageObject)
    const adminPrograms = new AdminPrograms(pageObject)

    await adminQuestions.addNameQuestion({
      questionName: 'enumerator-ete-name',
    })
    await adminQuestions.addEnumeratorQuestion({
      questionName: 'enumerator-ete-householdmembers',
      description: 'desc',
      questionText: 'Household members',
      helpText: 'list household members',
    })
    await adminQuestions.addNameQuestion({
      questionName: 'enumerator-ete-repeated-name',
      description: 'desc',
      questionText: 'Name for $this',
      helpText: 'full name for $this',
      enumeratorName: 'enumerator-ete-householdmembers',
    })
    await adminQuestions.addEnumeratorQuestion({
      questionName: 'enumerator-ete-repeated-jobs',
      description: 'desc',
      questionText: 'Jobs for $this',
      helpText: "$this's jobs",
      enumeratorName: 'enumerator-ete-householdmembers',
    })
    await adminQuestions.addNumberQuestion({
      questionName: 'enumerator-ete-repeated-jobs-income',
      description: 'desc',
      questionText: "Income for $this.parent's job at $this",
      helpText: 'Monthly income at $this',
      enumeratorName: 'enumerator-ete-repeated-jobs',
    })

    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(
      programName,
      'ete enumerator program description'
    )

    // All non-repeated questions should be available in the question bank.
    expect(await pageObject.innerText('id=question-bank-questions')).toContain(
      'enumerator-ete-name'
    )
    expect(await pageObject.innerText('id=question-bank-questions')).toContain(
      'enumerator-ete-householdmembers'
    )

    // Add an enumerator question. All options should go away.
    await pageObject.click('button:text("enumerator-ete-householdmembers")')
    expect(await pageObject.innerText('id=question-bank-questions')).toBe(
      'Question bank'
    )

    // Remove the enumerator question and add a non-enumerator question, and the enumerator option should not be in the bank.
    await pageObject.click(
      '.cf-program-question:has-text("enumerator-ete-householdmembers") >> .cf-remove-question-button'
    )
    await pageObject.click('button:text("enumerator-ete-name")')
    expect(
      await pageObject.innerText('id=question-bank-questions')
    ).not.toContain('enumerator-ete-householdmembers')

    // Create a new block with the first enumerator question, and then create a repeated block. The repeated questions should be the only options.
    await pageObject.click('#add-block-button')
    await pageObject.click('button:text("enumerator-ete-householdmembers")')
    await pageObject.click('#create-repeated-block-button')
    expect(await pageObject.innerText('id=question-bank-questions')).toContain(
      'enumerator-ete-repeated-name'
    )
    expect(await pageObject.innerText('id=question-bank-questions')).toContain(
      'enumerator-ete-repeated-jobs'
    )

    // Go back to the enumerator block, and with a repeated block, it cannot be deleted now. The enumerator question cannot be removed, either.
    await pageObject.click('p:text("Screen 2")')
    expect(
      await pageObject.getAttribute('#delete-block-button', 'disabled')
    ).not.toBeNull()
    expect(
      await pageObject.getAttribute(
        '.cf-program-question:has-text("enumerator-ete-householdmembers") >> .cf-remove-question-button',
        'disabled'
      )
    ).not.toBeNull()

    // Create the rest of the program.
    // Add repeated name question
    await pageObject.click('p:text("Screen 3")')
    await pageObject.click('button:text("enumerator-ete-repeated-name")')

    // Create another repeated block and add the nested enumerator question
    await pageObject.click('p:text("Screen 2")')
    await pageObject.click('#create-repeated-block-button')
    await pageObject.click('button:text("enumerator-ete-repeated-jobs")')

    // Create a nested repeated block and add the nested text question
    await pageObject.click('#create-repeated-block-button')
    await pageObject.click('button:text("enumerator-ete-repeated-jobs-income")')
    await validateScreenshot(pageObject)

    // Publish!
    await adminPrograms.publishProgram(programName)

    await logout(pageObject)
  })

  it('has no accessiblity violations', async () => {
    await loginAsGuest(pageObject)
    await selectApplicantLanguage(pageObject, 'English', true)
    const applicantQuestions = new ApplicantQuestions(pageObject)
    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.answerNameQuestion('Porky', 'Pig')
    await applicantQuestions.clickNext()

    // Check that we are on the enumerator page
    expect(await pageObject.isVisible('.cf-question-enumerator')).toEqual(true)
    await validateScreenshot(pageObject)

    // Validate that enumerators are accessible
    await validateAccessibility(pageObject)
  })

  it('Applicant can fill in lots of blocks, and then go back and delete some repeated entities', async () => {
    await loginAsGuest(pageObject)
    await selectApplicantLanguage(pageObject, 'English', true)
    const applicantQuestions = new ApplicantQuestions(pageObject)
    await applicantQuestions.applyProgram(programName)

    // Fill in name question
    await applicantQuestions.answerNameQuestion('Porky', 'Pig')
    await applicantQuestions.clickNext()

    // Put in two things in the enumerator question
    await applicantQuestions.addEnumeratorAnswer('Bugs')
    await applicantQuestions.addEnumeratorAnswer('Daffy')
    await applicantQuestions.clickNext()

    // FIRST REPEATED ENTITY
    // Answer name
    await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
    await applicantQuestions.clickNext()

    // Put one thing in the nested enumerator for enum one
    await applicantQuestions.addEnumeratorAnswer('Cartoon Character')
    await applicantQuestions.clickNext()

    // Answer the nested repeated question
    await applicantQuestions.answerNumberQuestion('100')
    await applicantQuestions.clickNext()

    // SECOND REPEATED ENTITY
    // Answer name
    await applicantQuestions.answerNameQuestion('Daffy', 'Duck')
    await applicantQuestions.clickNext()

    // Put an empty answer in the nested enumerator for enum two.
    await applicantQuestions.addEnumeratorAnswer('')
    await applicantQuestions.clickNext()

    // Oops! Can't have blank lines.
    // Verify that the error message is visible.
    expect(
      await pageObject.innerText('.cf-applicant-question-errors:visible')
    ).toEqual('Please enter a value for each line.')

    // Put two things in the nested enumerator for enum two
    await applicantQuestions.deleteEnumeratorEntityByIndex(1)
    await applicantQuestions.addEnumeratorAnswer('Banker')
    await applicantQuestions.addEnumeratorAnswer('Banker')
    await applicantQuestions.clickNext()

    // Oops! Can't have duplicates.
    // Verify that the error message is visible.
    expect(
      await pageObject.innerText('.cf-applicant-question-errors:visible')
    ).toEqual('Please enter a unique value for each line.')

    // Remove one of the 'Banker' entries and add 'Painter'.
    // the value attribute of the inputs isn't set, so we're clicking the second one.
    await applicantQuestions.deleteEnumeratorEntityByIndex(2)
    await applicantQuestions.addEnumeratorAnswer('Painter')
    await applicantQuestions.clickNext()

    // Answer two nested repeated text questions
    await applicantQuestions.answerNumberQuestion('31')
    await applicantQuestions.clickNext()
    await applicantQuestions.answerNumberQuestion('12')
    await applicantQuestions.clickNext()

    // Make sure the enumerator answers are in the review page
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Porky Pig'
    )
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Bugs Bunny'
    )
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Cartoon Character'
    )
    expect(await pageObject.innerText('#application-summary')).toContain('100')
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Daffy Duck'
    )
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Banker'
    )
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Painter'
    )
    expect(await pageObject.innerText('#application-summary')).toContain('31')
    expect(await pageObject.innerText('#application-summary')).toContain('12')
    await validateScreenshot(pageObject)

    // Go back to delete enumerator answers
    await pageObject.click(
      '.cf-applicant-summary-row:has(div:has-text("Household members")) a:has-text("Edit")'
    )
    await waitForPageJsLoad(pageObject)

    await applicantQuestions.deleteEnumeratorEntity('Bugs')
    await applicantQuestions.deleteEnumeratorEntity('Daffy')
    // Submit the answers by clicking next, and then go to review pageObject.
    await applicantQuestions.clickNext()
    await applicantQuestions.clickReview()

    // Make sure there are no enumerators or repeated things in the review page
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Porky Pig'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Bugs Bunny'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Cartoon Character'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      '100'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Daffy Duck'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Banker'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Painter'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      '31'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      '12'
    )

    // Go back and add an enumerator answer.
    await pageObject.click(
      '.cf-applicant-summary-row:has(div:has-text("Household members")) a:has-text("Continue")'
    )
    await waitForPageJsLoad(pageObject)
    await applicantQuestions.addEnumeratorAnswer('Tweety')
    await applicantQuestions.clickNext()
    await applicantQuestions.answerNameQuestion('Tweety', 'Bird')
    await applicantQuestions.clickNext()
    await applicantQuestions.clickReview()

    // Make sure there are no enumerators or repeated things in the review page
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Porky Pig'
    )
    expect(await pageObject.innerText('#application-summary')).toContain(
      'Tweety Bird'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Bugs Bunny'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Cartoon Character'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      '100'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Daffy Duck'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Banker'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      'Painter'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      '31'
    )
    expect(await pageObject.innerText('#application-summary')).not.toContain(
      '12'
    )
    await validateScreenshot(pageObject)

    await logout(pageObject)
  })

  it('Applicant can navigate to previous blocks', async () => {
    await loginAsGuest(pageObject)
    await selectApplicantLanguage(pageObject, 'English', true)
    const applicantQuestions = new ApplicantQuestions(pageObject)
    await applicantQuestions.applyProgram(programName)

    // Fill in name question
    await applicantQuestions.answerNameQuestion('Porky', 'Pig')
    await applicantQuestions.clickNext()

    // Put in two things in the enumerator question
    await applicantQuestions.addEnumeratorAnswer('Bugs')
    await applicantQuestions.addEnumeratorAnswer('Daffy')
    await applicantQuestions.clickNext()

    // REPEATED ENTITY
    // Answer name
    await applicantQuestions.answerNameQuestion('Bugs', 'Bunny')
    await applicantQuestions.clickNext()

    // Put one thing in the nested enumerator for enum one
    await applicantQuestions.addEnumeratorAnswer('Cartoon Character')
    await applicantQuestions.clickNext()

    // Answer the nested repeated question
    await applicantQuestions.answerNumberQuestion('100')
    await applicantQuestions.clickNext()

    // Check previous navigation works
    // Click previous and see number question
    await applicantQuestions.clickPrevious()
    await applicantQuestions.checkNumberQuestionValue('100')

    // Click previous and see enumerator question
    await applicantQuestions.clickPrevious()
    await applicantQuestions.checkEnumeratorAnswerValue('Cartoon Character', 1)

    // Click previous and see name question
    await applicantQuestions.clickPrevious()
    await applicantQuestions.checkNameQuestionValue('Bugs', 'Bunny')

    // Click previous and see enumerator question
    await applicantQuestions.clickPrevious()
    await applicantQuestions.checkEnumeratorAnswerValue('Daffy', 2)
    await applicantQuestions.checkEnumeratorAnswerValue('Bugs', 1)

    // Click previous and see name question
    await applicantQuestions.clickPrevious()
    await applicantQuestions.checkNameQuestionValue('Porky', 'Pig')
    await validateScreenshot(pageObject)

    await logout(pageObject)
  })

  it('Create new version of enumerator and update repeated questions and programs', async () => {
    await loginAsAdmin(pageObject)
    const adminQuestions = new AdminQuestions(pageObject)
    const adminPrograms = new AdminPrograms(pageObject)

    await adminQuestions.createNewVersion('enumerator-ete-householdmembers')

    // Repeated questions are updated.
    await adminQuestions.expectDraftQuestionExist(
      'enumerator-ete-repeated-name'
    )
    await adminQuestions.expectDraftQuestionExist(
      'enumerator-ete-repeated-jobs'
    )
    await adminQuestions.expectDraftQuestionExist(
      'enumerator-ete-repeated-jobs-income'
    )

    // Assert publish does not cause problem, i.e. no program refers to old questions.
    await adminPrograms.publishProgram(programName)

    await logout(pageObject)
  })
})
