import { Page } from 'playwright'

export class AdminPredicates {
  public page!: Page

  constructor(page: Page) {
    this.page = page;
  }

  // For multi-option questions where the value is a checkbox of options, provide a comma-separated
  // list of the options you would like to check as the value. Ex: blue,red,green
  async addPredicate(questionName: string, action: string, scalar: string, operator: string, value: string) {
    await this.page.waitForLoadState('load');
    await this.page.click(`text="${questionName}"`);
    await this.page.selectOption('.cf-predicate-action:visible select', { label: action });
    await this.page.selectOption('.cf-scalar-select:visible select', { label: scalar });
    await this.page.selectOption('.cf-operator-select:visible select', { label: operator });

    const valueInput = await this.page.$('.cf-predicate-value-input:visible input');
    if (valueInput) {
      await valueInput.fill(value);
    } else {
      // We have a checkbox select.
      const valueArray = value.split(',');
      for (var index in valueArray) {
        await this.page.check(`label:has-text("${valueArray[index]}")`);
      }
    }

    await this.page.click('button:visible:has-text("Submit")');
  }

  async expectVisibilityConditionEquals(condition: string) {
    expect(await this.page.innerText('.cf-display-predicate')).toContain(condition);
  }
}
