import {test, expect} from './support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  waitForPageJsLoad,
  enableFeatureFlag,
} from './support'
import {
  PrimaryApplicantInfoAlertType,
  PrimaryApplicantInfoField,
} from './support/admin_questions'

test.describe('primary applicant info questions', () => {
  test('shows primary applicant info toggles/alerts correctly when creating a new question, and tag is persisted', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const enumeratorName = 'name-enumerator'
    const nameQuestion = 'name-question'
    const nameField = PrimaryApplicantInfoField.APPLICANT_NAME

    // Create an enumerator
    await adminQuestions.addEnumeratorQuestion({
      questionName: enumeratorName,
    })

    // Create a new question
    await adminQuestions.gotoAdminQuestionsPage()
    await page.click('#create-question-button')
    await page.click('#create-name-question')
    await waitForPageJsLoad(page)

    await adminQuestions.fillInQuestionBasics({
      questionName: nameQuestion,
      description: 'description',
      questionText: 'text',
      helpText: 'help text',
    })

    // Verify alert shown when universal isn't set, and hidden when it is
    await adminQuestions.expectPrimaryApplicantInfoAlert(
      PrimaryApplicantInfoAlertType.NOT_UNIVERSAL,
      true,
    )
    await adminQuestions.expectPrimaryApplicantInfoToggleVisible(
      nameField,
      false,
    )
    await validateScreenshot(page, 'primary-applicant-info-universal-not-set')
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.expectPrimaryApplicantInfoAlert(
      PrimaryApplicantInfoAlertType.NOT_UNIVERSAL,
      false,
    )
    await adminQuestions.expectPrimaryApplicantInfoToggleVisible(
      nameField,
      true,
    )

    // Verify the PAI section gets hidden when an enumerator is selected, then shown when unselected
    await adminQuestions.expectPrimaryApplicantInfoSectionVisible(true)
    await page.selectOption('#question-enumerator-select', {
      label: enumeratorName,
    })
    await adminQuestions.expectPrimaryApplicantInfoSectionVisible(false)
    await page.selectOption('#question-enumerator-select', {
      label: 'does not repeat',
    })

    // Verify we can set the toggle when universal is set and tag is saved correctly
    await adminQuestions.clickPrimaryApplicantInfoToggle(nameField)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(nameField, true)
    await adminQuestions.clickSubmitButtonAndNavigate('Create')
    await adminQuestions.expectAdminQuestionsPageWithCreateSuccessToast()
    await adminQuestions.gotoQuestionEditPage(nameQuestion)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(nameField, true)
  })

  test('shows primary applicant info toggles/alerts correctly when editing an existing question, and tag is persisted', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const nameQuestion = 'name-question'
    const nameField = PrimaryApplicantInfoField.APPLICANT_NAME
    const dateQuestion = 'date-question'
    const dateField = PrimaryApplicantInfoField.APPLICANT_DOB
    const emailQuestion = 'email-question'
    const emailField = PrimaryApplicantInfoField.APPLICANT_EMAIL
    const phoneQuestion = 'phone-question'
    const phoneField = PrimaryApplicantInfoField.APPLICANT_PHONE

    // Create questions without universal/PAI
    await adminQuestions.addNameQuestion({questionName: nameQuestion})
    await adminQuestions.addDateQuestion({questionName: dateQuestion})
    await adminQuestions.addEmailQuestion({questionName: emailQuestion})
    await adminQuestions.addPhoneQuestion({questionName: phoneQuestion})

    // Edit question, verify alert/toggle shown/hidden correctly
    await adminQuestions.gotoQuestionEditPage(nameQuestion)
    await adminQuestions.expectPrimaryApplicantInfoAlert(
      PrimaryApplicantInfoAlertType.NOT_UNIVERSAL,
      true,
    )
    await adminQuestions.expectPrimaryApplicantInfoToggleVisible(
      nameField,
      false,
    )
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.expectPrimaryApplicantInfoAlert(
      PrimaryApplicantInfoAlertType.NOT_UNIVERSAL,
      false,
    )
    await adminQuestions.expectPrimaryApplicantInfoToggleVisible(
      nameField,
      true,
    )

    // Set PAI tags, save, verify tag is persisted
    await adminQuestions.clickPrimaryApplicantInfoToggle(nameField)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(nameField, true)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.gotoQuestionEditPage(nameQuestion)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(nameField, true)
    await validateScreenshot(page, 'primary-applicant-info-name')

    await adminQuestions.gotoQuestionEditPage(dateQuestion)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.clickPrimaryApplicantInfoToggle(dateField)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(dateField, true)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.gotoQuestionEditPage(dateQuestion)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(dateField, true)
    await validateScreenshot(page, 'primary-applicant-info-dob')

    await adminQuestions.gotoQuestionEditPage(emailQuestion)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.clickPrimaryApplicantInfoToggle(emailField)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(emailField, true)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.gotoQuestionEditPage(emailQuestion)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(emailField, true)
    await validateScreenshot(page, 'primary-applicant-info-email')

    await adminQuestions.gotoQuestionEditPage(phoneQuestion)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.clickPrimaryApplicantInfoToggle(phoneField)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(phoneField, true)
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.gotoQuestionEditPage(phoneQuestion)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(phoneField, true)
    await validateScreenshot(page, 'primary-applicant-info-phone')
    // Make sure unsetting the PAI tag is persisted too
    await adminQuestions.clickPrimaryApplicantInfoToggle(phoneField)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(
      phoneField,
      false,
    )
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.gotoQuestionEditPage(phoneQuestion)
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(
      phoneField,
      false,
    )

    // Verify the PAI tag gets unset when universal gets unset
    await adminQuestions.gotoQuestionEditPage(nameQuestion)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.expectPrimaryApplicantInfoAlert(
      PrimaryApplicantInfoAlertType.NOT_UNIVERSAL,
      true,
    )
    await adminQuestions.expectPrimaryApplicantInfoToggleVisible(
      nameField,
      false,
    )
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminQuestions.clickSubmitButtonAndNavigate(
      'Remove from universal questions',
    )
    await adminQuestions.gotoQuestionEditPage(nameQuestion)
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.expectPrimaryApplicantInfoToggleValue(nameField, false)
  })

  test('shows the alert when a different question has the primary applicant info tag', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)

    const paiNameQuestion = 'pai-name-question'
    const nonPaiNameQuestion = 'non-pai-name-question'
    // Create universal question with PAI tag set
    await adminQuestions.addNameQuestion({
      questionName: paiNameQuestion,
      universal: true,
      primaryApplicantInfo: true,
    })

    // Create another question of the same type, set universal, verify correct alert shown
    await adminQuestions.addNameQuestion({
      questionName: nonPaiNameQuestion,
      universal: true,
    })
    await adminQuestions.gotoQuestionEditPage(nonPaiNameQuestion)
    await adminQuestions.expectPrimaryApplicantInfoAlert(
      PrimaryApplicantInfoAlertType.TAG_SET,
      true,
    )
    await validateScreenshot(page, 'primary-applicant-info-already-set')

    // Unset universal, make sure the alert shows the appropriate text
    await adminQuestions.clickUniversalToggle()
    await adminQuestions.expectPrimaryApplicantInfoAlert(
      PrimaryApplicantInfoAlertType.TAG_SET_NOT_UNIVERSAL,
      true,
    )
    await validateScreenshot(
      page,
      'primary-applicant-info-non-universal-and-already-set',
    )
  })

  test('logging in does not overwrite name with OIDC-provided name', async ({
    page,
    adminQuestions,
    adminPrograms,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'bulk_status_update_enabled')

    await adminQuestions.addNameQuestion({
      questionName: 'name',
      universal: true,
      primaryApplicantInfo: true,
    })
    await adminPrograms.addProgram('test')
    await adminPrograms.editProgramBlock('test', 'desc', ['name'])
    await adminPrograms.gotoAdminProgramsPage()
    await adminPrograms.expectDraftProgram('test')
    await adminPrograms.publishProgram('test')
    await adminPrograms.expectActiveProgram('test')
    await adminQuestions.expectActiveQuestionExist('name')

    await logout(page)
    await loginAsTestUser(page)

    await applicantQuestions.applyProgram('test')
    await applicantQuestions.answerNameQuestion('Geordi', 'LaForge')
    await applicantQuestions.clickNext()
    await applicantQuestions.submitFromReviewPage()

    await logout(page)
    await loginAsProgramAdmin(page)

    await adminPrograms.viewApplications('test')
    await expect(
      page.locator(adminPrograms.selectApplicationRowForApplicant('LaForge')),
    ).toBeVisible()

    await logout(page)
    await loginAsTestUser(
      page,
      'a:has-text("Log in")',
      false,
      'LaForge, Geordi',
    )
    await logout(page)
    await loginAsProgramAdmin(page)

    await adminPrograms.viewApplications('test')
    await expect(
      page.locator(adminPrograms.selectApplicationRowForApplicant('LaForge')),
    ).toBeVisible()
  })
})
