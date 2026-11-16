package views.trustedintermediary;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the trusted intermediary account settings page (Thymeleaf). */
@Data
@Builder
public final class TrustedIntermediaryAccountSettingsPageViewModel implements BaseViewModel {

  private final String tiGroupName;

  // Targets of the tab links under the group name
  private final String clientListUrl;
  private final String accountSettingsUrl;

  // Rows of the organization members table, sorted by display name
  private final ImmutableList<Member> members;

  // Id and "Error: ..." text of the flash error toast, or null when there is no error
  private final String errorToastId;
  private final String errorMessage;

  /** One row of the organization members table. */
  @Data
  @Builder
  public static final class Member {
    private final String displayName;

    // Null when the member has no email address
    private final String emailAddress;

    // False until the member has signed in for the first time
    private final boolean signedIn;
  }
}
