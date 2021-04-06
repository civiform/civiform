import { Page } from 'playwright'
import { readFileSync } from 'fs'

export class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminProgramsPage() {
    await this.page.click('nav :text("Programs")');
    await this.expectAdminProgramsPage();
  }

  async expectAdminProgramsPage() {
    expect(await this.page.innerText('h1')).toEqual('All Programs');
  }

  async expectProgramExist(programName: string, description: string) {
    await this.gotoAdminProgramsPage();
    const tableInnerText = await this.page.innerText('main');

    expect(tableInnerText).toContain(programName);
    expect(tableInnerText).toContain(description);
  }

  async addProgram(programName: string, description = 'program description') {
    await this.gotoAdminProgramsPage();
    await this.page.click('#new-program-button');

    await this.page.fill('text=Program name', programName);
    await this.page.fill('text=Program description', description);

    await this.page.click('text=Create');

    await this.expectAdminProgramsPage();

    await this.expectProgramExist(programName, description);
  }

  async gotoDraftProgramEditPage(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);
    // Get the admin program card enclosing the specified program. Once we settle on
    // admin style, we should make a more identifiable class, e.g. ".cf-admin-program-card".
    await this.page.click(`div.border:has-text("${programName}") :text("Edit")`);
    await this.expectProgramEditPage(programName);
  }

  async expectDraftProgram(programName: string) {
    expect(await this.page.innerText(`div.border:has-text("${programName}") p`)).toContain('DRAFT');
  }

  async expectActiveProgram(programName: string) {
    expect(await this.page.innerText(`div.border:has-text("${programName}") p`)).toContain('ACTIVE');
  }

  async expectProgramEditPage(programName: string = '') {
    expect(await this.page.innerText('h1')).toContain(`Edit program: ${programName}`);
  }

  async expectProgramBlockEditPage(programName: string = '') {
    expect(await this.page.innerText('html')).toContain(programName);
    expect(await this.page.innerText('label')).toEqual('BLOCK NAME');
    expect(await this.page.innerText('h1')).toContain('Question bank');
  }

  async editProgramBlock(programName: string, blockDescription = 'block description', questionNames: string[] = []) {
    await this.gotoDraftProgramEditPage(programName);

    await this.page.click('text=Manage Questions');
    await this.expectProgramBlockEditPage(programName);

    await this.page.fill('text=Block Description', blockDescription);

    for (const questionName of questionNames) {
      await this.page.click(`text="${questionName}"`, { force: true });
    }
    await this.page.click('#update-block-button');
  }

  async publishProgram(programName: string) {
    await this.page.click(`div.border:has-text("${programName}") :text("Publish")`);
  }

  async viewApplications(programName: string) {
    await this.page.click(`div.border:has-text("${programName}") :text("Applications")`);
  }

  async getCsv() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download all (CSV)"')
    ]);
    const path = await downloadEvent.path();
    if (path === null) {
      throw new Error('download failed');
    }
    return readFileSync(path, 'utf8');
  }

  async addAndPublishProgramWithQuestions(questionNames: string[], programName: string) {
    await this.addProgram(programName);
    await this.editProgramBlock(programName, 'dummy description', questionNames);

    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);

    await this.publishProgram(programName);
    await this.expectActiveProgram(programName);
  }
}
