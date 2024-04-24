import {test, expect} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'
import {ProgramVisibility} from '../support/admin_programs'

test.describe(
  'publishing all draft questions and programs',
  {tag: ['@uses-fixtures']},
  () => {
    const hiddenProgramNoQuestions = 'Public test program hidden no questions'
    const visibleProgramWithQuestion =
      'Public test program visible with question'
    const questionName = 'publish-test-address-q'
    const questionText = 'publish-test-address-q'
    // CreateNewVersion implicitly updates the question text to be suffixed with " new version".
    const draftQuestionText = `${questionText} new version`

    test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
      // beforeAll
      await loginAsAdmin(page)

      // Create a hidden program with no questions
      await adminPrograms.addProgram(
        hiddenProgramNoQuestions,
        'program description',
        'https://usa.gov',
        ProgramVisibility.HIDDEN,
      )

      // Create a new question referenced by a program.
      await adminQuestions.addAddressQuestion({questionName, questionText})
      await adminPrograms.addProgram(visibleProgramWithQuestion)
      await adminPrograms.editProgramBlock(
        visibleProgramWithQuestion,
        'dummy description',
        [questionName],
      )

      // Publish.
      await adminPrograms.publishAllDrafts()

      // Make an edit to the program with no questions.
      await adminPrograms.createNewVersion(hiddenProgramNoQuestions)

      // Make an edit to the shared question.
      await adminQuestions.createNewVersion(questionName)

      await adminPrograms.gotoAdminProgramsPage()
      // beforeEach
    })

    test('shows programs and questions that will be published in the modal', async ({
      adminPrograms,
    }) => {
      await adminPrograms.expectProgramReferencesModalContains({
        expectedQuestionsContents: [`${draftQuestionText} - Edit`],
        expectedProgramsContents: [
          `${hiddenProgramNoQuestions} (Hidden from applicants) Edit`,
          `${visibleProgramWithQuestion} (Publicly visible) Edit`,
        ],
      })
    })

    test('validate screenshot', async ({adminPrograms}) => {
      await adminPrograms.openPublishAllDraftsModal()
      await validateScreenshot(
        adminPrograms.publishAllProgramsModalLocator(),
        'publish-modal',
      )
    })
  },
)

test.describe(
  'publishing all programs with disabled programs feature flag on',
  {tag: ['@uses-fixtures']},
  () => {
    const disabledProgram = 'Disabled test program'
    const publicProgram = 'Public test program'
    const questionName = 'admin-publish-test-address-q'
    const questionText = 'admin-publish-test-address-q'
    // CreateNewVersion implicitly updates the question text to be suffixed with " new version".
    const draftQuestionText = `${questionText} new version`

    test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
      await enableFeatureFlag(page, 'disabled_visibility_condition_enabled')
      await  loginAsAdmin(page)

      //Create a disabled program
      await adminPrograms.addDisabledProgram(disabledProgram)

      //Create a new question referenced by a program
      await adminQuestions.addAddressQuestion({questionName, questionText})
      await adminPrograms.addProgram(publicProgram)
      await adminPrograms.editProgramBlock(
        publicProgram,
        'dummy description',
      [questionName],
      )
      // Publish.
      await adminPrograms.publishAllDrafts()

      // Make an edit to the disabled program.
      await adminPrograms.createNewVersion(disabledProgram)

      // Make an edit to the shared question.
      await adminQuestions.createNewVersion(questionName)

      await adminPrograms.gotoAdminProgramsPage()
      // beforeEach
    })

      test('shows programs and questions that will be publised in the modal, including disabled programs', async({
        page, adminPrograms,
      }) => {
        await adminPrograms.openPublishAllDraftsModal()
        await adminPrograms.expectProgramReferencesModalContains({
          expectedQuestionsContents: [`${draftQuestionText} - Edit`],
          expectedProgramsContents: [
            `${disabledProgram} (Hidden from applicants and TIs) Edit`,
            `${publicProgram} (Publicly visible) Edit`,
          ],
        })
        await validateScreenshot( page,
          'publish-modal-including-disabled-programs',)
      })

  }
)

test.describe(
  'publishing all programs with universal questions feature flag on',
  {tag: ['@uses-fixtures']},
  () => {
    test('shows a modal with information about universal questions', async ({
      page,
      adminPrograms,
      adminQuestions,
    }) => {
      const programOne = 'program one'
      const programTwo = 'program two'
      const nameQuestion = 'name'
      const textQuestion = 'text'
      const addressQuestion = 'address'

      await loginAsAdmin(page)

      await test.step('Create programs', async () => {
        await adminPrograms.addProgram(programOne)
        await adminPrograms.addProgram(programTwo)
      })

      await test.step('Create questions', async () => {
        await adminQuestions.addNameQuestion({
          questionName: nameQuestion,
          universal: true,
        })

        await adminQuestions.addTextQuestion({
          questionName: textQuestion,
          universal: true,
        })

        await adminQuestions.addAddressQuestion({
          questionName: addressQuestion,
          universal: false,
        })
      })

      await test.step('Add questions to programs', async () => {
        await adminPrograms.gotoEditDraftProgramPage(programOne)
        await adminPrograms.addQuestionFromQuestionBank(nameQuestion)
        await adminPrograms.addQuestionFromQuestionBank(textQuestion)
        await adminPrograms.gotoEditDraftProgramPage(programTwo)
        await adminPrograms.addQuestionFromQuestionBank(nameQuestion)
        await adminPrograms.addQuestionFromQuestionBank(addressQuestion)
      })

      await test.step('Trigger the modal', async () => {
        await adminPrograms.gotoAdminProgramsPage()
        await page.click('#publish-all-programs-modal-button')

        expect(await page.innerText('#publish-all-programs-modal')).toContain(
          'program one (Publicly visible) - Contains all universal questions',
        )

        expect(await page.innerText('#publish-all-programs-modal')).toContain(
          'program two (Publicly visible) - Contains 1 of 2 universal questions',
        )

        await validateScreenshot(page, 'publish-all-programs-modal-with-uq')
      })

      await test.step('Publish the programs', async () => {
        await adminQuestions.clickSubmitButtonAndNavigate(
          'Publish all draft programs and questions',
        )
      })

      await test.step('Assert program data', async () => {
        await adminPrograms.expectDoesNotHaveDraftProgram(programOne)
        await adminPrograms.expectDoesNotHaveDraftProgram(programTwo)
        await adminPrograms.expectActiveProgram(programOne)
        await adminPrograms.expectActiveProgram(programTwo)
      })
    })
  },
)
