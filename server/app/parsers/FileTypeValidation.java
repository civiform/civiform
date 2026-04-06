package parsers;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.pekko.util.ByteString;

/**
 * Validates uploaded file content by comparing magic bytes (file header signatures) against the
 * declared content type. Throws {@link FileUploadTypeException} when the actual file type does not
 * match what was declared, or when the detected type is not an allowed upload type.
 */
public final class FileTypeValidation {

  static final int HEADER_SIZE = 16;

  /** All supported file types. */
  private static final ImmutableList<FileType> FILE_TYPES =
      ImmutableList.of(
          FileType.of(
              ".png",
              "image/png",
              "image/*",
              new byte[][] {{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}}),
          FileType.of(
              ".jpg",
              "image/jpeg",
              "image/*",
              new byte[][] {{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}),
          FileType.of(
              ".jpeg",
              "image/jpeg",
              "image/*",
              new byte[][] {{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}),
          FileType.of(
              ".gif",
              "image/gif",
              "image/*",
              new byte[][] {
                {(byte) 0x47, 0x49, 0x46, 0x38, 0x37, 0x61},
                {(byte) 0x47, 0x49, 0x46, 0x38, 0x39, 0x61}
              }),
          FileType.of(
              ".pdf", "application/pdf", null, new byte[][] {{0x25, 0x50, 0x44, 0x46, 0x2D}}),
          FileType.of(".bmp", "image/bmp", "image/*", new byte[][] {{0x42, 0x4D}}),
          FileType.of(
              ".webp",
              "image/webp",
              "image/*",
              new byte[][] {
                {0x52, 0x49, 0x46, 0x46} // RIFF
              }),
          FileType.of(
              ".tiff",
              "image/tiff",
              "image/*",
              new byte[][] {
                {0x49, 0x49, 0x2A, 0x00},
                {0x4D, 0x4D, 0x00, 0x2A}
              }),
          FileType.of(".zip", "application/zip", null, new byte[][] {{0x50, 0x4B, 0x03, 0x04}}));

  private static final ImmutableSet<String> ZIP_BASED_TYPES =
      ImmutableSet.of(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/vnd.openxmlformats-officedocument.presentationml.presentation");

  /** Extension → FileType lookup (moved here to avoid static init cycle). */
  private static final ImmutableMap<String, FileType> BY_EXTENSION =
      FILE_TYPES.stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  t -> t.extension().toLowerCase(Locale.ROOT), t -> t, (a, b) -> a));

  private FileTypeValidation() {}

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

    FileType detected = detectType(headerBytes);

    if (detected == null) {
      throw new FileUploadTypeException(
          String.format("File \"%s\": could not verify file type from content bytes.", fileName));
    }

    if (!isAllowedType(detected.mimeType(), allowedTypes)) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": detected file type \"%s\" is not an allowed upload type.",
              fileName, detected.mimeType()));
    }

    if (!typesMatch(detected.mimeType(), declaredContentType)) {
      throw new FileUploadTypeException(
          String.format(
              "File \"%s\": declared content type \"%s\" does not match detected type \"%s\".",
              fileName, declaredContentType, detected.mimeType()));
    }
  }

  /** Detects the file type from magic bytes. */
  static FileType detectType(ByteString headerBytes) {
    if (headerBytes == null) {
      return null;
    }

    if (isRiffWebp(headerBytes)) {
      return FILE_TYPES.stream()
          .filter(t -> "image/webp".equals(t.mimeType()))
          .findFirst()
          .orElse(null);
    }

    for (FileType type : FILE_TYPES) {
      if (type.matches(headerBytes)) {
        return type;
      }
    }

    return null;
  }

  private static boolean isRiffWebp(ByteString bytes) {
    return bytes.length() >= 12
        && bytes.apply(0) == 0x52
        && bytes.apply(1) == 0x49
        && bytes.apply(2) == 0x46
        && bytes.apply(3) == 0x46
        && bytes.apply(8) == 0x57
        && bytes.apply(9) == 0x45
        && bytes.apply(10) == 0x42
        && bytes.apply(11) == 0x50;
  }

  static ImmutableList<String> parseSpecifiers(String specifiers) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();

    for (String spec : Splitter.on(',').trimResults().omitEmptyStrings().split(specifiers)) {
      String normalized = spec.toLowerCase(Locale.ROOT);

      if (normalized.endsWith("/*")) {
        builder.add(normalized.substring(0, normalized.length() - 1));
      } else if (normalized.startsWith(".")) {
        String mime = toMime(normalized);
        if (mime != null) {
          builder.add(mime);
        }
      } else {
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
      } else if (normalized.equals(allowed)) {
        return true;
      }
    }

    return false;
  }

  private static boolean typesMatch(String detected, String declared) {
    if (detected == null || declared == null) {
      return false;
    }

    String d = detected.toLowerCase(Locale.ROOT).trim();
    String dec = declared.toLowerCase(Locale.ROOT).trim();

    if (d.equals(dec)) {
      return true;
    }

    if (d.equals("application/zip") && ZIP_BASED_TYPES.contains(dec)) {
      return true;
    }

    String detectedFamily = Iterables.get(Splitter.on('/').split(d), 0);
    String declaredFamily = Iterables.get(Splitter.on('/').split(dec), 0);

    return detectedFamily.equals(declaredFamily);
  }

  /** Map helpers moved here to avoid static initialization cycles. */
  static FileType fromExtension(String extension) {
    if (extension == null) {
      return null;
    }
    return BY_EXTENSION.get(extension.toLowerCase(Locale.ROOT));
  }

  static String toMime(String extension) {
    FileType type = fromExtension(extension);
    return type != null ? type.mimeType() : null;
  }

  /** Represents a file type with one or more magic byte signatures. */
  private record FileType(
      String extension, String mimeType, String wildcardMime, List<byte[]> magicSignatures) {

    static FileType of(
        String extension, String mimeType, String wildcardMime, byte[][] signatures) {
      return new FileType(extension, mimeType, wildcardMime, Arrays.asList(signatures));
    }

    boolean matches(ByteString data) {
      for (byte[] sig : magicSignatures) {
        if (data.length() < sig.length) {
          continue;
        }

        boolean match = true;
        for (int i = 0; i < sig.length; i++) {
          if (data.apply(i) != sig[i]) {
            match = false;
            break;
          }
        }

        if (match) {
          return true;
        }
      }
      return false;
    }
  }
}
