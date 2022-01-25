package services.cloud;

import views.applicant.ApplicantProgramBlockEditView;

public class FileNameFormatter {

  /**
   * Returns the file's original file name, in the format "dev/${filename}" or
   * "applicant-%d/program-%d/block-%s/${filename}".
   */
  public static String getPrefixedOriginalFileName(String fileName, String originalFileName) {
    String[] newFileName = fileName.split("/");
    newFileName[newFileName.length - 1] = originalFileName;
    return String.join("/", newFileName);
  }

  public static String formatFileUploadQuestionFilename(
      ApplicantProgramBlockEditView.Params params) {
    // Note: This key uniquely identifies the file to be uploaded by the applicant and will be
    // persisted in DB. Other parts of the system rely on the format of the key, e.g. in
    // FileController.java we check if a file can be accessed based on the key content, so be extra
    // cautious if you want to change the format.
    return String.format(
        "applicant-%d/program-%d/block-%s/${filename}",
        params.applicantId(), params.programId(), params.block().getId());
  }

  public static String formatDevUploadFilename() {
    return "dev/${filename}";
  }
}
