import {
  AdminPrograms,
  AdminQuestions,
  createTestContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
  waitForPageJsLoad,
} from './support'

describe('End to end enumerator test', () => {
  const programName = 'Ete enumerator program'
  const ctx = createTestContext(/* clearDb= */ false)

  it('Updates enumerator elements in preview', async () => {
    const {page, adminQuestions} = ctx
    await loginAsAdmin(page)

    await adminQuestions.gotoAdminQuestionsPage()

    await page.click('#create-question-button')
    await page.click('#create-enumerator-question')
    await waitForPageJsLoad(page)

    // Click the add button in the preview to ensure we get an entity row and corresponding delete
    // button.
    await page.click('button:text("Add Sample repeated entity type")')

    // Now update the text when configuring the question and ensure that
    // the preview values update.
    await page.fill('text=Repeated Entity Type', 'New entity type')

    // Verify question preview has the default values.
    await adminQuestions.expectCommonPreviewValues({
      questionText: 'Sample question text',
      questionHelpText: '',
    })
    await adminQuestions.expectEnumeratorPreviewValues({
      entityNameInputLabelText: 'New entity type name',
      addEntityButtonText: 'Add New entity type',
      deleteEntityButtonText: 'Remove New entity type',
    })
  })

  it('Create nested enumerator and repeated questions as admin', async () => {
    const {page, adminQuestions, adminPrograms} = ctx
    await loginAsAdmin(page)

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
      'ete enumerator program description',
    )

    // All non-repeated questions should be available in the question bank.
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'enumerator-ete-name',
    )
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'enumerator-ete-householdmembers',
    )

    // Add an enumerator question. All options should go away.
    await adminPrograms.addQuestionFromQuestionBank(
      'enumerator-ete-householdmembers',
    )
    expect(await page.innerText('id=question-bank-questions')).toBe('')

    // Remove the enumerator question and add a non-enumerator question, and the enumerator option should not be in the bank.
    await page.click(
      '.cf-program-question:has-text("enumerator-ete-householdmembers") >> .cf-remove-question-button',
    )
    await adminPrograms.addQuestionFromQuestionBank('enumerator-ete-name')
    expect(await page.innerText('id=question-bank-questions')).not.toContain(
      'enumerator-ete-householdmembers',
    )

    // Create a new block with the first enumerator question, and then create a repeated block. The repeated questions should be the only options.
    await page.click('#add-block-button')
    await adminPrograms.addQuestionFromQuestionBank(
      'enumerator-ete-householdmembers',
    )
    await page.click('#create-repeated-block-button')
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'enumerator-ete-repeated-name',
    )
    expect(await page.innerText('id=question-bank-questions')).toContain(
      'enumerator-ete-repeated-jobs',
    )

    // Go back to the enumerator block, and with a repeated block, it cannot be deleted now. The enumerator question cannot be removed, either.
    await page.click('p:text("Screen 2")')
    expect(
      await page.getAttribute('#block-delete-modal-button', 'disabled'),
    ).not.toBeNull()
    expect(
      await page.getAttribute(
        '.cf-program-question:has-text("enumerator-ete-householdmembers") >> .cf-remove-question-button',
        'disabled',
      ),
    ).not.toBeNull()

    // Create the rest of the program.
    // Add repeated name question
    await page.click('p:text("Screen 3")')
    await adminPrograms.addQuestionFromQuestionBank(
      'enumerator-ete-repeated-name',
    )

    // Create another repeated block and add the nested enumerator question
    await page.click('p:text("Screen 2")')
    await page.click('#create-repeated-block-button')
    await adminPrograms.addQuestionFromQuestionBank(
      'enumerator-ete-repeated-jobs',
    )

    // Create a nested repeated block and add the nested text question
    await page.click('#create-repeated-block-button')
    await adminPrograms.addQuestionFromQuestionBank(
      'enumerator-ete-repeated-jobs-income',
    )

    // Publish!
    await adminPrograms.publishProgram(programName)

    await logout(page)
  })

  it('has no accessibility violations', async () => {
    const {page, applicantQuestions} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English', true)
    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.answerNameQuestion('Porky', 'Pig')
    await applicantQuestions.clickNext()

    // Check that we are on the enumerator page
    expect(await page.isVisible('.cf-question-enumerator')).toEqual(true)

    // Validate that enumerators are accessible
    await validateAccessibility(page)

    // Adding enumerator answers causes a clone of a hidden DOM element. This element
    // should have unique IDs. If not, it will cause accessibility violations.
    // See https://github.com/civiform/civiform/issues/3565.
    await applicantQuestions.addEnumeratorAnswer('Bugs')
    await applicantQuestions.addEnumeratorAnswer('Daffy')
    await validateAccessibility(page)

    // Correspondingly, removing an element happens without a page refresh. Remove an
    // element and add another to ensure that element IDs remain unique.
    await applicantQuestions.deleteEnumeratorEntityByIndex(1)
    await applicantQuestions.addEnumeratorAnswer('Porky')
    await validateAccessibility(page)
  })

  it('validate screenshot', async () => {
    const {page, applicantQuestions} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English', true)
    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.answerNameQuestion('Porky', 'Pig')
    await applicantQuestions.clickNext()

    await applicantQuestions.addEnumeratorAnswer('Bugs')

    await validateScreenshot(page, 'enumerator')
  })

  it('validate screenshot with errors', async () => {
    const {page, applicantQuestions} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English', true)
    await applicantQuestions.applyProgram(programName)

    await applicantQuestions.answerNameQuestion('Porky', 'Pig')
    await applicantQuestions.clickNext()

    // Click next without adding an enumerator
    await applicantQuestions.clickNext()
    await validateScreenshot(page, 'enumerator-errors')
  })

  it('Applicant can fill in lots of blocks, and then go back and delete some repeated entities', async () => {
    const {page, applicantQuestions} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English', true)
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
      await page.innerText('.cf-applicant-question-errors:visible'),
    ).toEqual('Please enter a value for each line.')

    // Put two things in the nested enumerator for enum two
    await applicantQuestions.deleteEnumeratorEntityByIndex(1)
    await applicantQuestions.addEnumeratorAnswer('Banker')
    await applicantQuestions.addEnumeratorAnswer('Banker')
    await applicantQuestions.clickNext()

    // Oops! Can't have duplicates.
    // Verify that the error message is visible.
    expect(
      await page.innerText('.cf-applicant-question-errors:visible'),
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
    expect(await page.innerText('#application-summary')).toContain('Porky Pig')
    expect(await page.innerText('#application-summary')).toContain('Bugs Bunny')
    expect(await page.innerText('#application-summary')).toContain(
      'Cartoon Character',
    )
    expect(await page.innerText('#application-summary')).toContain('100')
    expect(await page.innerText('#application-summary')).toContain('Daffy Duck')
    expect(await page.innerText('#application-summary')).toContain('Banker')
    expect(await page.innerText('#application-summary')).toContain('Painter')
    expect(await page.innerText('#application-summary')).toContain('31')
    expect(await page.innerText('#application-summary')).toContain('12')

    // Go back to delete enumerator answers
    await page.click(
      '.cf-applicant-summary-row:has(div:has-text("Household members")) a:has-text("Edit")',
    )
    await waitForPageJsLoad(page)

    await applicantQuestions.deleteEnumeratorEntity('Bugs')
    // Submit the answers by clicking next, and then go to review page.
    await applicantQuestions.clickNext()

    // Make sure that the removed enumerator is not present in the review page
    expect(await page.innerText('#application-summary')).toContain('Porky Pig')
    expect(await page.innerText('#application-summary')).not.toContain(
      'Bugs Bunny',
    )
    expect(await page.innerText('#application-summary')).not.toContain(
      'Cartoon Character',
    )
    expect(await page.innerText('#application-summary')).not.toContain('100')

    // Go back and add an enumerator answer.
    await page.click(
      '.cf-applicant-summary-row:has(div:has-text("Household members")) a:has-text("Edit")',
    )
    await waitForPageJsLoad(page)
    await applicantQuestions.addEnumeratorAnswer('Tweety')
    await applicantQuestions.clickNext()
    await applicantQuestions.answerNameQuestion('Tweety', 'Bird')
    await applicantQuestions.clickNext()
    await applicantQuestions.clickReview()

    // Review page should contain Daffy Duck and newly added Tweety Bird.
    expect(await page.innerText('#application-summary')).toContain('Porky Pig')
    expect(await page.innerText('#application-summary')).toContain(
      'Tweety Bird',
    )
    expect(await page.innerText('#application-summary')).toContain('Daffy Duck')
    expect(await page.innerText('#application-summary')).toContain('Banker')
    expect(await page.innerText('#application-summary')).toContain('Painter')
    expect(await page.innerText('#application-summary')).toContain('31')
    expect(await page.innerText('#application-summary')).toContain('12')
    // Review page should not contain deleted enumerator info for Bugs Bunny.
    expect(await page.innerText('#application-summary')).not.toContain(
      'Bugs Bunny',
    )
    expect(await page.innerText('#application-summary')).not.toContain(
      'Cartoon Character',
    )
    expect(await page.innerText('#application-summary')).not.toContain('100')

    await logout(page)
  })

  it('Applicant can navigate to previous blocks', async () => {
    const {page, applicantQuestions} = ctx
    await loginAsGuest(page)
    await selectApplicantLanguage(page, 'English', true)
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

    await logout(page)
  })

  it('Create new version of enumerator and update repeated questions and programs', async () => {
    const {page} = ctx
    await loginAsAdmin(page)
    const adminQuestions = new AdminQuestions(page)
    const adminPrograms = new AdminPrograms(page)

    await adminQuestions.createNewVersion('enumerator-ete-householdmembers')

    // Repeated questions are updated.
    await adminQuestions.expectDraftQuestionExist(
      'enumerator-ete-repeated-name',
    )
    await adminQuestions.expectDraftQuestionExist(
      'enumerator-ete-repeated-jobs',
    )
    await adminQuestions.expectDraftQuestionExist(
      'enumerator-ete-repeated-jobs-income',
    )

    // Assert publish does not cause problem, i.e. no program refers to old questions.
    await adminPrograms.publishProgram(programName)

    await logout(page)
  })
})
