package services.cloud;

/**
 * Formats file names for files that will be **publicly accessible**.
 *
 * <p>See {@link ApplicantFileNameFormatter} for formatting applicant file names.
 */
public final class PublicFileNameFormatter {
  private static final String PROGRAM_IMAGE_FILE_KEY_PREFIX = "program-summary-image/program-";

  /**
   * This key uniquely identifies the image to be used for a specific program and will be persisted
   * in the DB. It must be unique, so be cautious with changing the logic here.
   *
   * <p>If this key is changed, also check with {@link ApplicantFileNameFormatter} to verify that
   * the names don't conflict.
   */
  public static String formatPublicProgramImageFileKey(long programId) {
    return String.format("%s%d/${filename}", PROGRAM_IMAGE_FILE_KEY_PREFIX, programId);
  }

  /**
   * Returns true if the file key suitably identifies a public program image file and false
   * otherwise.
   *
   * <p>{@link PublicStorageClient} implementations must use this check before accessing a file.
   */
  public static boolean isFileKeyForPublicProgramImage(String fileKey) {
    return fileKey.startsWith(PROGRAM_IMAGE_FILE_KEY_PREFIX);
  }
}
