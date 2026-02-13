package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.ApplicationModel;
import services.DateConverter;
import services.applicant.ApplicantService;
import services.pagination.PageNumberPaginationSpec;
import services.pagination.PaginationResult;
import services.program.ProgramDefinition;
import services.statuses.StatusDefinitions;
import views.ApplicantUtils;
import views.admin.programs.ProgramApplicationTablePageViewModel;

/** Maps application data to the ProgramApplicationTablePageViewModel for the table page. */
public final class ProgramApplicationTablePageMapper {

  public ProgramApplicationTablePageViewModel map(
      ProgramDefinition program,
      StatusDefinitions activeStatusDefinitions,
      ImmutableList<String> allStatuses,
      PageNumberPaginationSpec paginationSpec,
      PaginationResult<ApplicationModel> paginatedApplications,
      Optional<String> search,
      Optional<String> fromDate,
      Optional<String> untilDate,
      Optional<String> applicationStatus,
      Optional<Boolean> showDownloadModal,
      Optional<String> message,
      boolean showDownloadButton,
      DateConverter dateConverter,
      ApplicantUtils applicantUtils,
      ApplicantService applicantService) {
    boolean displayStatus = allStatuses.size() > 0;
    boolean hasEligibilityEnabled = program.hasEligibilityEnabled();

    Optional<StatusDefinitions.Status> defaultStatus = activeStatusDefinitions.getDefaultStatus();

    ImmutableList<ProgramApplicationTablePageViewModel.ApplicationRowData> applicationRows =
        paginatedApplications.getPageContents().stream()
            .map(
                application -> {
                  String applicantNameWithId =
                      String.format(
                          "%s (%d)",
                          applicantUtils.getApplicantNameEnUs(
                              application.getApplicant().getApplicantDisplayName()),
                          application.id);
                  String statusText =
                      application
                          .getLatestStatus()
                          .map(
                              s ->
                                  String.format(
                                      "%s%s",
                                      s,
                                      defaultStatus
                                              .map(defaultString -> defaultString.matches(s))
                                              .orElse(false)
                                          ? " (default)"
                                          : ""))
                          .orElse("None");

                  Optional<String> eligibility = Optional.empty();
                  if (hasEligibilityEnabled) {
                    Optional<Boolean> maybeEligibilityStatus =
                        applicantService.getApplicationEligibilityStatus(application, program);
                    eligibility =
                        Optional.of(
                            maybeEligibilityStatus.isPresent() && maybeEligibilityStatus.get()
                                ? "Meets eligibility"
                                : "Doesn't meet eligibility");
                  }

                  String submitTime;
                  try {
                    submitTime =
                        dateConverter.renderDateTimeHumanReadable(application.getSubmitTime());
                  } catch (NullPointerException e) {
                    submitTime = "";
                  }

                  return ProgramApplicationTablePageViewModel.ApplicationRowData.builder()
                      .applicationId(application.id)
                      .applicantNameWithId(applicantNameWithId)
                      .programId(application.getProgram().id)
                      .search(search)
                      .fromDate(fromDate)
                      .untilDate(untilDate)
                      .currentPage(paginationSpec.getCurrentPage())
                      .applicationStatusFilter(applicationStatus)
                      .eligibilityStatus(eligibility)
                      .applicationStatus(statusText)
                      .submitTime(submitTime)
                      .submittedBy(application.getSubmitterEmail().orElse(""))
                      .build();
                })
            .collect(ImmutableList.toImmutableList());

    ImmutableList<ProgramApplicationTablePageViewModel.PaginationLink> paginationLinks =
        buildPaginationLinks(
            program.id(),
            paginationSpec.getCurrentPage(),
            paginatedApplications.getNumPages(),
            search,
            fromDate,
            untilDate,
            applicationStatus);

    ImmutableList<String> activeStatusTexts =
        activeStatusDefinitions.getStatuses().stream()
            .map(StatusDefinitions.Status::statusText)
            .collect(ImmutableList.toImmutableList());

    return ProgramApplicationTablePageViewModel.builder()
        .programAdminName(program.adminName())
        .programId(program.id())
        .showDownloadButton(showDownloadButton)
        .displayStatus(displayStatus)
        .hasEligibilityEnabled(hasEligibilityEnabled)
        .noStatusFilterUuid(repository.SubmittedApplicationFilter.NO_STATUS_FILTERS_OPTION_UUID)
        .searchValue(search)
        .fromDateValue(fromDate)
        .untilDateValue(untilDate)
        .selectedApplicationStatus(applicationStatus)
        .allApplicationStatuses(allStatuses)
        .activeStatuses(activeStatusTexts)
        .dropdownPlaceholder("Choose an option:")
        .showDownloadModal(showDownloadModal.orElse(false))
        .infoMessage(message)
        .applications(applicationRows)
        .currentPage(paginationSpec.getCurrentPage())
        .totalPages(paginatedApplications.getNumPages())
        .paginationLinks(paginationLinks)
        .build();
  }

  private ImmutableList<ProgramApplicationTablePageViewModel.PaginationLink> buildPaginationLinks(
      long programId,
      int currentPage,
      int totalPages,
      Optional<String> search,
      Optional<String> fromDate,
      Optional<String> untilDate,
      Optional<String> applicationStatus) {
    ImmutableList.Builder<ProgramApplicationTablePageViewModel.PaginationLink> builder =
        ImmutableList.builder();
    for (int i = 1; i <= totalPages; i++) {
      builder.add(
          ProgramApplicationTablePageViewModel.PaginationLink.builder()
              .pageNumber(i)
              .programId(programId)
              .search(search)
              .fromDate(fromDate)
              .untilDate(untilDate)
              .applicationStatus(applicationStatus)
              .isCurrentPage(i == currentPage)
              .build());
    }
    return builder.build();
  }
}
