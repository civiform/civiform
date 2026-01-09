import {expect} from '@playwright/test'
import {Page} from '@playwright/test'
import {waitForHtmxReady} from './wait'

// For legacy predicate view.
type PredicateSpec = {
  questionName: string
  action?: string | null
  scalar: string
  operator: string
  value?: string
  values?: string[]
  complexValues?: PredicateValue[]
}

type PredicateValue = {
  value: string
  secondValue?: string
}

// For expanded predicate view.
export type SubconditionSpec = {
  conditionId: number
  subconditionId: number
  questionText: string
  scalar?: string
  operator?: string
  value: SubconditionValue
}

/**
 * For SubconditionValue, expect only three valid cases:
 * 1. firstValue is populated, rest are unpopulated.
 * 2. firstValue and secondValue are populated, multiValues is unpopulated.
 * 3. multiValues is populated, rest are unpopulated.
 */
export type SubconditionValue = {
  firstValue?: string
  secondValue?: string
  multiValues?: MultiValueSpec[]
}

export type MultiValueSpec = {
  adminName: string
  text: string
  checked: boolean
}

export class AdminPredicates {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async addValueRows(count: number) {
    for (let i = 0; i < count; i++) {
      await this.page.click('#predicate-add-value-row')
    }
  }

  async clickEditPredicateButton(predicateType: 'visibility' | 'eligibility') {
    await this.page.click(
      `button:has-text("Edit existing ${predicateType} condition")`,
    )
  }

  async clickRemovePredicateButton(
    predicateType: 'visibility' | 'eligibility',
  ) {
    await this.page.click(
      `button:has-text("Remove existing ${predicateType} condition")`,
    )
  }

  async updateEligibilityMessage(eligibilityMsg: string) {
    await this.page.getByLabel('Eligibility Message').fill(eligibilityMsg)
    await this.page
      .getByRole('button', {name: 'Save eligibility message'})
      .click()
  }

  async addPredicates(...predicateSpecs: PredicateSpec[]) {
    for (const predicateSpec of predicateSpecs) {
      await this.selectQuestionForPredicate(predicateSpec.questionName)
    }

    await this.clickAddConditionButton()
    const totalRowsNeeded = predicateSpecs[0]?.values?.length ?? 0
    await this.addValueRows(Math.max(totalRowsNeeded - 1, 0))

    for (const predicateSpec of predicateSpecs) {
      await this.configurePredicate(predicateSpec)
    }

    await this.clickSaveConditionButton()
  }

  async selectQuestionForPredicate(questionName: string) {
    await this.page.click(`label:has-text("Admin ID: ${questionName}")`)
  }

  async clickAddConditionButton() {
    await this.page.getByRole('button', {name: 'Add condition'}).click()
    await waitForHtmxReady(this.page)
  }

  async clickDeleteConditionButton(conditionId: number) {
    this.page.once('dialog', async (dialog) => {
      expect(dialog.message().toString()).toContain(
        'Are you sure you would like to delete this condition',
      )
      await dialog.accept()
    })
    await this.page
      .getByRole('button', {name: 'Delete condition'})
      .nth(conditionId - 1)
      .click()
    await waitForHtmxReady(this.page)
  }

  async clickDeleteSubconditionButton(
    conditionId: number,
    subconditionId: number,
  ) {
    await this.page
      .locator(`#condition-${conditionId}`)
      .getByRole('button', {name: 'Delete sub-condition'})
      .nth(subconditionId - 1)
      .click()
    await waitForHtmxReady(this.page)
  }

  async deleteAndExpectNoSubcondition(
    conditionId: number,
    subconditionId: number,
  ) {
    await this.clickDeleteSubconditionButton(conditionId, subconditionId)
    await this.expectNoSubcondition(conditionId, subconditionId)
  }

  async clickDeleteAllConditionsButton() {
    this.page.once('dialog', async (dialog) => {
      expect(dialog.message().toString()).toContain(
        'Are you sure you would like to delete all conditions',
      )
      await dialog.accept()
    })
    await this.page.getByRole('button', {name: 'Delete all conditions'}).click()
    await waitForHtmxReady(this.page)
  }

  async clickAddSubconditionButton(conditionId: number) {
    await this.page
      .getByRole('button', {name: 'Add sub-condition'})
      .nth(conditionId - 1)
      .click()
    await waitForHtmxReady(this.page)
  }

  async addAndExpectCondition(conditionId: number) {
    await this.clickAddConditionButton()
    await this.expectCondition(conditionId)
  }

