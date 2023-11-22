package services.cloud;

/**
 * Formats file names for files that will be **publicly accessible**.
 *
 * <p>See {@link FileNameFormatter} for formatting applicant files.
 */
public class PublicFileNameFormatter {
  /**
   * This key uniquely identifies the image to be used for a specific program and will be persisted
   * in the DB. It must be unique, so be cautious with changing the logic here. TODO: Should this go
   * in separate file?
   */
  public static String formatPublicProgramImageFilename(long programId) {
    return String.format("program-card-images/program-%d/${filename}", programId);
  }

  /**
   * Returns true if the file key suitably identifies a public file, and false if the key is
   * incorrectly formatted for public files.
   *
   * <p>Must be checked in {@link PublicStorageClient} implementations before accessing a file.
   */
  public static boolean isFileKeyForPublicUse(String fileKey) {
    return fileKey.startsWith("program-card-images") && !fileKey.contains("applicant-");
  }
}
