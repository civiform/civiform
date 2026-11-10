package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import mapping.admin.shared.LegacyPaginationMapper;
import models.ApplicationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import repository.SubmittedApplicationFilter;
import services.DateConverter;
import services.MessageKey;
import services.applicant.ApplicantService;
import services.pagination.PageNumberPaginationSpec;
import services.pagination.PaginationResult;
import services.program.ProgramDefinition;
import services.statuses.StatusDefinitions;
import views.ApplicantUtils;
import views.admin.programs.ProgramApplicationListPageViewModel;
import views.admin.programs.ProgramApplicationListPageViewModel.ApplicationRow;
import views.admin.programs.ProgramApplicationTableView.RenderFilterParams;
import views.admin.shared.LegacyPagination;

/** Maps data to the ProgramApplicationListPageViewModel for the program applications list page. */
public final class ProgramApplicationListPageMapper {

  private static final Logger log = LoggerFactory.getLogger(ProgramApplicationListPageMapper.class);

  /**
   * Maps one page of a program's submitted applications to the view model.
   *
   * @param program the program whose applications are listed
   * @param activeStatusDefinitions the program's currently active statuses, for the bulk update
   *     form and the default status marker
   * @param allPossibleProgramApplicationStatuses every status ever applied to the program's
   *     applications, for the status filter
   * @param paginationSpec the requested page
   * @param paginatedApplications the applications on that page
   * @param filterParams the active filter values
   * @param showDownloadButton whether the current admin may download application data
   * @param showDownloadModal whether to open the download modal on load
   * @param message info alert text after a bulk status update
   * @param applicantService computes each application's eligibility status
   * @param dateConverter renders submission times
   * @param enUsMessages en-US messages, for the guest applicant name and the bulk status selector
   *     placeholder; the legacy view rendered both in English regardless of locale
   */
  public ProgramApplicationListPageViewModel map(
      ProgramDefinition program,
      StatusDefinitions activeStatusDefinitions,
      ImmutableList<String> allPossibleProgramApplicationStatuses,
      PageNumberPaginationSpec paginationSpec,
      PaginationResult<ApplicationModel> paginatedApplications,
      RenderFilterParams filterParams,
      boolean showDownloadButton,
      boolean showDownloadModal,
      Optional<String> message,
      ApplicantService applicantService,
      DateConverter dateConverter,
      Messages enUsMessages) {
    Optional<StatusDefinitions.Status> defaultStatus = activeStatusDefinitions.getDefaultStatus();
    int currentPage = paginationSpec.getCurrentPage();

    ImmutableList<ApplicationRow> applicationRows =
        paginatedApplications.getPageContents().stream()
            .map(
                application ->
                    buildRow(
                        application,
                        program,
                        defaultStatus,
                        filterParams,
                        currentPage,
                        applicantService,
                        dateConverter,
                        enUsMessages))
            .collect(ImmutableList.toImmutableList());

    LegacyPagination pagination = null;
    if (paginatedApplications.getNumPages() > 1) {
      pagination =
          new LegacyPaginationMapper()
              .map(
                  currentPage,
                  paginatedApplications.getNumPages(),
                  pageNumber ->
                      routes.AdminApplicationController.index(
                              program.id(),
                              filterParams.search(),
                              Optional.of(pageNumber),
                              filterParams.fromDate(),
                              filterParams.untilDate(),
                              filterParams.selectedApplicationStatus(),
                              /* selectedApplicationUri= */ Optional.empty(),
                              /* showDownloadModal= */ Optional.empty(),
                              /* message= */ Optional.empty())
                          .url());
    }

    return ProgramApplicationListPageViewModel.builder()
        .programName(program.adminName())
        .indexUrl(
            routes.AdminApplicationController.index(
                    program.id(),
                    /* search= */ Optional.empty(),
                    /* page= */ Optional.empty(),
                    /* fromDate= */ Optional.empty(),
                    /* untilDate= */ Optional.empty(),
                    /* applicationStatus= */ Optional.empty(),
                    /* selectedApplicationUri= */ Optional.empty(),
                    /* showDownloadModal= */ Optional.empty(),
                    /* message= */ Optional.empty())
                .url())
        .fromDate(filterParams.fromDate().orElse(""))
        .untilDate(filterParams.untilDate().orElse(""))
        .search(filterParams.search().orElse(""))
        .selectedApplicationStatus(filterParams.selectedApplicationStatus().orElse(""))
        .noStatusFilterOptionValue(SubmittedApplicationFilter.NO_STATUS_FILTERS_OPTION_UUID)
        .applicationStatusOptions(allPossibleProgramApplicationStatuses)
        .showDownloadButton(showDownloadButton)
        .showDownloadModal(showDownloadModal)
        .downloadAllCsvUrl(
            routes.AdminApplicationController.downloadAll(
                    program.id(),
                    /* search= */ Optional.empty(),
                    /* fromDate= */ Optional.empty(),
                    /* untilDate= */ Optional.empty(),
                    /* applicationStatus= */ Optional.empty(),
                    /* ignoreFilters= */ Optional.empty())
                .url())
        .downloadAllJsonUrl(
            routes.AdminApplicationController.downloadAllJson(
                    program.id(),
                    /* search= */ Optional.empty(),
                    /* fromDate= */ Optional.empty(),
                    /* untilDate= */ Optional.empty(),
                    /* applicationStatus= */ Optional.empty(),
                    /* ignoreFilters= */ Optional.empty())
                .url())
        .updateStatusesUrl(routes.AdminApplicationController.updateStatuses(program.id()).url())
        .bulkStatusPlaceholder(enUsMessages.at(MessageKey.DROPDOWN_PLACEHOLDER.getKeyName()))
        .bulkStatusOptions(
            activeStatusDefinitions.getStatuses().stream()
                .map(StatusDefinitions.Status::statusText)
                .collect(ImmutableList.toImmutableList()))
        .statusUpdateMessage(message.orElse(null))
        .eligibilityEnabled(program.hasEligibilityEnabled())
        .applicationRows(applicationRows)
        .pagination(pagination)
        .build();
  }

