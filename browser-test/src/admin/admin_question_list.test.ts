import {test, expect} from '../support/civiform_fixtures'
import {
  AdminPrograms,
  AdminQuestions,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
  waitForPageJsLoad,
} from '../support'
import {Page} from '@playwright/test'

test.describe('Admin question list', () => {
  test('sorts by last updated, preferring draft over active', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
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

    await adminQuestions.gotoAdminQuestionsPage()

    // Most recently added question is on top.
    await expectQuestionListElements(page, [
      'question list test question two',
      'question list test question one',
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      'question list test question two',
      'question list test question one',
    ])

    // Publish all programs and questions, order should be maintained.
    await adminPrograms.publishAllDrafts()
    // Create a draft version of the program so that the question bank can be accessed.
    await adminPrograms.createNewVersion(programName)
    await adminQuestions.gotoAdminQuestionsPage()

    await expectQuestionListElements(page, [
      'question list test question two',
      'question list test question one',
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      'question list test question two',
      'question list test question one',
    ])

    // Now create a draft version of the previously last question. After,
    // it should be on top.
    await adminQuestions.createNewVersion(questionOnePublishedText)
    // CreateNewVersion implicitly updates the question text to be suffixed with " new version".

    await adminQuestions.gotoAdminQuestionsPage()

    await expectQuestionListElements(page, [
      'question list test question one new version',
      'question list test question two',
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      'question list test question one new version',
      'question list test question two',
    ])

    // Now create a new question, which should be on top.
    const questionThreePublishedText = 'question list test question three'
    await adminQuestions.addNameQuestion({
      questionName: questionThreePublishedText,
      questionText: questionThreePublishedText,
    })

    await adminQuestions.gotoAdminQuestionsPage()

    await expectQuestionListElements(page, [
      'question list test question three',
      'question list test question one new version',
      'question list test question two',
    ])
    await expectQuestionBankElements(programName, adminPrograms, [
      'question list test question three',
      'question list test question one new version',
      'question list test question two',
    ])
  })

  test('rendering markdown works', async ({page, adminQuestions}) => {
    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: 'a',
      questionText: '*italics*',
      helpText: '*italics help text*',
      markdown: true,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'b',
      questionText: '**bold**',
      helpText: '**bold help text**',
      markdown: true,
    })
    await adminQuestions.addTextQuestion({
      questionName: 'c',
      questionText: '[link](example.com)',
      helpText: '[help text link](example.com)',
      markdown: true,
    })

    await adminQuestions.gotoAdminQuestionsPage()
    await validateScreenshot(page, 'questions-list-markdown-rendering')
  })

  test('filters question list with search query', async ({
    page,
    adminQuestions,
  }) => {
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

    const searchLocator = page.getByRole('textbox', {name: 'Filter questions'})

    await searchLocator.fill('first')
    await expectQuestionListElements(page, ['first question'])

    await searchLocator.fill('second')
    await expectQuestionListElements(page, ['second question'])

    await searchLocator.fill('')
    await expectQuestionListElements(page, [
      'second question',
      'first question',
    ])
  })

  test('filters question list with URL param search query', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.addTextQuestion({
      questionName: 'q-f',
      questionText: 'first question',
    })
    await adminQuestions.addTextQuestion({
      questionName: 'q-s',
      questionText: 'second question',
    })

    await page.goto(`/admin/questions?filter=first`)
    await expectQuestionListElements(page, ['first question'])

    await page.goto(`/admin/questions?filter=q-`)
    await expectQuestionListElements(page, [
      'second question',
      'first question',
    ])
  })

  test('sorts question list based on selection', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)

    await test.step('setup program', async () => {
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

      await adminPrograms.addAndPublishProgramWithQuestions(
        ['a'],
        'program-one',
      )
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['a', 'c'],
        'program-two',
      )
    })

    await test.step('setup question', async () => {
      await adminQuestions.gotoAdminQuestionsPage()
    })

    await adminQuestions.sortQuestions('adminname-asc')
    await expectQuestionListElements(page, ['a', 'b', 'c'])

    await validateScreenshot(
      page,
      'questions-list-sort-dropdown-last-adminname-asc',
    )

    await adminQuestions.sortQuestions('adminname-desc')
    await expectQuestionListElements(page, ['c', 'b', 'a'])
    // Question 'b' is referenced by 0 programs, 'c' by 1 program, and 'a' by 2 programs.
    await adminQuestions.sortQuestions('numprograms-asc')
    await expectQuestionListElements(page, ['b', 'c', 'a'])
    await adminQuestions.sortQuestions('numprograms-desc')
    await expectQuestionListElements(page, ['a', 'c', 'b'])

    // Question 'c' was modified after question 'a' which was modified after 'b'.
    await adminQuestions.sortQuestions('lastmodified-desc')
    await expectQuestionListElements(page, ['c', 'a', 'b'])

    await validateScreenshot(page, 'questions-list-sort-dropdown-lastmodified')
  })

  test('shows if questions are marked for archival', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
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

  test('does not sort archived questions', async ({
    page,
    adminQuestions,
    adminPrograms,
  }) => {
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
    await expectQuestionListElements(page, [
      'question list test question one',
      'question list test question two',
      'question list test question three',
    ])
    await adminQuestions.sortQuestions('lastmodified-desc')
    await expectQuestionListElements(page, [
      'question list test question two',
      'question list test question one',
      'question list test question three',
    ])
  })

  test('persists universal state and orders questions correctly', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    // Navigate to the new question page and ensure that the universal toggle is unset
    await adminQuestions.gotoAdminQuestionsPage()
    await adminQuestions.page.click('#create-question-button')
    await adminQuestions.page.click('#create-text-question')
    await waitForPageJsLoad(adminQuestions.page)
    await expect(adminQuestions.getUniversalToggleValue()).not.toBeChecked()

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
    await expect(adminQuestions.getUniversalToggleValue()).toBeChecked()
    await adminQuestions.gotoQuestionEditPage(question2Name)
    await expect(adminQuestions.getUniversalToggleValue()).toBeChecked()
    await validateScreenshot(page, 'question-edit-universal-set')
    await adminQuestions.gotoQuestionEditPage(question3Name)
    await expect(adminQuestions.getUniversalToggleValue()).not.toBeChecked()
    await adminQuestions.gotoQuestionEditPage(question4Name)
    await expect(adminQuestions.getUniversalToggleValue()).not.toBeChecked()
    await validateScreenshot(page, 'question-edit-universal-unset')

    // Ensure ordering is correct
    await adminQuestions.gotoAdminQuestionsPage()
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      'universalTestQuestionTwo',
      'universalTestQuestionOne',
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      'universalTestQuestionFour',
      'universalTestQuestionThree',
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
      'universalTestQuestionOne',
      'universalTestQuestionTwo',
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      'universalTestQuestionThree',
      'universalTestQuestionFour',
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
    await expect(adminQuestions.getUniversalToggleValue()).not.toBeChecked()
    await adminQuestions.gotoQuestionEditPage(question3Name)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.expectAdminQuestionsPageWithUpdateSuccessToast()
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      'universalTestQuestionThree',
      'universalTestQuestionTwo',
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      'universalTestQuestionOne',
      'universalTestQuestionFour',
    ])

    // Ensure sorting by Admin ID works correctly
    await adminQuestions.sortQuestions('adminname-asc')
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      'universalTestQuestionThree',
      'universalTestQuestionTwo',
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      'universalTestQuestionFour',
      'universalTestQuestionOne',
    ])

    await adminQuestions.sortQuestions('adminname-desc')
    expect(await adminQuestions.universalQuestionNames()).toEqual([
      'universalTestQuestionTwo',
      'universalTestQuestionThree',
    ])
    expect(await adminQuestions.nonUniversalQuestionNames()).toEqual([
      'universalTestQuestionOne',
      'universalTestQuestionFour',
    ])
  })

  async function expectQuestionListElements(
    page: Page,
    expectedQuestions: string[],
  ) {
    if (expectedQuestions.length === 0) {
      throw new Error('expected at least one question')
    }

    for (const question of expectedQuestions) {
      await expect(
        page.locator('.usa-card h2').getByText(question),
      ).toBeVisible()
    }
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
    await adminPrograms.closeQuestionBank()
  }
})

