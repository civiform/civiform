package views.trustedintermediary;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class TiDashboardPageViewModel implements BaseViewModel {
  private final String tiGroupName;
  private final long tiGroupId;
  private final List<ClientRow> clients;
  private final int page;
  private final int totalPageCount;
  private final String searchNameQuery;
  private final String searchDayQuery;
  private final String searchMonthQuery;
  private final String searchYearQuery;
  private final boolean isValidSearch;
  private final int totalClients;

  public String getDashboardUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.dashboard(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty())
        .url();
  }

  public String getClearSearchUrl() {
    return getDashboardUrl();
  }

  public String getAccountSettingsUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.accountSettings().url();
  }

  @Data
  @Builder
  public static final class ClientRow {
    private final String displayName;
    private final long accountId;
    private final Optional<Long> applicantId;
    private final String dateOfBirth;
    private final Optional<String> phoneNumber;
    private final Optional<String> email;
    private final int applicationCount;
    private final String programNames;
    private final String tiNote;

    public String getEditUrl() {
      return controllers.ti.routes.TrustedIntermediaryController.showEditClientForm(accountId)
          .url();
    }

    public String getViewApplicationsUrl() {
      return applicantId
          .map(
              id ->
                  controllers.applicant.routes.ApplicantProgramsController.indexWithApplicantId(
                          id, ImmutableList.of())
                      .url())
          .orElse("");
    }
  }

  public String getPaginationUrl(int pageNumber) {
    return controllers.ti.routes.TrustedIntermediaryController.dashboard(
            Optional.ofNullable(searchNameQuery.isEmpty() ? null : searchNameQuery),
            Optional.ofNullable(searchDayQuery.isEmpty() ? null : searchDayQuery),
            Optional.ofNullable(searchMonthQuery.isEmpty() ? null : searchMonthQuery),
            Optional.ofNullable(searchYearQuery.isEmpty() ? null : searchYearQuery),
            Optional.of(pageNumber))
        .url();
  }

  public String getAddNewClientUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.showAddClientForm(tiGroupId).url();
  }

  public String getClientListUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.dashboard(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(1))
        .url();
  }

  public String getDisplayingClientsText() {
    boolean noSearchTerms =
        searchNameQuery.isEmpty()
            && searchDayQuery.isEmpty()
            && searchMonthQuery.isEmpty()
            && searchYearQuery.isEmpty();
    if (noSearchTerms) {
      return "Displaying all clients";
    }
    if (totalClients == 1) {
      return "Displaying 1 client";
    }
    return "Displaying " + totalClients + " clients";
  }

  public String getApplicationsSubmittedText(int applicationCount) {
    if (applicationCount == 1) {
      return "1 application submitted";
    }
    return applicationCount + " applications submitted";
  }
}
