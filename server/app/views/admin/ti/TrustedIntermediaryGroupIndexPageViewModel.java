package views.admin.ti;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the trusted intermediary group list page (Thymeleaf). */
@Data
@Builder
public final class TrustedIntermediaryGroupIndexPageViewModel implements BaseViewModel {

  // Target of the create-group form
  private final String createGroupUrl;

  // Sticky values of the create-group fields after a failed create, empty otherwise
  private final String providedName;
  private final String providedDescription;

  // "Error: ..." text of the flash error toast, or null when there is no error
  private final String errorMessage;

  private final ImmutableList<SortOption> sortOptions;

  private final ImmutableList<Group> groups;

  /** One option of the client-side sort select. */
  @Data
  @Builder
  public static final class SortOption {
    private final String value;
    private final String label;
    private final boolean selected;
  }

  /** One row of the group table. */
  @Data
  @Builder
  public static final class Group {
    private final String name;
    private final String description;
    private final int memberCount;
    private final int clientCount;
    // Target of the row's "Edit members" button
    private final String editUrl;
    // Target of the row's delete form
    private final String deleteUrl;
  }
}
