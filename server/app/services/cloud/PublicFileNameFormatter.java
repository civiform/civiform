package services.cloud;

/**
 * Formats file names for files that will be **publicly accessible**.
 *
 * <p>See {@link ApplicantFileNameFormatter} for formatting applicant file names.
 */
public class PublicFileNameFormatter {
  /**
   * This key uniquely identifies the image to be used for a specific program and will be persisted
   * in the DB. It must be unique, so be cautious with changing the logic here.
   */
  public static String formatPublicProgramImageFilename(long programId) {
    return String.format("program-summary-image/program-%d/${filename}", programId);
  }

  /**
   * Returns true if the file key suitably identifies a public program image file and false
   * otherwise.
   *
   * <p>{@link PublicStorageClient} implementations must use this check before accessing a file.
   */
  public static boolean isFileKeyForPublicProgramImage(String fileKey) {
    return fileKey.startsWith("program-summary-image/program-");
  }
}
