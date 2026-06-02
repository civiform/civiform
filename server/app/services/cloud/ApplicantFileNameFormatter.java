package services.cloud;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;

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
   * Returns the filename prefix string to identify this applicant's file in a filename.
   *
   * <p>The result will have a trailing / to ensure it does not match entries in which it otherwise
   * would be a substring of other applicant's files.
   */
  public static String formatFilenameApplicantLookupPrefixString(long applicantId) {
    return String.format("applicant-%d/", applicantId);
  }

  /**
   * Generates a file key with a random UUID as the filename, preserving the original file's
   * extension. Used in the new HTMX file upload flow where the server generates the storage name.
   */
  public static String formatFileUploadQuestionFilenameWithUuid(
      long applicantId, long programId, String blockId, String originalFileName) {
    checkArgument(applicantId > 0, "applicantId must be > 0");
    checkArgument(programId > 0, "programId must be > 0");
    checkArgument(isNotBlank(blockId), "blockId must not be null or empty");
    checkArgument(isNotBlank(originalFileName), "originalFileName must not be null or empty");
    return String.format(
        "applicant-%d/program-%d/block-%s/%s.%s",
        applicantId,
        programId,
        blockId,
        UUID.randomUUID(),
        FilenameUtils.getExtension(originalFileName));
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

  /**
   * Builds a {@code Content-Disposition} value for presigned GETs so cloud storage can return the
   * applicant's original file name without exceeding ISO-8859-1 header limits.
   *
   * <p>Uses {@code filename*=UTF-8''...} (RFC 5987) so the full UTF-8 name is preserved (e.g. macOS
   * screenshot names with narrow no-break spaces).
   */
  public static String buildResponseContentDisposition(String displayFileName) {
    if (displayFileName.isBlank()) {
      throw new IllegalArgumentException("'displayFileName' must not be blank.");
    }
    String filename =
        URLEncoder.encode(displayFileName, StandardCharsets.UTF_8).replace("+", "%20");
    return String.format("inline; filename*=UTF-8''%s", filename);
  }
}