  private static ApplicationRow buildRow(
      ApplicationModel application,
      ProgramDefinition program,
      Optional<StatusDefinitions.Status> defaultStatus,
      RenderFilterParams filterParams,
      int currentPage,
      ApplicantService applicantService,
      DateConverter dateConverter,
      Messages enUsMessages) {
    String statusText =
        application
            .getLatestStatus()
            .map(
                status ->
                    defaultStatus.map(def -> def.matches(status)).orElse(false)
                        ? status + " (default)"
                        : status)
            .orElse("None");

    String viewUrl =
        routes.AdminApplicationController.show(
                application.getProgram().id,
                application.id,
                filterParams.search(),
                filterParams.fromDate(),
                filterParams.untilDate(),
                Optional.of(currentPage),
                filterParams.selectedApplicationStatus())
            .url();

    return ApplicationRow.builder()
        .id(application.id)
        .applicantNameWithApplicationId(
            String.format(
                "%s (%d)",
                ApplicantUtils.getApplicantName(
                    application.getApplicant().getApplicantDisplayName(), enUsMessages),
                application.id))
        .viewUrl(viewUrl)
        .meetsEligibility(
            applicantService.getApplicationEligibilityStatus(application, program).orElse(false))
        .statusText(statusText)
        .submitTime(renderSubmitTime(application, dateConverter))
        .submitterEmail(application.getSubmitterEmail().orElse(""))
        .build();
  }

  /** The legacy view rendered an empty cell (and logged) for applications without a submit time. */
  private static String renderSubmitTime(
      ApplicationModel application, DateConverter dateConverter) {
    if (application.getSubmitTime() == null) {
      log.error("Application {} submitted without submission time marked.", application.id);
      return "";
    }

    return dateConverter.renderDateTimeHumanReadable(application.getSubmitTime());
  }
}