  async addAndExpectSubcondition(conditionId: number, subconditionId: number) {
    await this.clickAddSubconditionButton(conditionId)
    await this.expectSubcondition(conditionId, subconditionId)
  }

  async selectRootLogicalOperator(logicalOperatorValue: string) {
    await this.page
      .getByRole('combobox', { name: 'root-nodeType' })
      .selectOption(logicalOperatorValue)
    await waitForHtmxReady(this.page)
  }

  async expectRootLogicalOperatorValues(logicalOperatorValue: string) {
    const conditionLogicSeparatorsText = this.page.locator(
      '.cf-predicate-condition-separator span',
    )

    expect(conditionLogicSeparatorsText.count()).not.toEqual(0)

    for (const separatorText of await conditionLogicSeparatorsText.all()) {
      await expect(separatorText).toHaveText(logicalOperatorValue.toLowerCase())
      await expect(separatorText).toBeVisible()
    }
  }

  async clickSaveAndExitButton() {
    await this.page.getByRole('button', {name: 'Save and exit'}).click()
  }

  async clickCancelButton() {
    await this.page.getByRole('button', {name: 'Cancel'}).click()
  }

  async clickSaveConditionButton() {
    await this.page.getByRole('button', {name: 'Save condition'}).click()
  }

  async expectDeleteAllConditionsButton() {
    await expect(
      this.page.locator('#delete-all-conditions-button'),
    ).toBeVisible()
  }

  async expectNoDeleteAllConditionsButton() {
    await expect(
      this.page.locator('#delete-all-conditions-button'),
    ).toBeHidden()
  }

  async expectPredicateErrorToast(type: string) {
    const toastMessages = await this.page.innerText('#toast-container')
    expect(toastMessages).toContain(`One or more ${type} is missing`)
  }

  async getQuestionId(questionName: string): Promise<string> {
    const questionNameField = this.page.getByTestId(questionName)
    expect((await questionNameField.all()).length).toEqual(1)

    const questionId = await questionNameField.getAttribute('data-question-id')
    expect(questionId).not.toBeNull()

    return questionId as string
  }

  /**
   * Configures a predicate with the given inputs. For the values, it uses the first defined parameter in this order:
   * 1. complexValues
   * 2. values
   * 3. value
   */
  async configurePredicate({
    questionName,
    action,
    scalar,
    operator,
    value,
    values,
    complexValues,
  }: PredicateSpec) {
    const questionId = await this.getQuestionId(questionName)

    if (action != null) {
      await this.page.selectOption(`.cf-predicate-action select`, {
        label: action,
      })
    }
    await this.page.selectOption(
      `.cf-scalar-select[data-question-id="${questionId}"] select`,
      {
        label: scalar,
      },
    )
    await this.page.selectOption(
      `.cf-operator-select[data-question-id="${questionId}"] select`,
      {
        label: operator,
      },
    )

    const valuesToSet = this.coalesceValueOptions(complexValues, values, value)
    let groupNum = 1
    for (const valueToSet of valuesToSet) {
      await this.legacyFillValue(scalar, valueToSet, groupNum++, questionId)
    }
  }

  /**
   * Configures an expanded form logic predicate with the given inputs. For the values, populate whatever is present.
   */
  async configureSubcondition({
    conditionId,
    subconditionId,
    questionText,
    scalar,
    operator,
    value,
  }: SubconditionSpec) {
    await this.selectQuestion(conditionId, subconditionId, questionText)

    if (scalar) {
      await this.selectScalar(conditionId, subconditionId, scalar)
    }

    if (operator) {
      await this.selectOperator(conditionId, subconditionId, operator)
    }

    await this.fillValue(conditionId, subconditionId, value)
  }

  /**
   * Configures multiple subconditions at once using the given inputs.
   */
  async configureSubconditions(subconditions: SubconditionSpec[]) {
    for (const subcondition of subconditions) {
      await this.configureSubcondition(subcondition)
    }
  }

  /**
   * Asserts the state of a given subcondition, checking selected question, scalar, operator, and value(s).
   */
  async expectSubconditionEquals({
    conditionId,
    subconditionId,
    questionText,
    scalar,
    operator,
    value,
  }: SubconditionSpec) {
    await this.expectSelectedQuestion(conditionId, subconditionId, questionText)

    if (scalar) {
      await this.expectSelectedScalar(conditionId, subconditionId, scalar)
    }

    if (operator) {
      await this.expectSelectedOperator(conditionId, subconditionId, operator)
    }

    await this.expectFilledValue(conditionId, subconditionId, value)
  }

