package parsers;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.util.ByteString;
import org.apache.tika.Tika;

/**
 * Validates uploaded file content by looking up the MIME type for the filename's extension,
 * checking it against the allowed upload types, and confirming with Apache Tika that the file's
 * bytes actually match that type. Throws {@link FileUploadTypeException} on any mismatch.
 */
public final class FileTypeValidation {

  @Inject
  public FileTypeValidation() {}

  private static final Tika TIKA = new Tika();

  private static final int HEADER_SIZE = 16;

  private static final ImmutableMap<String, String> MIME_BY_EXTENSION =
      ImmutableMap.<String, String>builder()
          .put(".png", "image/png")
          .put(".jpg", "image/jpeg")
          .put(".jpeg", "image/jpeg")
          .put(".gif", "image/gif")
          .put(".pdf", "application/pdf")
          .put(".bmp", "image/bmp")
          .put(".webp", "image/webp")
          .put(".tiff", "image/tiff")
          // xlsx is a ZIP container, and Tika's byte-prefix detection reports it as
          // application/zip. Accepted risk: a plain .zip renamed to .xlsx passes as xlsx.
          .put(".xlsx", "application/zip")
          .build();

  /** Parses a comma-separated list of MIME types, wildcards, and extensions. */
  public static ImmutableList<String> parseSpecifiers(String specifiers) {
    return Splitter.on(',')
        .trimResults()
        .omitEmptyStrings()
        .splitToStream(specifiers)
        .map(spec -> spec.toLowerCase(Locale.ROOT))
        .flatMap(spec -> Stream.ofNullable(normalizeSpecifier(spec)))
        .collect(ImmutableList.toImmutableList());
  }

  private static String normalizeSpecifier(String normalized) {
    if (normalized.endsWith("/*")) {
      return normalized;
    }
    if (normalized.startsWith(".")) {
      return MIME_BY_EXTENSION.get(normalized);
    }
    return normalized;
  }

  private static String extensionOf(String fileName) {
    if (fileName == null) {
      return null;
    }
    int dot = fileName.lastIndexOf('.');
    if (dot < 0) {
      return null;
    }
    return fileName.substring(dot).toLowerCase(Locale.ROOT);
  }

  private static boolean isAllowedType(String mimeType, ImmutableList<String> allowedTypes) {
    return allowedTypes.stream()
        .anyMatch(
            allowed ->
                allowed.endsWith("/*")
                    ? mimeType.startsWith(allowed.substring(0, allowed.length() - 1))
                    : mimeType.equals(allowed));
  }

  /**
   * Returns a pass-through {@link Flow} that captures the first {@link #HEADER_SIZE} bytes of the
   * stream and validates them against the filename extension and allowed upload types. Throws
   * {@link FileUploadTypeException} on the validating element if the file is not acceptable.
   */
  public Flow<ByteString, ByteString, ?> sniffingFlow(
      String fileName, ImmutableList<String> allowedTypes) {
    AtomicReference<ByteString> headerBytes = new AtomicReference<>(ByteString.emptyByteString());

    return Flow.of(ByteString.class)
        .map(
            bytes -> {
              ByteString currentHeader = headerBytes.get();

              if (currentHeader.size() < FileTypeValidation.HEADER_SIZE) {
                int remaining = FileTypeValidation.HEADER_SIZE - currentHeader.size();
                ByteString slice = bytes.take(remaining);
                headerBytes.set(currentHeader.concat(slice));

                // Validate once we have enough bytes
                if (headerBytes.get().size() >= FileTypeValidation.HEADER_SIZE) {
                  validateHeaderBytes(headerBytes.get(), fileName, allowedTypes);
                }
              }

              return bytes;
            });
  }

  void validateHeaderBytes(
      ByteString headerBytes, String fileName, ImmutableList<String> allowedTypes) {

    String extension = extensionOf(fileName);
    String expected = MIME_BY_EXTENSION.get(extension);
    if (expected == null) {
      throw new FileUploadTypeException(
          String.format(
              "File %s: extension %s is not a supported upload type.", fileName, extension));
    }

    if (!isAllowedType(expected, allowedTypes)) {
      throw new FileUploadTypeException(
          String.format(
              "File %s: file type %s is not an allowed upload type.", fileName, expected));
    }

    String detected = TIKA.detect(headerBytes.toArray());
    if (!expected.equals(detected)) {
      throw new FileUploadTypeException(
          String.format(
              "File %s: content bytes do not match extension %s (detected %s).",
              fileName, extension, detected));
    }
  }
}
