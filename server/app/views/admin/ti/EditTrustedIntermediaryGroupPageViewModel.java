package views.admin.ti;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the trusted intermediary group edit page (Thymeleaf). */
@Data
@Builder
public final class EditTrustedIntermediaryGroupPageViewModel implements BaseViewModel {

  private final String groupName;
  private final String groupDescription;

  // Target of the "Back to all intermediaries" link
  private final String backUrl;
  // Target of the add-member form
  private final String addIntermediaryUrl;
  // Target of every member row's delete form; the row's hidden accountId selects the member
  private final String removeIntermediaryUrl;

  // Sticky value of the email field after a failed add, empty otherwise
  private final String providedEmailAddress;

  // "Error: ..." text of the flash error toast, or null when there is no error
  private final String errorMessage;

  private final ImmutableList<Member> members;

  /** One member row of the group table. */
  @Data
  @Builder
  public static final class Member {
    private final String displayName;
    private final String emailAddress;
    // False until the account has signed in at least once (owns no applicants yet)
    private final boolean signedIn;
    private final String accountId;
  }
}
