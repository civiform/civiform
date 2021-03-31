import { Page } from 'playwright'

export class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminProgramsPage() {
    await this.page.click('nav :has-text("Programs")')
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

  async addProgramBlock(programName: string) {
    await this.gotoAdminProgramsPage()
    // This only works when we have one program
    await this.page.click('text=Edit')

    await this.page.click('text="Manage Questions"')

    await this.page.click('text=Add Block')
  }

  async addQuestion(questionName: string) {
    await this.gotoAdminProgramsPage()
    // This only works when we have one program
    await this.page.click('text=Edit')

    await this.page.click('text=Manage Questions')

    await this.page.click('text=Add Block')
  }
}
