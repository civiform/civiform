import { Page } from 'playwright'
import { readFileSync } from  'fs'


export class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async viewApplications() {
    await this.page.click('text="Applications →"');
  }

  async getCsv() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download all (CSV)"')
    ]);
    const path = await downloadEvent.path();
    return readFileSync(path, 'utf8');
  }

  async addProgram(questionNames: string[], programName: string) {
    await this.page.click('text=Programs');
    await this.page.click('#new-program-button');
    await this.page.fill('text=Program Name', programName);
    await this.page.fill('text=Program Description', "dummy description");
    await this.page.click('#program-create-button');
    await this.page.click('text=Edit');
    await this.page.click('text=Manage Questions');
    await this.page.fill('text=Block Description', "dummy description");
    for (const questionName of questionNames) {
      await this.page.click(`text="${questionName}"`, {force: true});
    }
    await this.page.click('#update-block-button');
    await this.page.click('text=Programs');

    // This is an assert, actually - the click selectors allow us to more clearly express what we're looking for than
    // the other selectors (like $).  This isn't documented but appears to be true.
    await this.page.click('text=DRAFT');

    await this.page.click('text=Programs');
    await this.page.click('text=Publish');

    // Also an assert.
    await this.page.click('text=ACTIVE');
  }
}
