package parsers;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Objects;
import lombok.Getter;

/**
 * Allowlist extensions for {@link FileTypeValidation}. Each constant defines a supported extension
 * or a wildcard such as {@code image/*}) and the MIME type or wildcard used when checking uploads.
 */
@Getter
public enum FileTypeSpecifier {
  IMAGE_WILDCARD("image/*", "image/*"),

  PNG(".png", "image/png"),
  JPG(".jpg", "image/jpeg"),
  JPEG(".jpeg", "image/jpeg"),
  GIF(".gif", "image/gif"),
  PDF(".pdf", "application/pdf"),
  BMP(".bmp", "image/bmp"),
  WEBP(".webp", "image/webp"),
  TIFF(".tiff", "image/tiff");

  static final ImmutableMap<String, String> MIME_BY_EXTENSION_MAP = buildMimeByExtension();

  private final String extension;
  private final String mimeType;

  FileTypeSpecifier(String extension, String mimeType) {
    String t = Objects.requireNonNull(extension, "extension");
    String m = Objects.requireNonNull(mimeType, "mimeType");
    if (t.isEmpty() || m.isEmpty()) {
      throw new IllegalArgumentException("extension and mimeType must be non-empty");
    }
    this.extension = t;
    this.mimeType = m;
  }

  private static ImmutableMap<String, String> buildMimeByExtension() {
    ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
    for (FileTypeSpecifier s : values()) {
      if (s.getExtension().startsWith(".")) {
        b.put(s.getExtension(), s.getMimeType());
      }
    }
    return b.build();
  }

  /**
   * Parses a comma-separated allowlist. Extension and wildcard tokens match one constant; a MIME
   * token matches every constant with that MIME (e.g. {@code image/jpeg} yields both {@link #JPG}
   * and {@link #JPEG}).
   */
  public static ImmutableList<FileTypeSpecifier> parseCommaSeparated(String specifiers) {
    ImmutableList.Builder<FileTypeSpecifier> out = ImmutableList.builder();
    for (String raw : Splitter.on(',').trimResults().omitEmptyStrings().split(specifiers)) {
      String part = raw.toLowerCase(Locale.ROOT);
      FileTypeSpecifier extensionMatch = null;
      for (FileTypeSpecifier s : values()) {
        if (s.getExtension().equals(part)) {
          extensionMatch = s;
          break;
        }
      }
      if (extensionMatch != null) {
        out.add(extensionMatch);
        continue;
      }
      ImmutableList.Builder<FileTypeSpecifier> mimeMatches = ImmutableList.builder();
      for (FileTypeSpecifier s : values()) {
        if (s.getMimeType().equals(part)) {
          mimeMatches.add(s);
        }
      }
      ImmutableList<FileTypeSpecifier> mimeList = mimeMatches.build();
      if (mimeList.isEmpty()) {
        throw new IllegalArgumentException("Unknown file type specifier extension: " + raw);
      }
      out.addAll(mimeList);
    }
    return out.build();
  }

  /** MIME or wildcard string used by {@link FileTypeValidation} allowlist checks. */
  String normalizedAllowEntry() {
    return getMimeType();
  }
}
