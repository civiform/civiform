package services.cloud;

/**
 * FileNameFormatter provides methods for formatting uploaded file names with a key prefix that
 * links them to an applicant, program, and block. These prefixed file names are stored in the
 * database and can uniquely identify files, and determine whether the files can be accessed by the
 * current user.
 */
public class FileNameFormatter {

  /**
   * This key uniquely identifies the file to be uploaded by the applicant and will be persisted in
   * DB. It must be unique, so be cautious with changing the logic here.
   */
  public static String formatFileUploadQuestionFilename(
      long applicantId, long programId, String blockId) {

    return String.format(
        "applicant-%d/program-%d/block-%s/${filename}", applicantId, programId, blockId);
  }
}
