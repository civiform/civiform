import {test} from '../support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from '../support'
test.describe(
  'view program references from question view',
  {tag: ['@uses-fixtures']},
  () => {
    test('shows no results for an unreferenced question', async ({
      page,
      adminQuestions,
    }) => {
      await loginAsAdmin(page)
      const questionName = 'unreferenced-q'
      await adminQuestions.addAddressQuestion({questionName})
      await adminQuestions.expectQuestionProgramReferencesText({
        questionName,
        expectedProgramReferencesText: 'Used in 0 programs',
        version: 'draft',
      })
    })

    test('shows results for referencing programs', async ({
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      const firstProgramName = 'First program'
      const secondProgramName = 'Second program'
      const questionName = 'question-references-q'

      await loginAsAdmin(page)
      await adminQuestions.addAddressQuestion({questionName})

      await test.step(`Create two programs and add ${questionName}`, async () => {
        // Add a reference to the question in the second block. We'll later assert
        // that the links in the modal takes us to the correct block.
        await adminPrograms.addProgram(firstProgramName)
        await adminPrograms.addProgramBlockUsingSpec(
          firstProgramName,
          'first block',
          [],
        )
        await adminPrograms.addProgramBlockUsingSpec(
          firstProgramName,
          'second block',
          [
            {
              name: questionName,
              isOptional: false,
            },
          ],
        )

        await adminPrograms.addProgram(secondProgramName)
        await adminPrograms.addProgramBlockUsingSpec(
          secondProgramName,
          'first block',
          [
            {
              name: questionName,
              isOptional: false,
            },
          ],
        )
      })

      await test.step('Verify draft question and publish', async () => {
        await adminQuestions.gotoAdminQuestionsPage()
        await adminQuestions.expectQuestionProgramReferencesText({
          questionName,
          expectedProgramReferencesText: 'Added to 2 programs.',
          version: 'draft',
        })

        await adminPrograms.publishAllDrafts()
      })

      await test.step('Add a reference from a new program in the draft version', async () => {
        const thirdProgramName = 'Third program'
        await adminPrograms.addProgram(thirdProgramName)
        await adminPrograms.addProgramBlockUsingSpec(
          thirdProgramName,
          'first block',
          [
            {
              name: questionName,
              isOptional: false,
            },
          ],
        )
      })

      await test.step('Remove question from an existing published program', async () => {
        await adminPrograms.createNewVersion(secondProgramName)
        await adminPrograms.removeQuestionFromProgram(
          secondProgramName,
          'Screen 2',
          [questionName],
        )
      })

      await test.step('Verify question and program', async () => {
        await adminQuestions.gotoAdminQuestionsPage()
        await validateScreenshot(page, 'question-used-in-programs')

        await adminQuestions.expectQuestionProgramReferencesText({
          questionName,
          expectedProgramReferencesText:
            'Used in 1 program.\n\nAdded to 1 program.\n\nRemoved from 1 program.',
          version: 'active',
        })

        await adminQuestions.expectProgramReferencesModalContains({
          questionName,
          expectedUsedProgramReferences: ['First program'],
          expectedAddedProgramReferences: ['Third program'],
          expectedRemovedProgramReferences: ['Second program'],
        })

        await adminQuestions.clickOnProgramReferencesModal(questionName)
        await validateScreenshot(page, 'question-program-modal')
      })
    })

    test('shows results for referencing disabled programs listed separately from other programs', async ({
      page,
      adminQuestions,
      adminPrograms,
    }) => {
      await enableFeatureFlag(page, 'disabled_visibility_condition_enabled')
      const programName = 'Program name'
      const disabledProgramName = 'Disabled program name'
      const questionName = 'question-references-q'

      await loginAsAdmin(page)
      await adminQuestions.addAddressQuestion({questionName})

      await test.step(`Create two programs and add ${questionName}`, async () => {
        await adminPrograms.addProgram(programName)
        await adminPrograms.addProgramBlockUsingSpec(programName, 'block', [
          {
            name: questionName,
            isOptional: false,
          },
        ])

        await adminPrograms.addDisabledProgram(disabledProgramName)
        await adminPrograms.addProgramBlockUsingSpec(
          disabledProgramName,
          'first block',
          [
            {
              name: questionName,
              isOptional: false,
            },
          ],
          /* isProgramDisabled = */ true,
        )
      })

      await test.step(`Verify question and program`, async () => {
        await adminQuestions.gotoAdminQuestionsPage()
        await validateScreenshot(page, 'question-used-in-disabled-programs')
        await adminQuestions.expectQuestionProgramReferencesText({
          questionName,
          expectedProgramReferencesText:
            'Added to 1 program in use.\n\nAdded to 1 disabled program.',
          version: 'draft',
        })
      })
    })
  },
)
