import {assertNotNull} from './util'

/** @fileoverview Collection of utility functions for working with file upload.
 * Can be used for both the admin UI and applicant UI.
 */

// This should be kept in sync with views/fileupload/FileUploadViewStrategy.FILE_LIMIT_ATTR.
const MAX_FILE_SIZE_MB_ATTR = 'data-file-limit-mb'

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
