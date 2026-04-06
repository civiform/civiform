package parsers;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Locale;
import java.util.Map;
import org.apache.pekko.util.ByteString;

/**
 * Validates uploaded file content by comparing magic bytes (file header signatures) against the
 * declared content type. Throws {@link FileUploadTypeException} when the actual file type does not
 * match what was declared, or when the detected type is not an allowed upload type.
 */
public final class FileTypeValidation {
  static final int HEADER_SIZE = 16;

  // Magic byte signatures for common file types, mapped to their MIME types.
  // Ordered longest-first so more specific signatures match before shorter prefixes.
  // References:
  //   https://en.wikipedia.org/wiki/List_of_file_signatures
  //   https://labex.io/tutorials/java-how-to-detect-file-types-in-java-438487
  private static final ImmutableMap<byte[], String> MAGIC_BYTES =
      ImmutableMap.<byte[], String>builder()
          .put(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, "image/png")
          .put(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "image/jpeg")
          .put(new byte[] {0x47, 0x49, 0x46, 0x38, 0x37, 0x61}, "image/gif") // GIF87a
          .put(new byte[] {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}, "image/gif") // GIF89a
          .put(new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D}, "application/pdf")
          .put(new byte[] {0x42, 0x4D}, "image/bmp")
          .put(
              new byte[] {0x52, 0x49, 0x46, 0x46},
              "image/webp") // RIFF header; further check for WEBP below
          .put(new byte[] {0x49, 0x49, 0x2A, 0x00}, "image/tiff") // Little-endian TIFF
          .put(new byte[] {0x4D, 0x4D, 0x00, 0x2A}, "image/tiff") // Big-endian TIFF
          .put(new byte[] {0x4D, 0x5A}, "application/x-executable") // PE/MZ executable
          .build();

  // Maps file extension specifiers (e.g. ".pdf") to MIME types
  private static final ImmutableMap<String, String> EXTENSION_TO_MIME =
      ImmutableMap.of(
          ".pdf", "application/pdf",
          ".bmp", "image/bmp",
          ".gif", "image/gif",
          ".jpg", "image/jpeg",
          ".jpeg", "image/jpeg",
          ".png", "image/png",
          ".tiff", "image/tiff",
          ".webp", "image/webp");

  private FileTypeValidation() {}

  /**
   * Validates that the file header bytes match the declared content type and that the detected type
   * is allowed.
   *
   * @param headerBytes the first bytes of the file (at least {@link #HEADER_SIZE} bytes)
   * @param declaredContentType the content type declared in the multipart upload
   * @param fileName the original filename, for error reporting
   * @param allowedFileTypeSpecifiers comma-separated file type specifiers from config (e.g.
   *     "image/*,.pdf")
   * @throws FileUploadTypeException if the detected type doesn't match declared, or is disallowed
   */
  public static void validateHeaderBytes(
      ByteString headerBytes,
      String declaredContentType,
      String fileName,
      String allowedFileTypeSpecifiers) {
    ImmutableList<String> allowedTypes = parseSpecifiers(allowedFileTypeSpecifiers);
    if (!isAllowedType(declaredContentType, allowedTypes)) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": declared content type \"%s\" is not an allowed upload type.",
              fileName, declaredContentType));
    }

    String detectedType = detectType(headerBytes);

    if (detectedType == null) {
      throw new FileUploadTypeException(
          String.format("File \"%s\": could not verify file type from content bytes.", fileName));
    }

    if (!isAllowedType(detectedType, allowedTypes)) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": detected file type \"%s\" is not an allowed upload type.",
              fileName, detectedType));
    }

    if (!typesMatch(detectedType, declaredContentType)) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": declared content type \"%s\" does not match detected type \"%s\".",
              fileName, declaredContentType, detectedType));
    }
  }

  /** Detects the MIME type from magic bytes, or returns null if unknown. */
  static String detectType(ByteString headerBytes) {
    byte[] bytes = headerBytes.toArray();

    // Special case: RIFF container could be WEBP or other formats
    if (bytes.length >= 12
        && bytes[0] == 0x52
        && bytes[1] == 0x49
        && bytes[2] == 0x46
        && bytes[3] == 0x46
        && bytes[8] == 0x57
        && bytes[9] == 0x45
        && bytes[10] == 0x42
        && bytes[11] == 0x50) {
      return "image/webp";
    }

    for (Map.Entry<byte[], String> entry : MAGIC_BYTES.entrySet()) {
      byte[] magic = entry.getKey();
      if (startsWith(bytes, magic)) {
        // Skip generic RIFF match if we didn't match WEBP above
        if (entry.getValue().equals("image/webp")) {
          continue;
        }
        return entry.getValue();
      }
    }
    return null;
  }

  private static boolean startsWith(byte[] data, byte[] prefix) {
    if (data.length < prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (data[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Parses a comma-separated file type specifiers string (e.g. "image/*,.pdf") into a list of MIME
   * type matchers. Wildcard specifiers like "image/*" become prefix matchers ("image/"), and
   * extension specifiers like ".pdf" are mapped to their MIME types.
   */
  static ImmutableList<String> parseSpecifiers(String specifiers) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String spec : Splitter.on(',').trimResults().omitEmptyStrings().split(specifiers)) {
      String normalized = spec.toLowerCase(Locale.ROOT);
      if (normalized.endsWith("/*")) {
        // Wildcard MIME type (e.g. "image/*") -> prefix match ("image/")
        builder.add(normalized.substring(0, normalized.length() - 1));
      } else if (normalized.startsWith(".")) {
        // Extension specifier (e.g. ".pdf") -> look up MIME type
        String mime = EXTENSION_TO_MIME.get(normalized);
        if (mime != null) {
          builder.add(mime);
        }
      } else {
        // Assume it's a full MIME type
        builder.add(normalized);
      }
    }
    return builder.build();
  }

  private static boolean isAllowedType(String mimeType, ImmutableList<String> allowedTypes) {
    if (mimeType == null) {
      return false;
    }
    String normalized = mimeType.toLowerCase(Locale.ROOT).trim();
    for (String allowed : allowedTypes) {
      if (allowed.endsWith("/")) {
        if (normalized.startsWith(allowed)) {
          return true;
        }
      } else {
        if (normalized.equals(allowed)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Checks if two MIME types are compatible (same family). */
  private static boolean typesMatch(String detected, String declared) {
    if (detected == null || declared == null) {
      return false;
    }
    String normalizedDetected = detected.toLowerCase(Locale.ROOT).trim();
    String normalizedDeclared = declared.toLowerCase(Locale.ROOT).trim();

    // Exact match
    if (normalizedDetected.equals(normalizedDeclared)) {
      return true;
    }

    // Same family match (e.g. image/jpeg matches image/*)
    String detectedFamily = Iterables.get(Splitter.on('/').split(normalizedDetected), 0);
    String declaredFamily = Iterables.get(Splitter.on('/').split(normalizedDeclared), 0);
    return detectedFamily.equals(declaredFamily);
  }
}
