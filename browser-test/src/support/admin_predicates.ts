import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'

type PredicateSpec = {
  questionName: string
  action?: string | null
  scalar: string
  operator: string
  value?: string
  values?: string[]
}

export class AdminPredicates {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async addValueRows(predicateSpec: PredicateSpec) {
    const values = predicateSpec.values

    if (values && values.length > 1) {
      for (let i = 1; i < values.length; i++) {
        await this.page.click('#predicate-add-value-row')
      }
    }
  }

  async clickEditPredicateButton(predicateType: 'visibility' | 'eligibility') {
    await this.page.click(
      `button:has-text("Edit existing ${predicateType} condition")`,
    )
  }

  async addPredicates(predicateSpecs: PredicateSpec[]) {
    for (const predicateSpec of predicateSpecs) {
      await this.selectQuestionForPredicate(predicateSpec.questionName)
    }

    await this.clickAddConditionButton()
    await this.addValueRows(predicateSpecs[0])

    for (const predicateSpec of predicateSpecs) {
      await this.configurePredicate(predicateSpec)
    }

    await this.clickSaveConditionButton()
  }

  async selectQuestionForPredicate(questionName: string) {
    await this.page.click(`label:has-text("Admin ID: ${questionName}")`)
  }

  async clickAddConditionButton() {
    await this.page.click('button:has-text("Add condition")')
  }

  async clickSaveConditionButton() {
    await this.page.click('button:visible:has-text("Save condition")')
  }

  async getQuestionId(questionName: string): Promise<string> {
    const questionNameField = this.page.getByTestId(questionName)
    expect((await questionNameField.all()).length).toEqual(1)

    const questionId = await questionNameField.getAttribute('data-question-id')
    expect(questionId).not.toBeNull()

    return questionId as string
  }

  async configurePredicate({
    questionName,
    action,
    scalar,
    operator,
    value,
    values,
  }: PredicateSpec) {
    values = values ? values : value ? [value] : []

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

    let groupNum = 1
    for (const valueToSet of values) {
      // Service areas are the only value input that use a select
      if (scalar === 'service_area') {
        const valueSelect = await this.page.$(
          `select[name="group-${groupNum++}-question-${questionId}-predicateValue"]`,
        )

        if (valueSelect == null) {
          throw new Error(
            `Unable to find select for service area: select[name="group-${groupNum++}-question-${questionId}-predicateValue"]`,
          )
        }

        await valueSelect.selectOption({label: valueToSet})
        continue
      }

      const valueInput = await this.page.$(
        `input[name="group-${groupNum++}-question-${questionId}-predicateValue"]`,
      )

      if (valueInput) {
        await valueInput.fill(valueToSet || '')
      } else {
        // We have a checkbox for the value.
        const valueArray = valueToSet.split(',')
        for (const value of valueArray) {
          await this.page.check(`label:has-text("${value}")`)
        }
      }
    }
  }

  // For multi-option questions where the value is a checkbox of options, provide a comma-separated
  // list of the options you would like to check as the value. Ex: blue,red,green
  //
  // If action is null the action selector will not be set.
  async addPredicate(
    questionName: string,
    action: string | null,
    scalar: string,
    operator: string,
    value: string,
  ) {
    await this.selectQuestionForPredicate(questionName)
    await this.clickAddConditionButton()

    await this.configurePredicate({
      questionName,
      action,
      scalar,
      operator,
      value,
    })

    await this.clickSaveConditionButton()
    await waitForPageJsLoad(this.page)
  }

  // For the old admin predicates UI
  // // TODO(#4004): remove this function
  async addLegacyPredicate(
    questionName: string,
    action: string | null,
    scalar: string,
    operator: string,
    value: string,
  ) {
    await this.page.click(`button:has-text("Admin ID: ${questionName}")`)

    if (action != null) {
      await this.page.selectOption('.cf-predicate-action:visible select', {
        label: action,
      })
    }
    await this.page.selectOption('.cf-scalar-select:visible select', {
      label: scalar,
    })
    await this.page.selectOption('.cf-operator-select:visible select', {
      label: operator,
    })

    const valueInput = await this.page.$(
      '.cf-predicate-value-input:visible input',
    )
    if (valueInput) {
      await valueInput.fill(value)
    } else {
      // We have a checkbox for the value.
      const valueArray = value.split(',')
      for (const value of valueArray) {
        await this.page.check(`label:has-text("${value}")`)
      }
    }

    await this.page.click('button:visible:has-text("Submit")')
    await waitForPageJsLoad(this.page)
  }

  async expectPredicateDisplayTextContains(condition: string) {
    expect(await this.page.innerText('.cf-display-predicate')).toContain(
      condition,
    )
  }
}
