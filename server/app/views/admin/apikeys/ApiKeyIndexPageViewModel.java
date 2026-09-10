package views.admin.apikeys;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the API keys index page (Thymeleaf). */
@Data
@Builder
public final class ApiKeyIndexPageViewModel implements BaseViewModel {

  // Page heading, always "API Keys"
  private final String title;

  // Target of the "New API key" button
  private final String newKeyUrl;

  // Active / Retired / Expired links, in the order the legacy view rendered them
  private final ImmutableList<FilterLink> filterLinks;

  private final boolean hasApiKeys;
  // Lowercase name of the selected status listing, e.g. "active"; used in the
  // no-keys-found message.
  private final String selectedStatusLowercase;

  private final ImmutableList<ApiKey> apiKeys;

  /** A link to one of the API key status listings. */
  @Data
  @Builder
  public static final class FilterLink {
    private final String text;
    private final String href;
    private final boolean selected;
  }

  /** A single API key card. */
  @Data
  @Builder
  public static final class ApiKey {
    private final String name;
    // Lowercase status word rendered next to the name, e.g. "active"
    private final String status;
    // Slugified key name; the template derives the browser tests' element ids from it
    private final String nameSlug;

    private final String keyId;
    // Comma-joined allowed subnets, e.g. "1.1.1.1/32, 2.2.2.2/32"
    private final String subnets;
    private final String expirationDate;

    private final String createdDate;
    private final String createdBy;
    // "N/A" when the key has never been used
    private final String lastCallIp;
    private final Long callCount;

    private final boolean retired;
    // Null unless the key is retired.
    private final String retiredDate;
    // Retire form target, null for keys that are already retired.
    private final String retireUrl;

    private final ImmutableList<ProgramGrant> grants;
  }

  /** One row of a key's program grants table. */
  @Data
  @Builder
  public static final class ProgramGrant {
    // Null when the granted slug no longer matches a program name.
    private final String programName;
    private final String programSlug;
    private final String permission;
  }
}
