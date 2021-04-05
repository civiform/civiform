import { Page } from 'playwright'
import { readFileSync } from  'fs'

export class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminProgramsPage() {
    await this.page.click('nav :text("Programs")')
    await this.expectAdminProgramsPage()
  }

  async expectAdminProgramsPage() {
    expect(await this.page.innerText('h1')).toEqual('All Programs')
  }

  async expectProgramExist(programName: string, description: string) {
    await this.gotoAdminProgramsPage()
    const tableInnerText = await this.page.innerText('main')

    expect(tableInnerText).toContain(programName)
    expect(tableInnerText).toContain(description)
  }

  async addProgram(programName: string,
    description = 'program description') {
    await this.gotoAdminProgramsPage()
    await this.page.click('#new-program-button')

    await this.page.fill('text=Program name', programName)
    await this.page.fill('text=Program description', description)

    await this.page.click('text=Create')

    await this.expectAdminProgramsPage()

    await this.expectProgramExist(programName, description)
  }

  async gotoEditProgramPage(programName: string) {
    await this.gotoAdminProgramsPage()
    await this.page.click('div:has-text("' + programName + '") :text("Edit")')
    await this.expectEditProgramPage(programName)
  }

  async expectEditProgramPage(programName: string = '') {
    expect(await this.page.innerText('h1')).toContain('Edit program: ' + programName)
  }

  async expectEditProgramBlockPage(programName: string = '') {
    expect(await this.page.innerText('html')).toContain(programName)
    expect(await this.page.innerText('label')).toEqual('BLOCK NAME')
    expect(await this.page.innerText('h1')).toContain('Question bank')
  }

  async addProgramBlock(programName: string) {
    await this.gotoEditProgramPage(programName)

    await this.page.click('text=Manage Questions')
    await this.expectEditProgramBlockPage(programName)

    await this.page.click('text=Add Block')
    expect(await this.page.getAttribute('input#block-name-input', 'value')).toEqual('Block 2')
  }

  async addQuestion(programName: string, questionName: string) {
    await this.gotoEditProgramPage(programName)

    await this.page.click('text=Manage Questions')
    await this.expectEditProgramBlockPage(programName)

    await this.page.click('text=Add Block')
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
