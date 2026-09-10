package mapping.admin.apikeys;

import java.util.Locale;

/**
 * Status of an API key, doubling as the filter tabs on the API keys index page. Declaration order
 * is the order the legacy view rendered the filter links in.
 */
public enum ApiKeyStatus {
  ACTIVE("Active"),
  RETIRED("Retired"),
  EXPIRED("Expired");

  private final String displayName;

  ApiKeyStatus(String displayName) {
    this.displayName = displayName;
  }

  /** Capitalized name, as the legacy view's filter links and {@code selectedStatus} used it. */
  public String displayName() {
    return displayName;
  }

  /** Lowercase name, as rendered next to a key's name and in the no-keys-found message. */
  public String lowercaseName() {
    return displayName.toLowerCase(Locale.ROOT);
  }
}
