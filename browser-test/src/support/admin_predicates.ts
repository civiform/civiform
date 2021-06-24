import { Page } from 'playwright'

export class AdminPredicates {
  public page!: Page

  constructor(page: Page) {
    this.page = page;
  }

  async addPredicate(questionName: string, action: string, scalar: string, operator: string, value: string) {
    await this.page.waitForLoadState('load');
    await this.page.click(`text="${questionName}"`);
    await this.page.selectOption('.cf-predicate-action:visible select', { label: action });
    await this.page.selectOption('.cf-scalar-select:visible select', { label: scalar });
    await this.page.selectOption('.cf-operator-select:visible select', { label: operator });

    // TODO: determine whether the value is an input or select
    await this.page.fill('.cf-predicate-value-input:visible input', value);
    await this.page.click('button:visible:has-text("Submit")');
  }

  async expectVisibilityConditionEquals(condition: string) {
    expect(await this.page.innerText('.cf-display-predicate')).toContain(condition);
  }
}