  /**
   * Assert the state of multiple subconditions at once, using the given inputs.
   */
  async expectSubconditionsEqual(subconditions: SubconditionSpec[]) {
    for (const subcondition of subconditions) {
      await this.expectSubconditionEquals(subcondition)
    }
  }

  coalesceValueOptions(
    complexValues?: PredicateValue[],
    values?: string[],
    value?: string,
  ): PredicateValue[] {
    if (complexValues) {
      return complexValues
    }
    if (values) {
      return values.map((v) => ({value: v}))
    }
    if (value) {
      return [{value: value}]
    }
    return []
  }

  async legacyFillValue(
    scalar: string,
    valueToSet: PredicateValue,
    groupNum: number,
    questionId: string,
  ) {
    // Service areas are the only value input that use a select
    if (scalar === 'service area') {
      const valueSelect = this.page.locator(
        `select[name="group-${groupNum}-question-${questionId}-predicateValue"]`,
      )
      await valueSelect.selectOption({label: valueToSet.value})
      return
    }

    const valueInput = this.page.locator(
      `input[name="group-${groupNum}-question-${questionId}-predicateValue"]`,
    )

    if ((await valueInput.count()) > 0) {
      await valueInput.fill(valueToSet.value || '')
    } else {
      const valueArray = valueToSet.value.split(',')
      for (const value of valueArray) {
        await this.page.getByLabel(value).check()
      }
    }

    const secondValueInput = this.page.locator(
      `input[name="group-${groupNum}-question-${questionId}-predicateSecondValue"]:enabled`,
    )
    if ((await secondValueInput.count()) > 0) {
      await secondValueInput.fill(valueToSet.secondValue || '')
    }
  }

  async fillValue(
    conditionId: number,
    subconditionId: number,
    value: SubconditionValue,
  ) {
    if (value.firstValue) {
      await this.page
        .locator(
          `#condition-${conditionId}-subcondition-${subconditionId}-value:enabled`,
        )
        .fill(value.firstValue)
    }

    if (value.secondValue) {
      await this.page
        .locator(
          `#condition-${conditionId}-subcondition-${subconditionId}-secondValue:enabled`,
        )
        .fill(value.secondValue)
    } else {
      await expect(
        this.page.locator(
          `#condition-${conditionId}-subcondition-${subconditionId}-secondValue:enabled`,
        ),
      ).toHaveCount(0)
    }

    if (value.multiValues) {
      for (let count = 1; count <= value.multiValues.length; count++) {
        const checkboxLabel = this.page.locator(
          `label[for="condition-${conditionId}-subcondition-${subconditionId}-values[${count}]"]`,
        )
        const multiValueSpec: MultiValueSpec = value.multiValues[count - 1]

        if (multiValueSpec.checked) {
          await checkboxLabel.check()
          await expect(checkboxLabel).toBeChecked()
        }

        await expect(checkboxLabel).toHaveText(multiValueSpec.text)
      }
    }
  }

  async expectPredicateDisplayTextContains(condition: string) {
    expect(await this.page.innerText('.cf-display-predicate')).toContain(
      condition,
    )
  }

  async expandPredicateDisplay(predicateType: 'visibility' | 'eligibility') {
    await this.page.click(
      `button:has-text("This screen has ${predicateType} ")`,
    )
  }

  async expectCondition(conditionId: number) {
    await expect(
      this.page.locator('#predicate-conditions-list').first(),
    ).toContainText('Condition ' + conditionId)
  }

  async expectNoCondition(conditionId: number) {
    await expect(
      this.page.locator('#predicate-conditions-list').first(),
    ).not.toContainText('Condition ' + conditionId)
  }

  async expectSubcondition(conditionId: number, subconditionId: number) {
    await expect(
      this.page.locator(
        `#condition-${conditionId}-subcondition-${subconditionId}-question`,
      ),
    ).toBeVisible()
  }

  async expectConditionAndSubconditions(
    conditionId: number,
    subconditionIds: number[],
  ) {
    await this.expectCondition(conditionId)
    for (const subconditionId of subconditionIds) {
      await this.expectSubcondition(conditionId, subconditionId)
    }
  }

  async expectNoSubcondition(conditionId: number, subconditionId: number) {
    await expect(
      this.page.locator(
        `#condition-${conditionId}-subcondition-${subconditionId}-question`,
      ),
    ).toBeHidden()
  }

