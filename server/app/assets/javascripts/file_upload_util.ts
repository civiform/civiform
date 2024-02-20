import {assertNotNull} from './util'

/** @fileoverview Collection of utility functions for working with file upload.
 */

const MAX_FILE_SIZE_MB_ATTR = 'data-file-limit-mb'

/**
 * Returns true if the file currently uploaded to {@code inputElement} is too large and false otherwise.
 *
 * Note that this will only check the first file uploaded to the input element because all our current file uploads only allow
 * a single file to be uploaded.
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
  const maxFileSizeBytes = maxFileSizeMb * 1024 * 1024

  return file.size > maxFileSizeBytes
}
