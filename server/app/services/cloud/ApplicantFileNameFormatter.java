package services.cloud;

import java.util.UUID;

/**
 * FileNameFormatter provides methods for formatting uploaded file names with a key prefix that
 * links them to an applicant, program, and block. These prefixed file names are stored in the
 * database and can uniquely identify files, and determine whether the files can be accessed by the
 * current user.
 */
public final class ApplicantFileNameFormatter {

  /**
   * This key uniquely identifies the file to be uploaded by the applicant and will be persisted in
   * DB. It must be unique, so be cautious with changing the logic here.
   *
   * <p>If this key is changed, also check with {@link PublicFileNameFormatter} to verify that the
   * names don't conflict, and possibly update the check in {@link
   * PublicFileNameFormatter#isFileKeyForPublicProgramImage(String)}.
   */
  public static String formatFileUploadQuestionFilename(
      long applicantId, long programId, String blockId) {

    return String.format(
        "applicant-%d/program-%d/block-%s/${filename}", applicantId, programId, blockId);
  }

  /**
   * Generates a file key with a random UUID as the filename, preserving the original file's
   * extension. Used in the new HTMX file upload flow where the server generates the storage name.
   */
  public static String formatFileUploadQuestionFilenameWithUuid(
      long applicantId, long programId, String blockId, String originalFileName) {
    String extension = "";
    int dotIndex = originalFileName.lastIndexOf('.');
    if (dotIndex >= 0) {
      extension = originalFileName.substring(dotIndex);
    }
    return String.format(
        "applicant-%d/program-%d/block-%s/%s%s",
        applicantId, programId, blockId, UUID.randomUUID(), extension);
  }

  /** Check if the formatted file key matches the applicant id */
  public static boolean isApplicantOwnedFileKey(String fileKey, long applicantId) {
    if (fileKey.isBlank()) {
      throw new IllegalArgumentException("'fileKey' must not be blank.");
    }

    if (applicantId <= 0) {
      throw new IllegalArgumentException("'applicantId' must be greater than zero.");
    }

    return fileKey.startsWith(String.format("applicant-%d/", applicantId));
  }
}
