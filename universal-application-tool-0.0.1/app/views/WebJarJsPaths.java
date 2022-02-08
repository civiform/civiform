package views;

import java.util.Optional;

/** Enum for holding the paths for web JARs needed throughout the code base. */
public enum WebJarJsPaths {
  AZURE_STORAGE_BLOB("lib/azure__storage-blob/browser/azure-storage-blob.min.js"),
  ;

  private final String webJarPath;

  WebJarJsPaths(String webJarPath) {
    this.webJarPath = webJarPath;
  }

  /** Returns the enum associated with the provided String value. */
  public static Optional<WebJarJsPaths> forString(String string) {
    for (WebJarJsPaths path : WebJarJsPaths.values()) {
      if (path.getString().equals(string)) {
        return Optional.of(path);
      }
    }
    return Optional.empty();
  }

  /** Returns the string value associated with the enum */
  public String getString() {
    return webJarPath;
  }
}
