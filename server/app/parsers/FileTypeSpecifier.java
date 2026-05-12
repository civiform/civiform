package parsers;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;

/**
 * Allowlist tokens for {@link FileTypeValidation}; extension constants also define {@link
 * #MIME_BY_EXTENSION_MAP}.
 */
public enum FileTypeSpecifier {
  IMAGE_WILDCARD("image/*"),

  PNG(".png", "image/png"),
  JPG(".jpg", "image/jpeg"),
  JPEG(".jpeg", "image/jpeg"),
  GIF(".gif", "image/gif"),
  PDF(".pdf", "application/pdf"),
  BMP(".bmp", "image/bmp"),
  WEBP(".webp", "image/webp"),
  TIFF(".tiff", "image/tiff"),

  APPLICATION_PDF("application/pdf"),
  IMAGE_PNG("image/png"),
  IMAGE_JPEG("image/jpeg"),
  IMAGE_GIF("image/gif"),
  IMAGE_BMP("image/bmp"),
  IMAGE_WEBP("image/webp"),
  IMAGE_TIFF("image/tiff");

  static final ImmutableMap<String, String> MIME_BY_EXTENSION_MAP = buildMimeByExtension();

  private final String token;
  private final String mimeForExtension;

  FileTypeSpecifier(String token) {
    this(token, null);
  }

  FileTypeSpecifier(String token, String mimeForExtension) {
    this.token = token;
    this.mimeForExtension = mimeForExtension;
  }

  private static ImmutableMap<String, String> buildMimeByExtension() {
    ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
    for (FileTypeSpecifier s : values()) {
      if (s.mimeForExtension != null) {
        b.put(s.token, s.mimeForExtension);
      }
    }
    return b.build();
  }

  public String token() {
    return token;
  }

  /** MIME or wildcard string used by {@link FileTypeValidation} allowlist checks. */
  String normalizedAllowEntry() {
    return mimeForExtension != null ? mimeForExtension : token;
  }

  public static ImmutableList<FileTypeSpecifier> parseCommaSeparated(String specifiers) {
    ImmutableList.Builder<FileTypeSpecifier> out = ImmutableList.builder();
    for (String raw : Splitter.on(',').trimResults().omitEmptyStrings().split(specifiers)) {
      String part = raw.toLowerCase(Locale.ROOT);
      FileTypeSpecifier match = null;
      for (FileTypeSpecifier s : values()) {
        if (s.token.equals(part)) {
          match = s;
          break;
        }
      }
      if (match == null) {
        throw new IllegalArgumentException("Unknown file type specifier token: " + raw);
      }
      out.add(match);
    }
    return out.build();
  }
}