test.describe('Translation tag shows up as expected', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'translation_management_improvement_enabled')
  })

  const questionName = 'Question for translation tags'
  const questionHelpText = 'Question help text'

  test('Tag translation incomplete and complete shows up as expected', async ({
    page,
    adminQuestions,
    adminTranslations,
  }) => {
    await test.step('Tag translation incomplete is visible', async () => {
      await loginAsAdmin(page)
      await adminQuestions.addTextQuestion({
        questionName,
        questionText: questionName,
        helpText: questionHelpText,
      })
      await adminQuestions.gotoAdminQuestionsPage()
      await expect(page.getByText('Translation Incomplete')).toBeVisible()
      await expect(page.getByText('Translation Complete')).toBeHidden()
    })

    await test.step('Translate the question in all languages', async () => {
      await adminQuestions.goToQuestionTranslationPage(questionName)
      const languages = [
        'Amharic',
        'Arabic',
        'Traditional Chinese',
        'French',
        'Japanese',
        'Korean',
        'Lao',
        'Russian',
        'Somali',
        'Spanish',
        'Tagalog',
        'Vietnamese',
      ]
      for (const language of languages) {
        await adminTranslations.selectLanguage(language)
        await adminTranslations.editQuestionTranslations(
          `${language} question text `,
          `${language} question help text`,
        )
      }
    })

    await test.step('Tag translation complete is visible', async () => {
      await adminQuestions.gotoAdminQuestionsPage()
      await expect(page.getByText('Translation Incomplete')).toBeHidden()
      await expect(page.getByText('Translation Complete')).toBeVisible()
      await validateScreenshot(
        page.locator('.cf-question-bank-element'),
        'question-translation-complete',
      )
    })
  })
})
