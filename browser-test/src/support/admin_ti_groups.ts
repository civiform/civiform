import { Page } from 'playwright'

export class AdminTIGroups {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminTIPage() {
    // The warning message may be in the way of this link
    await this.page.click('#warning-message-dismiss').catch(error => console.log("didn't find a warning, which is fine."));
    await this.page.click('nav :text("Intermediaries")');
    await this.expectAdminTIPage();
  }

  async expectAdminTIPage() {
    expect(await this.page.innerText('h1')).toEqual('Trusted Intermediary Groups');
  }

  async fillInGroupBasics(groupName: string, description: string) {
    // This function should only be called on group page.
    await this.page.fill('text="Name"', groupName);
    await this.page.fill('text=Description', description);
    await this.page.click('text="Create"');
  }

  async expectGroupExist(groupName: string, description = '') {
    await this.gotoAdminTIPage();
    const tableInnerText = await this.page.innerText('table');

    expect(tableInnerText).toContain(groupName);
    expect(tableInnerText).toContain(description);
  }


}
