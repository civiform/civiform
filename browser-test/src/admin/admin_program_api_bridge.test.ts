import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  isLocalDevEnvironment,
  loginAsAdmin,
  validateAccessibility,
  validateScreenshot,
  validateToastMessage,
  waitForPageJsLoad,
} from '../support'
import {ProgramBridgeConfigurationPage} from '../page/admin/programs/program_bridge_configuration_page'
import {MOCK_WEB_SERVICES_URL} from '../support/config'
import {NavigationOption} from '../support/admin_programs'

test.describe('program api bridge', () => {
  test.skip(!isLocalDevEnvironment(), 'Requires mock-web-services')

  const programName = 'Comprehensive Sample Program'
  const hostUrl = `${MOCK_WEB_SERVICES_URL}/api-bridge`
  const urlPath = '/bridge/success'

  test.beforeEach(
    async ({page, adminPrograms, bridgeDiscoveryPage, seeding}) => {
      await enableFeatureFlag(page, 'api_bridge_enabled')
      await loginAsAdmin(page)

      await bridgeDiscoveryPage.clickPrimaryNavSubMenuLink(
        'API',
        'Bridge Discovery',
      )
      await bridgeDiscoveryPage.fillUrl(hostUrl)
      await bridgeDiscoveryPage.clickSearchButton()
      await bridgeDiscoveryPage.clickAddButton(urlPath)

      await seeding.seedProgramsAndCategories()
      await adminPrograms.gotoEditDraftProgramPage(programName)
    },
  )

  test('configure api bridge on a program', async ({page, adminPrograms}) => {
    const programBridgeConfiguration = new ProgramBridgeConfigurationPage(page)

    await test.step('go to bridge edit page', async () => {
      await adminPrograms.clickEditBridgeDefinitionButton()
    })

    await test.step('select api bridge', async () => {
      await programBridgeConfiguration.changeBridgeAdminName('bridge-success')
      await expect(
        programBridgeConfiguration.getInputFieldsHeading(),
      ).toBeVisible()
      await expect(
        programBridgeConfiguration.getOutputFieldsHeading(),
      ).toBeVisible()
    })

    await test.step('fill out form', async () => {
      async function setInputBinding(
        externalName: ExternalName,
        questionName: QuestionName,
        scalarName: ScalarName,
      ) {
        await programBridgeConfiguration.setInputQuestion(
          externalName,
          questionName,
        )
        await programBridgeConfiguration.setInputScalar(
          externalName,
          scalarName,
        )
      }

      async function setOutputBinding(
        externalName: ExternalName,
        questionName: QuestionName,
        scalarName: ScalarName,
      ) {
        await programBridgeConfiguration.setOutputQuestion(
          externalName,
          questionName,
        )
        await programBridgeConfiguration.setOutputScalar(
          externalName,
          scalarName,
        )
      }

      await setInputBinding(
        ExternalName.ZipCode,
        QuestionName.SampleAddressQuestion,
        ScalarName.ZipCode,
      )
      await setInputBinding(
        ExternalName.AccountNumber,
        QuestionName.SampleNumberQuestion,
        ScalarName.Number,
      )
      await setOutputBinding(
        ExternalName.IsValid,
        QuestionName.SampleRadioButtonQuestion,
        ScalarName.Selection,
      )
      await setOutputBinding(
        ExternalName.AccountNumber,
        QuestionName.SampleNumberQuestion,
        ScalarName.Number,
      )
    })

    await test.step('save', async () => {
      await programBridgeConfiguration.save()
      await waitForPageJsLoad(page)
    })

    await test.step('verify block cannot be removed', async () => {
      await adminPrograms.removeCurrentBlock()
      await validateToastMessage(
        page,
        'This screen cannot be removed while any of its questions are used by the API bridge',
      )
    })

    await test.step('verify block cannot be removed', async () => {
      await adminPrograms.removeQuestionFromProgram(
        programName,
        'Screen 1',
        ['Sample Address Question'],
        NavigationOption.SKIP_EXCESSIVE_NAVIGATION,
      )
      await validateToastMessage(
        page,
        'This question cannot be removed while used by the API bridge',
      )
    })

    await test.step('reload bridge edit page', async () => {
      await adminPrograms.clickEditBridgeDefinitionButton()
      await programBridgeConfiguration.changeBridgeAdminName('bridge-success')
    })

    await test.step('verify saved fields reload correctly', async () => {
      async function assertInputBinding(
        externalName: ExternalName,
        questionName: QuestionName,
        scalarName: ScalarName,
      ) {
        await expect(
          programBridgeConfiguration.getInputQuestion(externalName),
        ).toHaveValue(questionName)

        await expect(
          programBridgeConfiguration.getInputScalar(externalName),
        ).toHaveValue(scalarName)
      }

      async function assertOutputBinding(
        externalName: ExternalName,
        questionName: QuestionName,
        scalarName: ScalarName,
      ) {
        await expect(
          programBridgeConfiguration.getOutputQuestion(externalName),
        ).toHaveValue(questionName)

        await expect(
          programBridgeConfiguration.getOutputScalar(externalName),
        ).toHaveValue(scalarName)
      }

      await assertInputBinding(
        ExternalName.ZipCode,
        QuestionName.SampleAddressQuestion,
        ScalarName.ZipCode,
      )
      await assertInputBinding(
        ExternalName.AccountNumber,
        QuestionName.SampleNumberQuestion,
        ScalarName.Number,
      )
      await assertOutputBinding(
        ExternalName.IsValid,
        QuestionName.SampleRadioButtonQuestion,
        ScalarName.Selection,
      )
      await assertOutputBinding(
        ExternalName.AccountNumber,
        QuestionName.SampleNumberQuestion,
        ScalarName.Number,
      )
    })

    await validateAccessibility(page)
    await validateScreenshot(page, 'admin-program-api-bridge')
  })
})

enum ExternalName {
  AccountNumber = 'Account Number',
  IsValid = 'Is Valid',
  ZipCode = 'ZIP Code',
}

enum QuestionName {
  SampleAddressQuestion = 'Sample_Address_Question',
  SampleNumberQuestion = 'Sample_Number_Question',
  SampleRadioButtonQuestion = 'Sample_Radio_Button_Question',
}

enum ScalarName {
  Number = 'NUMBER',
  Selection = 'SELECTION',
  ZipCode = 'ZIP',
}
