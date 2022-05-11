package services.cloud;

import views.ApplicationBaseView;

/**
 * FileNameFormatter provides methods for formatting uploaded file names with a key prefix that
 * links them to an applicant, program, and block. These prefixed file names are stored in the
 * database and can uniquely identify files, and determine whether the files can be accessed by the
 * current user.
 */
public class FileNameFormatter {

  /**
   * Returns the file's original file name, in the format "dev/${filename}" or
   * "applicant-%d/program-%d/block-%s/${filename}". These prefixes exist to uniquely identify
   * uploaded files and in the case of the second prefix, link files with their corresponding
   * applicant and program. The prefix is also used to determine if a file can be accessed in
   * FileController.java.
   */
  public static String getPrefixedOriginalFileName(String fileName, String originalFileName) {
    String[] newFileName = fileName.split("/");
    newFileName[newFileName.length - 1] = originalFileName;
    return String.join("/", newFileName);
  }

  /**
   * This key uniquely identifies the file to be uploaded by the applicant and will be persisted in
   * DB. Other parts of the system rely on the format of the key, e.g. in FileController.java we
   * check if a file can be accessed based on the key content, so be extra cautious if you want to
   * change the format.
   */
  public static String formatFileUploadQuestionFilename(ApplicationBaseView.Params params) {

    return String.format(
        "applicant-%d/program-%d/block-%s/${filename}",
        params.applicantId(), params.programId(), params.block().getId());
  }

  /**
   * This returns a prefixed filename used in the dev file upload view, which will be persisted in
   * the DB.
   */
  public static String formatDevUploadFilename() {
    return "dev/${filename}";
  }
}
