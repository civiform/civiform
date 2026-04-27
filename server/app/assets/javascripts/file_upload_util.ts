import {assertNotNull} from '@/util'

/** @fileoverview Collection of utility functions for working with file upload.
 * Can be used for both the admin UI and applicant UI.
 */

// This should be kept in sync with views/fileupload/FileUploadViewStrategy.FILE_LIMIT_ATTR.
const MAX_FILE_SIZE_MB_ATTR = 'data-file-limit-mb'

// Matches a file name with a number "-<number>" at the end. For example "file-2.png"
// Groups are: [1] The file name [2] The "-<number>" [3] - The file type, if it exists (e.g. .png), null otherwise.
const FILE_NAME_DIGIT_SUFFIX_REGEX = /(.*)(-\d*)(\..*)?$/
// Matches a file name with a file type at the end.
// Groups are [1] The file name [2] The file type.
const FILE_NAME_REGEX = /(.*)(\..*)$/

/**
 * Returns true if the file currently uploaded to {@code inputElement} is too large
 * and false otherwise. This will also return false if there is no file uploaded to
 * the {@code inputElement}.
 *
 * @param inputElement the HTMLInputElement that contains the file. Requirements:
 *   - Must be `type="file"`.
 *   - Must have an attribute with key {@code MAX_FILE_SIZE_MB_ATTR}. The attribute's
 *     value must be a number representing the max size in megabytes allowed by this
 *     input element.
 *
 * Note that this will only check the first file uploaded to the input element
 * because CiviForm currently only supports uploading a single file at a time.
 */
export function isFileTooLarge(inputElement: HTMLInputElement): boolean {
  if (inputElement.value == '') {
    return false
  }
  const files = inputElement.files
  if (files == null) {
    return false
  }

  const file = assertNotNull(files)[0]
  const maxFileSizeMb = parseInt(
    assertNotNull(inputElement.getAttribute(MAX_FILE_SIZE_MB_ATTR)),
  )
  // file.size is in bytes, so we need to convert the megabytes to bytes to have
  // an accurate comparison.
  const maxFileSizeBytes = maxFileSizeMb * 1024 * 1024

  return file.size > maxFileSizeBytes
}

/**
 * Returns a unique name.
 *
 * Note: This is only exported so we can test it. It should be considered private.
 *
 * @param name The name of the file which must be unique.
 * @param existingNames Array of existing names.
 * @returns unique name, based on the input name, which doesn't match any of the existing names. It does
 * this by appending a "-2", before the file type, or if a number already exists, a -"n+1".
 */
export const getUniqueName = (name: string, existingNames: string[]) => {
  while (existingNames.includes(name)) {
    const fileNameWithDigitSuffixMatch = name.match(
      FILE_NAME_DIGIT_SUFFIX_REGEX,
    )
    let numberToAppend = 2
    if (fileNameWithDigitSuffixMatch) {
      // File name already has a digit suffix. Increment that instead.
      numberToAppend =
        parseInt(fileNameWithDigitSuffixMatch[2].substring(1)) + 1
      // Put number before the file type ('.png') suffix if the file type is present ([3] is not null)
      name =
        fileNameWithDigitSuffixMatch[1] +
        '-' +
        numberToAppend +
        (fileNameWithDigitSuffixMatch[3] ?? '')
    } else {
      const fileNameWithoutDigitMatch = name.match(FILE_NAME_REGEX)
      if (fileNameWithoutDigitMatch) {
        // Put number before the file type ('.png').
        name =
          fileNameWithoutDigitMatch[1] +
          '-' +
          numberToAppend +
          fileNameWithoutDigitMatch[2]
      } else {
        // Otherwise, just add it to the end.
        name += '-' + numberToAppend
      }
    }
  }

  return name
}

/** Shows the error in the specified {@code errorDiv}. */
export const showError = (
  errorDiv: HTMLElement | null,
  fileInput: HTMLInputElement,
) => {
  if (errorDiv == null) {
    return
  }

  errorDiv.hidden = false
  fileInput.setAttribute('aria-invalid', 'true')

  const errorId = errorDiv.getAttribute('id')
  if (!errorId) {
    return
  }

  const ariaDescribedBy = fileInput.getAttribute('aria-describedby') ?? ''
  if (!ariaDescribedBy.includes(errorId)) {
    fileInput.setAttribute('aria-describedby', `${errorId} ${ariaDescribedBy}`)
  }
}

/** Hides the error in the specified {@code errorDiv}. */
export const hideError = (
  errorDiv: HTMLElement | null,
  fileInput: HTMLInputElement,
) => {
  if (errorDiv == null) {
    return
  }

  errorDiv.hidden = true
  fileInput.removeAttribute('aria-invalid')

  const errorId = errorDiv.getAttribute('id')
  if (!errorId) {
    return
  }

  const ariaDescribedBy = fileInput.getAttribute('aria-describedby') ?? ''
  if (ariaDescribedBy.includes(errorId)) {
    const ariaDescribedByWithoutError = ariaDescribedBy.replace(errorId, '')
    fileInput.setAttribute('aria-describedby', ariaDescribedByWithoutError)
  }
}
