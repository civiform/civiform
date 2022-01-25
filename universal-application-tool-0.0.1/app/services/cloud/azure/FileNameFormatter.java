package services.cloud.azure;

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
}