  async expectNoAddConditionButton() {
    await expect(
      this.page.getByRole('button', {name: 'Add condition'}),
    ).toBeHidden()
  }

  async expectAddConditionButton() {
    await expect(
      this.page.getByRole('button', {name: 'Add condition'}),
    ).toBeVisible()
  }

  async expectAddSubconditionButton(conditionId: number) {
    await expect(
      this.page
        .locator(`#condition-${conditionId}`)
        .getByRole('button', {name: 'Add sub-condition'}),
    ).toBeVisible()
  }

  async expectHtmxError() {
    await expect(
      this.page.getByText('We are experiencing a system error'),
    ).toBeVisible()
  }

  async expectEligibilityNullState() {
    await expect(
      this.page.locator('#predicate-operator-node-select-null-state'),
    ).toBeVisible()
    await expect(
      this.page.locator('#predicate-operator-node-select'),
    ).toBeHidden()
    await expect(
      this.page.locator('#predicate-operator-node-select-null-state'),
    ).toContainText('Applicant is always eligible')
  }

  async selectQuestion(
    conditionId: number,
    subconditionId: number,
    questionText: string,
  ) {
    await this.page
      .locator(
        `#condition-${conditionId}-subcondition-${subconditionId}-question`,
      )
      .selectOption(questionText)

    await waitForHtmxReady(this.page)
  }

  async selectOperator(
    conditionId: number,
    subconditionId: number,
    operatorValue: string,
  ) {
    await this.page
      .locator(
        `#condition-${conditionId}-subcondition-${subconditionId}-operator`,
      )
      .selectOption(`${operatorValue}`)

    await waitForHtmxReady(this.page)
  }

  async selectScalar(
    conditionId: number,
    subconditionId: number,
    scalarValue: string,
  ) {
    await this.page
      .locator(
        `#condition-${conditionId}-subcondition-${subconditionId}-scalar`,
      )
      .selectOption(`${scalarValue}`)

    await waitForHtmxReady(this.page)
  }

  async expectSelectedQuestion(
    conditionId: number,
    subconditionId: number,
    questionText: string,
  ) {
    const questionDropdownLocator = this.page.locator(
      `#condition-${conditionId}-subcondition-${subconditionId}-question`,
    )
    const selectionText = await questionDropdownLocator.evaluate(
      (selectElement: HTMLSelectElement) => {
        const selectedOption =
          selectElement.options[selectElement.selectedIndex]
        return selectedOption.textContent || selectedOption.innerText
      },
    )
    expect(selectionText).toEqual(questionText)
  }

  async expectSelectedScalar(
    conditionId: number,
    subconditionId: number,
    scalar: string,
  ) {
    await expect(
      this.page.locator(
        `#condition-${conditionId}-subcondition-${subconditionId}-scalar`,
      ),
    ).toHaveValue(scalar)
  }

  async expectSelectedOperator(
    conditionId: number,
    subconditionId: number,
    operator: string,
  ) {
    await expect(
      this.page.locator(
        `#condition-${conditionId}-subcondition-${subconditionId}-operator`,
      ),
    ).toHaveValue(operator)
  }

  async expectFilledValue(
    conditionId: number,
    subconditionId: number,
    value: SubconditionValue,
  ) {
    if (value.firstValue) {
      await expect(
        this.page.locator(
          `#condition-${conditionId}-subcondition-${subconditionId}-value:enabled`,
        ),
      ).toHaveValue(value.firstValue)
    }

    if (value.secondValue) {
      await expect(
        this.page.locator(
          `#condition-${conditionId}-subcondition-${subconditionId}-secondValue:enabled`,
        ),
      ).toHaveValue(value.secondValue)
    } else {
      await expect(
        this.page.locator(
          `#condition-${conditionId}-subcondition-${subconditionId}-secondValue:enabled`,
        ),
      ).toHaveCount(0)
    }

    if (value.multiValues) {
      for (let count = 1; count <= value.multiValues.length; count++) {
        const checkboxLabel = this.page.locator(
          `label[for="condition-${conditionId}-subcondition-${subconditionId}-values[${count}]"]`,
        )
        const multiValueSpec: MultiValueSpec = value.multiValues[count - 1]

        if (multiValueSpec.checked) {
          await expect(checkboxLabel).toBeChecked()
        }

        await expect(checkboxLabel).toHaveText(multiValueSpec.text)
      }
    }
  }
}
