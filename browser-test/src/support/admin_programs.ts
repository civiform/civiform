import { Page } from 'playwright'
import { readFileSync } from 'fs'
import { clickAndWaitForModal, waitForPageJsLoad } from './wait';

export class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminProgramsPage() {
    await this.page.click('nav :text("Programs")');
    await this.expectAdminProgramsPage();
    await waitForPageJsLoad(this.page);
  }

  async expectAdminProgramsPage() {
    expect(await this.page.innerText('h1')).toEqual('All programs');
  }

  async expectProgramExist(programName: string, description: string) {
    await this.gotoAdminProgramsPage();
    const tableInnerText = await this.page.innerText('main');

    expect(tableInnerText).toContain(programName);
    expect(tableInnerText).toContain(description);
  }

  async addProgram(programName: string, description = 'program description', externalLink = '', hidden = false) {
    await this.gotoAdminProgramsPage();
    await this.page.click('#new-program-button');
    await waitForPageJsLoad(this.page);

    await this.page.fill('#program-name-input', programName);
    await this.page.fill('#program-description-textarea', description);
    await this.page.fill('#program-display-name-input', programName);
    await this.page.fill('#program-display-description-textarea', description);
    await this.page.fill('#program-external-link-input', externalLink);

    if (hidden) {
      await this.page.check(`label:has-text("Hidden in Index")`);
    } else {
      await this.page.check(`label:has-text("Public")`);
    }

    await this.page.click('#program-update-button');
    await waitForPageJsLoad(this.page);

    await this.expectAdminProgramsPage();

    await this.expectProgramExist(programName, description);
  }

  selectProgramCard(programName: string, lifecycle: string) {
    return `.cf-admin-program-card:has(:text("${programName}")):has(:text("${lifecycle}"))`;
  }

  selectWithinProgramCard(programName: string, lifecycle: string, selector: string) {
    return this.selectProgramCard(programName, lifecycle) + ' ' + selector;
  }

  async gotoDraftProgramEditPage(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);
    await this.page.click(this.selectWithinProgramCard(programName, 'DRAFT', ':text("Edit")'));
    await waitForPageJsLoad(this.page);
    await this.expectProgramEditPage(programName);
  }

  async gotoDraftProgramManageTranslationsPage(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);
    await this.page.click(this.selectWithinProgramCard(programName, 'DRAFT', ':text("Manage Translations")'));
    await waitForPageJsLoad(this.page);
    await this.expectProgramManageTranslationsPage();
  }

  async gotoManageProgramAdminsPage(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);
    await this.page.click(this.selectWithinProgramCard(programName, 'DRAFT', ':text("Manage Admins")'));
    await waitForPageJsLoad(this.page);
    await this.expectManageProgramAdminsPage();
  }

  async goToEditBlockPredicatePage(programName: string, blockName: string) {
    await this.gotoDraftProgramEditPage(programName);
    await this.page.click('text=Manage Questions');
    await waitForPageJsLoad(this.page);
    await this.expectProgramBlockEditPage(programName);

    // Click on the block to edit
    await this.page.click(`a:has-text("${blockName}")`);
    await waitForPageJsLoad(this.page);

    // Click on the edit predicate button
    await this.page.click('#cf-edit-predicate');
    await waitForPageJsLoad(this.page);
    await this.expectEditPredicatePage(blockName);
  }

  async expectDraftProgram(programName: string) {
    expect(await this.page.innerText(this.selectProgramCard(programName, 'DRAFT'))).not.toContain('New Version');
  }

  async expectActiveProgram(programName: string) {
    expect(await this.page.innerText(this.selectProgramCard(programName, 'ACTIVE'))).toContain('New Version');
  }

  async expectObsoleteProgram(programName: string) {
    expect(await this.page.innerText(this.selectProgramCard(programName, 'OBSOLETE'))).toContain('Applications');
  }

  async expectProgramEditPage(programName: string = '') {
    expect(await this.page.innerText('h1')).toContain(`Edit program: ${programName}`);
  }

  async expectProgramManageTranslationsPage() {
    expect(await this.page.innerText('h1')).toContain('Manage program translations');
  }

  async expectManageProgramAdminsPage() {
    expect(await this.page.innerText('h1')).toContain('Manage Admins for Program');
  }

  async expectEditPredicatePage(blockName: string) {
    expect(await this.page.innerText('h1')).toContain('Visibility condition for ' + blockName);
  }

  async expectProgramBlockEditPage(programName: string = '') {
    expect(await this.page.innerText('id=program-title')).toContain(programName);
    expect(await this.page.innerText('id=block-edit-form')).not.toBeNull();
    // Compare string case insensitively because style may not have been computed.
    expect((await this.page.innerText('[for=block-name-input]')).toUpperCase()).toEqual('SCREEN NAME');
    expect((await this.page.innerText('[for=block-description-textarea]')).toUpperCase()).toEqual('SCREEN DESCRIPTION');
    expect(await this.page.innerText('h1')).toContain('Question bank');
  }

  async editProgramBlock(programName: string, blockDescription = 'screen description', questionNames: string[] = []) {
    await this.gotoDraftProgramEditPage(programName);

    await this.page.click('text=Manage Questions');
    await waitForPageJsLoad(this.page);
    await this.expectProgramBlockEditPage(programName);

    await clickAndWaitForModal(this.page, 'block-description-modal');
    await this.page.fill('textarea', blockDescription);
    // Make sure input validation enables the button before clicking.
    await this.page.click('#update-block-button:not([disabled])');

    for (const questionName of questionNames) {
      await this.page.click(`button:text("${questionName}")`);
    }
  }

  async editProgramBlockWithOptional(programName: string, blockDescription = 'screen description', questionNames:
    string[], optionalQuestionName: string) {
    await this.gotoDraftProgramEditPage(programName);

    await this.page.click('text=Manage Questions');
    await waitForPageJsLoad(this.page);
    await this.expectProgramBlockEditPage(programName);

    await clickAndWaitForModal(this.page, 'block-description-modal');
    await this.page.fill('textarea', blockDescription);
    await this.page.click('#update-block-button:not([disabled])');

    // Add the optional question
    await this.page.click(`button:text("${optionalQuestionName}")`);
    await waitForPageJsLoad(this.page);
    // Only allow one optional question per block; this selector will always toggle the first optional button.  It
    // cannot tell the difference between multiple option buttons
    await this.page.click(`:is(button:has-text("optional"))`);

    for (const questionName of questionNames) {
      await this.page.click(`button:text("${questionName}")`);
      await waitForPageJsLoad(this.page);
    }
  }

  async addProgramBlock(programName: string, blockDescription = 'screen description', questionNames: string[] = []) {
    await this.gotoDraftProgramEditPage(programName);

    await this.page.click('text=Manage Questions');
    await waitForPageJsLoad(this.page);
    await this.expectProgramBlockEditPage(programName);

    await this.page.click('#add-block-button');
    await waitForPageJsLoad(this.page);

    await clickAndWaitForModal(this.page, 'block-description-modal');
    await this.page.type('textarea', blockDescription);
    await this.page.click('#update-block-button:not([disabled])');
    // Wait for submit and redirect back to this page.
    await this.page.waitForURL(this.page.url());
    await waitForPageJsLoad(this.page);

    for (const questionName of questionNames) {
      await this.page.click(`button:text("${questionName}")`);
      await waitForPageJsLoad(this.page);
      // Make sure the question is successfully added to the screen.
      await this.page.waitForSelector(`div.cf-program-question p:text("${questionName}")`);
    }
    return await this.page.$eval('#block-name-input', el => (el as HTMLInputElement).value);
  }

  /** Adds a block with a single optional question followed by one or more required ones. */
  async addProgramBlockWithOptional(programName: string, blockDescription = 'screen description', questionNames:
    string[], optionalQuestionName: string) {
    await this.page.click('#add-block-button');
    await waitForPageJsLoad(this.page);

    await clickAndWaitForModal(this.page, 'block-description-modal');

    await this.page.type('textarea', blockDescription);
    await this.page.click('#update-block-button:not([disabled])');

    // Add the optional question
    await this.page.click(`button:text("${optionalQuestionName}")`);
    await waitForPageJsLoad(this.page);
    // Only allow one optional question per block; this selector will always toggle the first optional button.  It
    // cannot tell the difference between multiple optional buttons
    await this.page.click(`:is(button:has-text("optional"))`);

    for (const questionName of questionNames) {
      await this.page.click(`button:text("${questionName}")`);
      await waitForPageJsLoad(this.page);
    }
  }

  async addProgramRepeatedBlock(programName: string,
    enumeratorBlockName: string,
    blockDescription = 'screen description',
    questionNames: string[] = []) {
    await this.gotoDraftProgramEditPage(programName);

    await this.page.click('text=Manage Questions');
    await waitForPageJsLoad(this.page);
    await this.expectProgramBlockEditPage(programName);

    await this.page.click(`text=${enumeratorBlockName}`);
    await waitForPageJsLoad(this.page);
    await this.page.click('#create-repeated-block-button');
    await waitForPageJsLoad(this.page);

    await clickAndWaitForModal(this.page, 'block-description-modal');
    await this.page.fill('#block-description-textarea', blockDescription);
    await this.page.click('#update-block-button:not([disabled])');

    for (const questionName of questionNames) {
      await this.page.click(`button:text("${questionName}")`);
    }
  }

  async publishProgram(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectDraftProgram(programName);
    await this.publishAllPrograms();
    await this.expectActiveProgram(programName);
  }

  async publishAllPrograms() {
    await clickAndWaitForModal(this.page, 'publish-all-programs-modal');
    await this.page.click(`#publish-programs-button > button`);
    await waitForPageJsLoad(this.page);
  }

  async createNewVersion(programName: string) {
    await this.gotoAdminProgramsPage();
    await this.expectActiveProgram(programName);

    await this.page.click(this.selectWithinProgramCard(programName, 'ACTIVE', ':text("New Version")'));
    await waitForPageJsLoad(this.page);
    await this.page.click('#program-update-button');
    await waitForPageJsLoad(this.page);
    await this.expectDraftProgram(programName);
  }

  async createPublicVersion(programName: string) {
    await this.gotoAdminProgramsPage();

    await this.expectActiveProgram(programName);

    await this.page.click(this.selectWithinProgramCard(programName, 'ACTIVE', ':text("New Version")'));
    await waitForPageJsLoad(this.page);
    await this.page.check(`label:has-text("Public")`);
    await this.page.click('#program-update-button');
    await waitForPageJsLoad(this.page);

    await this.expectDraftProgram(programName);
  }

  async viewApplications(programName: string) {
    await this.page.click(this.selectWithinProgramCard(programName, 'ACTIVE', 'a:text("Applications")'));
    await waitForPageJsLoad(this.page);
  }

  async viewApplicationsInOldVersion() {
    await this.page.click('a:text("Applications")');
    await waitForPageJsLoad(this.page);
  }

  async viewApplicationsForOldVersion(programName: string) {
    await this.page.click(this.selectWithinProgramCard(programName, 'ACTIVE', ':text("Applications")'));
    await waitForPageJsLoad(this.page);
    await this.page.click("a:has-text(\"Applications\")");
    await waitForPageJsLoad(this.page);
  }

  selectApplicationCardForApplicant(applicantName: string) {
    return `.cf-admin-application-card:has-text("${applicantName}")`;
  }

  selectWithinApplicationForApplicant(applicantName: string, selector: string) {
    return this.selectApplicationCardForApplicant(applicantName) + ' ' + selector;
  }

  selectApplicationBlock(blockName: string) {
    return `.cf-admin-application-block-card:has-text("${blockName}")`;
  }

  selectWithinApplicationBlock(blockName: string, selector: string) {
    return this.selectApplicationBlock(blockName) + ' ' + selector;
  }

  async viewApplicationForApplicant(applicantName: string) {
    await this.page.click(this.selectWithinApplicationForApplicant(applicantName, 'a:text("View")'));
    await waitForPageJsLoad(this.page);
  }

  async expectApplicationAnswers(blockName: string, questionName: string, answer: string) {
    expect(await this.page.innerText(this.selectApplicationBlock(blockName))).toContain(questionName);
    expect(await this.page.innerText(this.selectApplicationBlock(blockName))).toContain(answer);
  }

  async expectApplicationAnswerLinks(blockName: string, questionName: string) {
    expect(await this.page.innerText(this.selectApplicationBlock(blockName))).toContain(questionName);
    expect(await this.page.getAttribute(this.selectWithinApplicationBlock(blockName, 'a'), 'href')).not.toBeNull();
  }

  async getCsv() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download all versions (CSV)"')
    ]);
    const path = await downloadEvent.path();
    if (path === null) {
      throw new Error('download failed');
    }
    return readFileSync(path, 'utf8');
  }

  async getDemographicsCsv() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click('text="Download Exported Data (CSV)"')
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

    await this.publishProgram(programName);
  }
}
