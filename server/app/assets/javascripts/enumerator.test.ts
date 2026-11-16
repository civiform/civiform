import {describe, it, expect, beforeEach, vi} from 'vitest'
import {init} from '@/enumerator'
import {assertNotNull} from '@/util'

// jsdom doesn't implement innerText, which enumerator.ts uses to relabel rows.
Object.defineProperty(HTMLElement.prototype, 'innerText', {
  get(this: HTMLElement) {
    return this.textContent
  },
  set(this: HTMLElement, value: string) {
    this.textContent = value
  },
  configurable: true,
})

// Builds an initial-question list page. Each row is a saved row whose input name bakes in its
// position, like the server renders it.
function renderList(answers: string[]) {
  const rows = answers
    .map(
      (answer, index) => `
        <div class="cf-enumerator-field" data-entity-saved="true">
          <label class="cf-entity-index-label">Season #${index + 1}</label>
          <input type="text" name="applicant.seasons[${index}].season.text" value="${answer}">
          <button type="button" id="${index}" class="cf-enumerator-delete-button">Remove #${index + 1}</button>
        </div>`,
    )
    .join('')
  document.body.innerHTML = `
    <div class="cf-question-enumerator" id="enumerator-question">
      <div id="enumerator-fields">${rows}</div>
      <input type="hidden" id="enumerator-delete-template" name="delete_entity[]" disabled>
      <button type="button" id="enumerator-field-add-button" hx-post="/add" data-initial-question="true">Add</button>
    </div>`
}

function submittedAnswers(): Record<string, string> {
  const answers: Record<string, string> = {}
  document
    .querySelectorAll<HTMLInputElement>('input[name^="applicant."]')
    .forEach((input) => {
      answers[input.name] = input.value
    })
  return answers
}

function removeRow(index: number) {
  assertNotNull(document.getElementById(String(index))).click()
}

describe('enumerator removal of saved rows', () => {
  beforeEach(() => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderList(['Spring', 'Summer', 'Fall'])
    init()
  })

  it('removeSavedRow_rowsAfterIt_keepTheirOriginalPositions', () => {
    removeRow(0)

    expect(submittedAnswers()).toEqual({
      'applicant.seasons[0].season.text': 'Spring',
      'applicant.seasons[1].season.text': 'Summer',
      'applicant.seasons[2].season.text': 'Fall',
    })
    const deleteMarker = assertNotNull(
      document.querySelector<HTMLInputElement>(
        'input[name="delete_entity[]"]:not([disabled])',
      ),
    )
    expect(deleteMarker.value).toBe('0')
  })

  it('removeSavedRow_editToRemainingRow_isSubmittedAtItsOwnPosition', () => {
    const summer = assertNotNull(
      document.querySelector<HTMLInputElement>('input[value="Summer"]'),
    )
    removeRow(0)
    summer.value = 'Winter'

    expect(summer.name).toBe('applicant.seasons[1].season.text')
  })

  it('removeSavedRow_visibleLabelsAreRenumbered', () => {
    removeRow(0)

    const labels = Array.from(
      document.querySelectorAll(
        '.cf-enumerator-field:not(.display-none) .cf-entity-index-label',
      ),
    ).map((label) => (label as HTMLElement).innerText)
    expect(labels).toEqual(['Season #1', 'Season #2'])
  })

  it('removeSavedRow_addButtonCountIncludesHiddenRows', () => {
    removeRow(0)

    const addButton = assertNotNull(
      document.getElementById('enumerator-field-add-button'),
    )
    const vals = JSON.parse(
      assertNotNull(addButton.getAttribute('hx-vals')),
    ) as {
      entityCount: number
    }
    expect(vals.entityCount).toBe(3)
  })

  it('removeUnsavedRow_laterRowsAreRenumbered', () => {
    renderList(['Spring'])
    const unsavedRow = (index: number, answer: string) => `
      <div class="cf-enumerator-field">
        <label class="cf-entity-index-label">Season #${index + 1}</label>
        <input type="text" name="applicant.seasons[${index}].season.text" value="${answer}">
        <button type="button" id="unsaved-${index}" class="cf-enumerator-delete-button">Remove #${index + 1}</button>
      </div>`
    assertNotNull(
      document.getElementById('enumerator-fields'),
    ).insertAdjacentHTML(
      'beforeend',
      unsavedRow(1, 'Summer') + unsavedRow(2, 'Fall'),
    )
    init()

    assertNotNull(document.getElementById('unsaved-1')).click()

    expect(submittedAnswers()).toEqual({
      'applicant.seasons[0].season.text': 'Spring',
      'applicant.seasons[1].season.text': 'Fall',
    })
  })
})
