import { Page } from 'playwright'

export class AdminPredicates {
  public page!: Page

  constructor(page: Page) {
    this.page = page;
  }

  async addPredicate(questionName: string, action: string, scalar: string, operator: string, value: string) {
    await this.page.click(`text="${questionName}"`);
    await this.page.selectOption('.cf-predicate-action select', { label: action });
    await this.page.selectOption('.cf-scalar-select select', { label: scalar });
    await this.page.selectOption('.cf-operator-select select', { label: operator });
    await this.page.fill('.cf-predicate-value-input input', value);
    await this.page.click('text=Submit');
  }

  async expectVisibilityConditionEquals(condition: string) {
    expect(await this.page.innerText('.cf-display-predicate')).toContain(condition);
  }
}
