import { Page } from 'playwright'
import { readFileSync } from 'fs'

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

  async addProgram(programName: string, description = 'program description') {
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

  async editProgramBlock(programName: string, blockDescription: string) {
    await this.gotoEditProgramPage(programName)

    await this.page.click('text=Manage Questions')
    await this.expectEditProgramBlockPage(programName)

    await this.page.fill('text=Block Description', blockDescription);
    await this.page.click('#update-block-button');
  }

  async addProgramBlock(programName: string) {
    await this.gotoEditProgramPage(programName)

    await this.page.click('text=Manage Questions')
    await this.expectEditProgramBlockPage(programName)

    await this.page.click('text=Add Block')
    expect(await this.page.getAttribute('input#block-name-input', 'value')).toEqual('Block 2')
  }

  async addQuestion(programName: string, questionNames: string[]) {
    await this.gotoEditProgramPage(programName)

    await this.page.click('text=Manage Questions')
    await this.expectEditProgramBlockPage(programName)

    for (const questionName of questionNames) {
      await this.page.click(`text="${questionName}"`, { force: true });
    }
    await this.page.click('#update-block-button');
  }

  async publishProgram(programName: string) {
    await this.page.click('div.border:has-text("' + programName + '") :text("Publish")');
  }

  async viewApplications(programName: string) {
    await this.page.click('div.border:has-text("' + programName + '") :text("Applications")');
  }

  async getCsv() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download all (CSV)"')
    ]);
    const path = await downloadEvent.path();
    return readFileSync(path, 'utf8');
  }

  async addProgramWithQuestions(questionNames: string[], programName: string) {
    await this.addProgram(programName, 'dummy description');
    await this.editProgramBlock(programName, 'dummy description')
    await this.addQuestion(programName, questionNames);
    await this.gotoAdminProgramsPage();

    // This is an assert, actually - the click selectors allow us to more clearly express what we're looking for than
    // the other selectors (like $).  This isn't documented but appears to be true.
    await this.page.click('text=DRAFT');

    await this.page.click('text=Programs');
    await this.publishProgram(programName)

    // Also an assert.
    await this.page.click('text=ACTIVE');
  }
}
