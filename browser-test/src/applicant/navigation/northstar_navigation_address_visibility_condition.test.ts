import {test, expect} from '../../support/civiform_fixtures'
import {
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  logout,
} from '../../support'
import {Eligibility} from '../../support/admin_programs'

test.describe('Applicant navigation flow', {tag: ['@northstar']}, () => {
  if (isLocalDevEnvironment()) {
    test.describe('using address as visibility condition', () => {
      const programName = 'Test program for address as visibility condition'
      const questionAddress = 'address-test-q'
      const questionText1 = 'text-test-q-one'
      const questionText2 = 'text-test-q-two'
      const screen1 = 'Screen 1'
      const screen2 = 'Screen 2'
      const screen3 = 'Screen 3'

      test.beforeEach(
        async ({page, adminQuestions, adminPrograms, adminPredicates}) => {
          await enableFeatureFlag(page, 'north_star_applicant_ui')
          await enableFeatureFlag(page, 'esri_address_correction_enabled')
          await enableFeatureFlag(
            page,
            'esri_address_service_area_validation_enabled',
          )

          await loginAsAdmin(page)

          // Create Questions
          await adminQuestions.addAddressQuestion({
            questionName: questionAddress,
            questionText: questionAddress,
          })

          await adminQuestions.addTextQuestion({
            questionName: questionText1,
            questionText: questionText1,
          })

          await adminQuestions.addTextQuestion({
            questionName: questionText2,
            questionText: questionText2,
          })

          // Create Program
          await adminPrograms.addProgram(programName)

          // Attach questions to program
          await adminPrograms.editProgramBlock(programName, screen1, [
            questionAddress,
          ])

          await adminPrograms.addProgramBlock(programName, screen2, [
            questionText1,
          ])

          await adminPrograms.addProgramBlock(programName, screen3, [
            questionText2,
          ])

          await adminPrograms.goToBlockInProgram(programName, screen1)

          await adminPrograms.clickAddressCorrectionToggleByName(
            questionAddress,
          )

          const addressCorrectionInput =
            adminPrograms.getAddressCorrectionToggleByName(questionAddress)

          await expect(addressCorrectionInput).toHaveValue('true')

          await adminPrograms.setProgramEligibility(
            programName,
            Eligibility.IS_NOT_GATING,
          )

          // Add address eligibility predicate
          await adminPrograms.goToEditBlockEligibilityPredicatePage(
            programName,
            screen1,
          )

          await adminPredicates.addPredicates({
            questionName: questionAddress,
            scalar: 'service area',
            operator: 'in service area',
            value: 'Seattle',
          })

          // Add the address visibility predicate
          await adminPrograms.goToBlockInProgram(programName, screen2)

          await adminPrograms.goToEditBlockVisibilityPredicatePage(
            programName,
            screen2,
          )

          await adminPredicates.addPredicates({
            questionName: questionAddress,
            action: 'shown if',
            scalar: 'service area',
            operator: 'in service area',
            value: 'Seattle',
          })

          // Publish Program
          await adminPrograms.gotoAdminProgramsPage()
          await adminPrograms.publishProgram(programName)

          await logout(page)
        },
      )

      test('when address is eligible show hidden screen', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Legit Address',
          '',
          'Seattle',
          'WA',
          '98109',
          0,
        )
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectVerifyAddressPage(true)
        await applicantQuestions.clickConfirmAddress()
        // Screen 1 will only be visible when the address is validated as being eligible. This test case uses an valid address.
        await applicantQuestions.answerTextQuestion('answer 1')
        await applicantQuestions.clickContinue()
        await applicantQuestions.answerTextQuestion('answer 2')
        await applicantQuestions.clickContinue()

        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          questionAddress,
          'Address In Area',
        )

        await applicantQuestions.clickSubmitApplication()
        await logout(page)
      })

      test('when address is not eligible do not show hidden screen', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        // Fill out application and submit.
        await applicantQuestions.answerAddressQuestion(
          'Nonlegit Address',
          '',
          'Seattle',
          'WA',
          '98109',
          0,
        )
        await applicantQuestions.clickContinue()
        await applicantQuestions.expectVerifyAddressPage(false)
        await applicantQuestions.clickConfirmAddress()
        // Screen 1 will only be visible when the address is validated as being eligible. This test case uses an invalid address.
        await applicantQuestions.answerTextQuestion('answer 2')
        await applicantQuestions.clickContinue()

        await applicantQuestions.expectQuestionAnsweredOnReviewPage(
          questionAddress,
          'Nonlegit Address',
        )

        await applicantQuestions.clickSubmitApplication()
        await logout(page)
      })
    })
  }
})
