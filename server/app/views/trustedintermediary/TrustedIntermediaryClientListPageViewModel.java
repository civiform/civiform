package views.trustedintermediary;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;
import views.admin.shared.LegacyPagination;

/** ViewModel for the trusted intermediary client list page (Thymeleaf). */
@Data
@Builder
public final class TrustedIntermediaryClientListPageViewModel implements BaseViewModel {

  private final String tiGroupName;

  // Targets of the tab links under the group name
  private final String clientListUrl;
  private final String accountSettingsUrl;

  // Target of the "Add new client" button
  private final String addClientUrl;

  // Target of the search form and of the "Clear search" link
  private final String searchUrl;

  // Sticky values of the search fields, empty when not searched on
  private final String nameQuery;
  private final String dayQuery;
  private final String monthQuery;
  private final String yearQuery;

  // False when the search has a partial date of birth and no name; shows the date error and hides
  // the client count heading
  private final boolean validSearch;

  private final ClientCountHeading clientCountHeading;

  // Number of clients on this page, shown by the MULTIPLE_CLIENTS heading
  private final int clientCount;

  private final ImmutableList<Client> clients;

  // Null when there are no clients, which hides the pagination control
  private final LegacyPagination pagination;

  // Id and "Error: ..." text of the flash error toast, or null when there is no error
  private final String errorToastId;
  private final String errorMessage;

  /** Which heading describes the clients listed below the search form. */
  public enum ClientCountHeading {
    /** Nothing was searched on. */
    ALL_CLIENTS,
    ONE_CLIENT,
    MULTIPLE_CLIENTS
  }

  /** One client card. */
  @Data
  @Builder
  public static final class Client {
    private final String displayName;

    // Target of the card's "Edit" link
    private final String editUrl;

    // False when the account has no applicant, which leaves most of the card empty
    private final boolean hasApplicant;

    // Id of the applicant the trusted intermediary acts as, null without an applicant
    private final Long applicantId;

    // Target of the card's "View applications" button, null without an applicant
    private final String applicationsUrl;

    // ISO date, empty when unset
    private final String dateOfBirth;

    // Formatted for display, null when the client has none
    private final String phoneNumber;

    // Null when the client has none
    private final String emailAddress;

    private final int applicationCount;

    // Comma separated names of the programs the client applied to
    private final String programNames;

    private final String notes;
  }
}
