package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class ProgramApplicationTablePageViewModel implements BaseViewModel {
  private final String programAdminName;
  private final long programId;
  private final boolean showDownloadButton;
  private final boolean displayStatus;
  private final boolean hasEligibilityEnabled;
  private final String noStatusFilterUuid;

  // Filter values
  private final Optional<String> searchValue;
  private final Optional<String> fromDateValue;
  private final Optional<String> untilDateValue;
  private final Optional<String> selectedApplicationStatus;

  // Status options for filter dropdown
  private final ImmutableList<String> allApplicationStatuses;

  // Status options for bulk update dropdown
  private final ImmutableList<String> activeStatuses;
  private final String dropdownPlaceholder;

  // Download modal
  private final boolean showDownloadModal;

  // Alert message (for bulk status update results)
  private final Optional<String> infoMessage;

  // Applications
  private final ImmutableList<ApplicationRowData> applications;

  // Pagination
  private final int currentPage;
  private final int totalPages;
  private final ImmutableList<PaginationLink> paginationLinks;

  public String getFormActionUrl() {
    return routes.AdminApplicationController.index(
            programId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty())
        .url();
  }

  public String getClearFiltersUrl() {
    return getFormActionUrl();
  }

  public String getDownloadCsvUrl() {
    return routes.AdminApplicationController.downloadAll(
            programId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty())
        .url();
  }

  public String getDownloadJsonUrl() {
    return routes.AdminApplicationController.downloadAllJson(
            programId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty())
        .url();
  }

  public String getBulkStatusUpdateUrl() {
    return routes.AdminApplicationController.updateStatuses(programId).url();
  }

  @Data
  @Builder
  public static final class ApplicationRowData {
    private final long applicationId;
    private final String applicantNameWithId;
    private final long programId;
    private final Optional<String> search;
    private final Optional<String> fromDate;
    private final Optional<String> untilDate;
    private final int currentPage;
    private final Optional<String> applicationStatusFilter;
    private final Optional<String> eligibilityStatus;
    private final String applicationStatus;
    private final String submitTime;
    private final String submittedBy;

    public String getViewUrl() {
      return routes.AdminApplicationController.show(
              programId,
              applicationId,
              search,
              fromDate,
              untilDate,
              Optional.of(currentPage),
              applicationStatusFilter)
          .url();
    }
  }

  @Data
  @Builder
  public static final class PaginationLink {
    private final int pageNumber;
    private final long programId;
    private final Optional<String> search;
    private final Optional<String> fromDate;
    private final Optional<String> untilDate;
    private final Optional<String> applicationStatus;
    private final boolean isCurrentPage;

    public String getUrl() {
      return routes.AdminApplicationController.index(
              programId,
              search,
              Optional.of(pageNumber),
              fromDate,
              untilDate,
              applicationStatus,
              Optional.empty(),
              Optional.empty(),
              Optional.empty())
          .url();
    }
  }
}
