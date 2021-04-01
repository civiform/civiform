import { Page } from 'playwright'

export class AdminQuestions {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async addAddressQuestion(questionName: string,
    description = 'address description',
    questionText = 'address question text',
    helpText = 'address question help text') {
    await this.page.click('text=Questions')
    await this.page.click('#create-question-button')

    await this.page.click('#create-address-question')

    await this.page.fill('text="Name"', questionName)
    await this.page.fill('text=Description', description)
    await this.page.fill('text=Question Text', questionText)
    await this.page.fill('text=Question help text', helpText)

    await this.page.click('text=Create')

    expect(await this.page.innerText('h1')).toEqual('All Questions')

    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
  }

  async addDropdownQuestion(questionName: string,
    options: Array<string>,
    description = 'dropdown description',
    questionText = 'dropdown question text',
    helpText = 'dropdown question help text') {
    await this.page.click('text=Questions')
    await this.page.click('#create-question-button')
    await this.page.click('#create-dropdown-question')


    await this.page.fill('text="Name"', questionName)
    await this.page.fill('text=Description', description)
    await this.page.fill('text=Question Text', questionText)
    await this.page.fill('text=Question help text', helpText)

    for (var index in options) {
      await this.page.click('#add-new-option')
      await this.page.fill('input:above(#add-new-option)', options[index])
    }

    await this.page.click('text=Create')

    expect(await this.page.innerText('h1')).toEqual('All Questions')

    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
  }

  async addNameQuestion(questionName: string,
    description = 'name description',
    questionText = 'name question text',
    helpText = 'name question help text') {
    await this.page.click('text=Questions')
    await this.page.click('#create-question-button')

    await this.page.click('#create-name-question')

    await this.page.fill('text="Name"', questionName)
    await this.page.fill('text=Description', description)
    await this.page.fill('text=Question Text', questionText)
    await this.page.fill('text=Question help text', helpText)

    await this.page.click('text=Create')

    expect(await this.page.innerText('h1')).toEqual('All Questions')

    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
  }

  async addNumberQuestion(questionName: string,
    description = 'number description',
    questionText = 'number question text',
    helpText = 'number question help text') {
    await this.page.click('text=Questions')
    await this.page.click('#create-question-button')

    await this.page.click('#create-number-question')

    await this.page.fill('text="Name"', questionName)
    await this.page.fill('text=Description', description)
    await this.page.fill('text=Question Text', questionText)
    await this.page.fill('text=Question help text', helpText)

    await this.page.click('text=Create')

    expect(await this.page.innerText('h1')).toEqual('All Questions')

    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
  }

  async addRadioButtonQuestion(questionName: string,
    options: Array<string>,
    description = 'radio button description',
    questionText = 'radio button question text',
    helpText = 'radio button question help text') {
    await this.page.click('text=Questions')
    await this.page.click('#create-question-button')
    await this.page.click('#create-radio_button-question')


    await this.page.fill('text="Name"', questionName)
    await this.page.fill('text=Description', description)
    await this.page.fill('text=Question Text', questionText)
    await this.page.fill('text=Question help text', helpText)

    for (var index in options) {
      await this.page.click('#add-new-option')
      await this.page.fill('input:above(#add-new-option)', options[index])
    }

    await this.page.click('text=Create')

    expect(await this.page.innerText('h1')).toEqual('All Questions')

    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
  }

  async addTextQuestion(questionName: string,
    description = 'text description',
    questionText = 'text question text',
    helpText = 'text question help text') {
    await this.page.click('text=Questions')
    await this.page.click('#create-question-button')

    await this.page.click('#create-text-question')

    await this.page.fill('text="Name"', questionName)
    await this.page.fill('text=Description', description)
    await this.page.fill('text=Question Text', questionText)
    await this.page.fill('text=Question help text', helpText)

    await this.page.click('text=Create')

    expect(await this.page.innerText('h1')).toEqual('All Questions')

    const tableInnerText = await this.page.innerText('table')

    expect(tableInnerText).toContain(questionName)
    expect(tableInnerText).toContain(questionText)
  }
}
