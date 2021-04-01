import { Page } from 'playwright'

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
}
