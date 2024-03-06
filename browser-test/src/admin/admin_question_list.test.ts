import {test, expect} from '../fixtures/custom_fixture'
import {
  AdminPrograms,
  AdminQuestions,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'

test.describe('Admin question list', {tag: ['@migrated']}, () => {
  test('sorts by last updated, preferring draft over active', async ({page, adminPrograms, adminQuestions} ) => {
    await loginAsAdmin(page)

    const questionOnePublishedText = 'question list test question one'
    const questionTwoPublishedText = 'question list test question two'
    await adminQuestions.addNameQuestion({
      questionName: questionOnePublishedText,
      questionText: questionOnePublishedText,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionTwoPublishedText,
      questionText: questionTwoPublishedText,
    })

    // Create a program so that the question bank can be accessed.
    const programName = 'Question list test program'
    await adminPrograms.addProgram(programName)

    // Most recently added question is on top.
    await expectQuestionListElements(adminQuestions, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])

    // Publish all programs and questions, order should be maintained.
    await adminPrograms.publishAllDrafts()
    // Create a draft version of the program so that the question bank can be accessed.
    await adminPrograms.createNewVersion(programName)
    await expectQuestionListElements(adminQuestions, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      questionTwoPublishedText,
      questionOnePublishedText,
    ])

    // Now create a draft version of the previously last question. After,
    // it should be on top.
    await adminQuestions.createNewVersion(questionOnePublishedText)
    // CreateNewVersion implicitly updates the question text to be suffixed with " new version".
    const questionOneDraftText = `${questionOnePublishedText} new version`
    await expectQuestionListElements(adminQuestions, [
      questionOneDraftText,
      questionTwoPublishedText,
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      questionOneDraftText,
      questionTwoPublishedText,
    ])

    // Now create a new question, which should be on top.
    const questionThreePublishedText = 'question list test question three'
    await adminQuestions.addNameQuestion({
      questionName: questionThreePublishedText,
      questionText: questionThreePublishedText,
    })
    await expectQuestionListElements(adminQuestions, [
      questionThreePublishedText,
      questionOneDraftText,
      questionTwoPublishedText,
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      questionThreePublishedText,
      questionOneDraftText,
      questionTwoPublishedText,
    ])
  })

  test('filters question list with search query', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: 'q-f',
      questionText: 'first question',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'q-s',
      questionText: 'second question',
    })

    await adminQuestions.gotoAdminQuestionsPage()

    await page.locator('#question-bank-filter').fill('first')
    expect(await adminQuestions.questionBankNames()).toEqual(['first question'])
    await page.locator('#question-bank-filter').fill('second')
    expect(await adminQuestions.questionBankNames()).toEqual([
      'second question',
    ])
    await page.locator('#question-bank-filter').fill('')
    expect(await adminQuestions.questionBankNames()).toEqual([
      'second question',
      'first question',
    ])
  })

  test('sorts question list based on selection', async ({page, adminQuestions, adminPrograms}) => {
    await loginAsAdmin(page)
    // Set the questionText to the same as questionName to make validation easier since questionBankNames()
    // returns the questionText. The questionText is not actually used to sort.
    await adminQuestions.addTextQuestion({
      questionName: 'b',
      questionText: 'b',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'a',
      questionText: 'a',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'c',
      questionText: 'c',
    })

    await adminPrograms.addAndPublishProgramWithQuestions(['a'], 'program-one')
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['a', 'c'],
      'program-two',
    )

    await adminQuestions.gotoAdminQuestionsPage()

    await adminQuestions.sortQuestions('adminname-asc')
    expect(await adminQuestions.questionBankNames()).toEqual(['a', 'b', 'c'])
    await validateScreenshot(
      page,
      'questions-list-sort-dropdown-last-adminname-asc',
    )
    await adminQuestions.sortQuestions('adminname-desc')
    expect(await adminQuestions.questionBankNames()).toEqual(['c', 'b', 'a'])

    // Question 'b' is referenced by 0 programs, 'c' by 1 program, and 'a' by 2 programs.
    await adminQuestions.sortQuestions('numprograms-asc')
    expect(await adminQuestions.questionBankNames()).toEqual(['b', 'c', 'a'])
    await adminQuestions.sortQuestions('numprograms-desc')
    expect(await adminQuestions.questionBankNames()).toEqual(['a', 'c', 'b'])

    // Question 'c' was modified after question 'a' which was modified after 'b'.
    await adminQuestions.sortQuestions('lastmodified-desc')
    expect(await adminQuestions.questionBankNames()).toEqual(['c', 'a', 'b'])

    await validateScreenshot(page, 'questions-list-sort-dropdown-lastmodified')
  })

  test('shows if questions are marked for archival', async ({page, adminQuestions, adminPrograms}) => {
    await loginAsAdmin(page)

    const questionOne = 'question list test question one'
    const questionTwo = 'question list test question two'
    const questionThree = 'question list test question three'
    await adminQuestions.addNameQuestion({
      questionName: questionOne,
      questionText: questionOne,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionTwo,
      questionText: questionTwo,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionThree,
      questionText: questionThree,
    })

    // Publish questions
    await adminPrograms.publishAllDrafts()

    await adminQuestions.createNewVersion(questionTwo)
    await adminQuestions.archiveQuestion({
      questionName: questionThree,
      expectModal: false,
    })

    await validateScreenshot(page, 'questions-list-with-archived-questions')
  })

  test('does not sort archived questions', async ({page, adminQuestions, adminPrograms}) => {
    await loginAsAdmin(page)

    const questionOne = 'question list test question one'
    const questionTwo = 'question list test question two'
    const questionThreeToBeArchived = 'question list test question three'
    // Set the questionText to the same as questionName to make validation easier since questionBankNames()
    // returns the questionText.
    await adminQuestions.addNameQuestion({
      questionName: questionOne,
      questionText: questionOne,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionTwo,
      questionText: questionTwo,
    })
    await adminQuestions.addNameQuestion({
      questionName: questionThreeToBeArchived,
      questionText: questionThreeToBeArchived,
    })

    // Publish questions
    await adminPrograms.publishAllDrafts()

    await adminQuestions.archiveQuestion({
      questionName: questionThreeToBeArchived,
      expectModal: false,
    })

    // Check that archived question is still at the bottom after sorting.
    await adminQuestions.sortQuestions('adminname-asc')
    expect(await adminQuestions.questionBankNames()).toEqual([
      questionOne,
      questionTwo,
      questionThreeToBeArchived,
    ])
    await adminQuestions.sortQuestions('lastmodified-desc')
    expect(await adminQuestions.questionBankNames()).toEqual([
      questionTwo,
      questionOne,
      questionThreeToBeArchived,
    ])
  })

  test('persists universal state and orders questions correctly', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)

    // Navigate to the new question page and ensure that the universal toggle is unset
    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.page.click('#create-question-button')
    await adminQuestions.page.click('#create-text-question')
    await waitForPageJsLoad(adminQuestions.page)
    expect(await adminQuestions.getUniversalToggleValue()).toEqual('false')

    const question1Name = 'universalTestQuestionOne'
    await adminQuestions.addTextQuestion({
      questionName: question1Name,
      questionText: question1Name,
      universal: true,
    })
    const question2Name = 'universalTestQuestionTwo'
    await adminQuestions.addTextQuestion({
      questionName: question2Name,
      questionText: question2Name,
      universal: true,
    })
    const question3Name = 'universalTestQuestionThree'
    await adminQuestions.addTextQuestion({
      questionName: question3Name,
      questionText: question3Name,
      universal: false,
    })
    const question4Name = 'universalTestQuestionFour'
    await adminQuestions.addTextQuestion({
      questionName: question4Name,
      questionText: question4Name,
      universal: false,
    })

    // Confirm that the previously selected universal option was saved.
    await adminQuestions.gotoQuestionEditPage(question1Name)
    expect(await adminQuestions.getUniversalToggleValue()).toEqual('true')
    await adminQuestions.gotoQuestionEditPage(question2Name)
    expect(await adminQuestions.getUniversalToggleValue()).toEqual('true')
    await validateScreenshot(page, 'question-edit-universal-set')
    await adminQuestions.gotoQuestionEditPage(question3Name)
    expect(await adminQuestions.getUniversalToggleValue()).toEqual('false')
    await adminQuestions.gotoQuestionEditPage(question4Name)
    expect(await adminQuestions.getUniversalToggleValue()).toEqual('false')
    await validateScreenshot(page, 'question-edit-universal-unset')

    // Ensure ordering is correct
    await adminQuestions.gotoAdminQuestionsPage()
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      question2Name,
      question1Name,
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      question4Name,
      question3Name,
    ])
    await validateScreenshot(page, 'universal-questions')

    // Update question1 and question 3 and ensure they now appears at the top of the list
    await adminQuestions.gotoQuestionEditPage(question1Name)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    await adminQuestions.gotoQuestionEditPage(question3Name)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      question1Name,
      question2Name,
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      question3Name,
      question4Name,
    ])

    // Make question1 non-universal and question3 universal and confirm that the new values are saved.
    await adminQuestions.gotoQuestionEditPage(question1Name)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    // Since we are toggling the universal question setting from "on" to "off", a confirmation modal will appear
    // Click the submit button on the modal to continue
    await adminQuestions.clickSubmitButtonAndNavigate(
      'Remove from universal questions',
    )
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    await adminQuestions.gotoQuestionEditPage(question1Name)
    expect(await adminQuestions.getUniversalToggleValue()).toEqual('false')
    await adminQuestions.gotoQuestionEditPage(question3Name)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      question3Name,
      question2Name,
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      question1Name,
      question4Name,
    ])

    // Ensure sorting by Admin ID works correctly
    await adminQuestions.sortQuestions('adminname-asc')
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      question3Name,
      question2Name,
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      question4Name,
      question1Name,
    ])

    await adminQuestions.sortQuestions('adminname-desc')
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      question2Name,
      question3Name,
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      question1Name,
      question4Name,
    ])
  })

  async function expectQuestionListElements(
    adminQuestions: AdminQuestions,
    expectedQuestions: string[],
  ) {
    if (expectedQuestions.length === 0) {
      throw new Error('expected at least one question')
    }
    const questionListNames = await adminQuestions.questionNames()
    expect(questionListNames).toEqual(expectedQuestions)
  }

  async function expectQuestionBankElements(
    programName: string,
    adminPrograms: AdminPrograms,
    expectedQuestions: string[],
  ) {
    if (expectedQuestions.length === 0) {
      throw new Error('expected at least one question')
    }
    await adminPrograms.gotoEditDraftProgramPage(programName)
    await adminPrograms.openQuestionBank()
    const questionBankNames = await adminPrograms.questionBankNames()
    expect(questionBankNames).toEqual(expectedQuestions)
  }
})
