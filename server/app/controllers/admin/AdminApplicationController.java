package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.admin.programs.ProgramApplicationView.CURRENT_STATUS;
import static views.admin.programs.ProgramApplicationView.NEW_STATUS;
import static views.admin.programs.ProgramApplicationView.NOTE;
import static views.admin.programs.ProgramApplicationView.SEND_EMAIL;

import annotations.BindingAnnotations.Now;
import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import controllers.BadRequestException;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.admin.BulkStatusUpdateForm;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.inject.Inject;
import models.ApplicationModel;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.SubmittedApplicationFilter;
import repository.TimeFilter;
import repository.VersionRepository;
import services.DateConverter;
import services.PaginationResult;
import services.UrlUtils;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.application.ApplicationEventDetails;
import services.applications.AccountHasNoEmailException;
import services.applications.PdfExporterService;
import services.applications.ProgramAdminApplicationService;
import services.applications.StatusEmailNotFoundException;
import services.export.CsvExporterService;
import services.export.JsonExporterService;
import services.export.PdfExporter;
import services.pagination.PageNumberPaginationSpec;
import services.pagination.SubmitTimeSequentialAccessPaginationSpec;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import services.statuses.StatusDefinitions;
import services.statuses.StatusNotFoundException;
import services.statuses.StatusService;
import views.ApplicantUtils;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramApplicationListView.RenderFilterParams;
import views.admin.programs.ProgramApplicationTableView;
import views.admin.programs.ProgramApplicationView;

/** Controller for admins viewing applications to programs. */
public final class AdminApplicationController extends CiviFormController {
  private static final int PAGE_SIZE = 10;
  private static final int PAGE_SIZE_BULK_STATUS = 10;

  private static final String REDIRECT_URI_KEY = "redirectUri";

  private final ApplicantService applicantService;
  private final PdfExporterService pdfExporterService;
  private final ProgramAdminApplicationService programAdminApplicationService;
  private final ProgramApplicationListView applicationListView;
  private final ProgramApplicationView applicationView;
  private final ProgramService programService;
  private final CsvExporterService exporterService;
  private final FormFactory formFactory;
  private final JsonExporterService jsonExporterService;
  private final Provider<LocalDateTime> nowProvider;
  private final MessagesApi messagesApi;
  private final DateConverter dateConverter;
  private final StatusService statusService;
  private final SettingsManifest settingsManifest;
  private final ProgramApplicationTableView tableView;

  public enum RelativeTimeOfDay {
    UNKNOWN,
    /** The start of the day, like 12:00:00 am */
    START,
    /** The end of the day, like 11:59:59 pm */
    END
  }

  @Inject
  public AdminApplicationController(
      ProgramService programService,
      ApplicantService applicantService,
      CsvExporterService csvExporterService,
      FormFactory formFactory,
      JsonExporterService jsonExporterService,
      PdfExporterService pdfExporterService,
      ProgramApplicationListView applicationListView,
      ProgramApplicationView applicationView,
      ProgramAdminApplicationService programAdminApplicationService,
      ProfileUtils profileUtils,
      MessagesApi messagesApi,
      DateConverter dateConverter,
      @Now Provider<LocalDateTime> nowProvider,
      VersionRepository versionRepository,
      StatusService statusService,
      SettingsManifest settingsManifest,
      ProgramApplicationTableView tableView) {
    super(profileUtils, versionRepository);
    this.programService = checkNotNull(programService);
    this.applicantService = checkNotNull(applicantService);
    this.applicationListView = checkNotNull(applicationListView);
    this.applicationView = checkNotNull(applicationView);
    this.programAdminApplicationService = checkNotNull(programAdminApplicationService);
    this.nowProvider = checkNotNull(nowProvider);
    this.exporterService = checkNotNull(csvExporterService);
    this.formFactory = checkNotNull(formFactory);
    this.jsonExporterService = checkNotNull(jsonExporterService);
    this.pdfExporterService = checkNotNull(pdfExporterService);
    this.messagesApi = checkNotNull(messagesApi);
    this.dateConverter = checkNotNull(dateConverter);
    this.statusService = checkNotNull(statusService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.tableView = checkNotNull(tableView);
  }

  /** Download a JSON file containing all applications to all versions of the specified program. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadAllJson(
      Http.Request request,
      long programId,
      Optional<String> search,
      Optional<String> fromDate,
      Optional<String> untilDate,
      Optional<String> applicationStatus,
      Optional<String> ignoreFilters)
      throws ProgramNotFoundException {
    final ProgramDefinition program;

    try {
      program = programService.getFullProgramDefinition(programId);
      checkProgramAdminAuthorization(request, program.adminName()).join();
    } catch (CompletionException | MissingOptionalException e) {
      return unauthorized();
    }

    boolean shouldApplyFilters = ignoreFilters.orElse("").isEmpty();
    SubmittedApplicationFilter filters = SubmittedApplicationFilter.EMPTY;
    if (shouldApplyFilters) {
      filters =
          SubmittedApplicationFilter.builder()
              .setSearchNameFragment(search)
              .setSubmitTimeFilter(
                  TimeFilter.builder()
                      .setFromTime(
                          parseDateTimeFromQuery(dateConverter, fromDate, RelativeTimeOfDay.START))
                      .setUntilTime(
                          parseDateTimeFromQuery(dateConverter, untilDate, RelativeTimeOfDay.END))
                      .build())
              .setApplicationStatus(applicationStatus)
              .build();
    }

    String filename = String.format("%s-%s.json", program.adminName(), nowProvider.get());
    String json =
        jsonExporterService.export(
            program,
            SubmitTimeSequentialAccessPaginationSpec.APPLICATION_MODEL_MAX_PAGE_SIZE_SPEC,
            filters,
            settingsManifest.getMultipleFileUploadEnabled(request));
    return ok(json)
        .as(Http.MimeTypes.JSON)
        .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
  }

  /** Download a CSV file containing all applications to all versions of the specified program. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadAll(
      Http.Request request,
      long programId,
      Optional<String> search,
      Optional<String> fromDate,
      Optional<String> untilDate,
      Optional<String> applicationStatus,
      Optional<String> ignoreFilters)
      throws ProgramNotFoundException {
    boolean shouldApplyFilters = ignoreFilters.orElse("").isEmpty();
    try {
      SubmittedApplicationFilter filters = SubmittedApplicationFilter.EMPTY;
      if (shouldApplyFilters) {
        filters =
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(search)
                .setSubmitTimeFilter(
                    TimeFilter.builder()
                        .setFromTime(
                            parseDateTimeFromQuery(
                                dateConverter, fromDate, RelativeTimeOfDay.START))
                        .setUntilTime(
                            parseDateTimeFromQuery(dateConverter, untilDate, RelativeTimeOfDay.END))
                        .build())
                .setApplicationStatus(applicationStatus)
                .build();
      }
      ProgramDefinition program = programService.getFullProgramDefinition(programId);
      checkProgramAdminAuthorization(request, program.adminName()).join();
      String filename = String.format("%s-%s.csv", program.adminName(), nowProvider.get());
      String csv =
          exporterService.getProgramAllVersionsCsv(
              programId, filters, settingsManifest.getMultipleFileUploadEnabled(request));
      return ok(csv)
          .as(Http.MimeTypes.BINARY)
          .withHeader(
              "Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
    } catch (CompletionException | MissingOptionalException e) {
      return unauthorized();
    }
  }

  /**
   * Parses a date from a raw query string (e.g. 2022-01-02) and returns an instant representing
   * that date in the UTC time zone.
   */
  private Optional<Instant> parseDateTimeFromQuery(
      DateConverter dateConverter,
      Optional<String> maybeQueryParam,
      RelativeTimeOfDay relativeTimeOfDay) {
    return maybeQueryParam
        .filter(s -> !s.isBlank())
        .map(
            s -> {
              try {
                switch (relativeTimeOfDay) {
                  case START:
                    return dateConverter.parseIso8601DateToStartOfLocalDateInstant(s);
                  case END:
                    return dateConverter.parseIso8601DateToEndOfLocalDateInstant(s);
                  default:
                    return dateConverter.parseIso8601DateToStartOfLocalDateInstant(s);
                }
              } catch (DateTimeParseException e) {
                throw new BadRequestException("Malformed query param");
              }
            });
  }

  /**
   * Download a CSV file containing demographics information of the current live version.
   * Demographics information is collected from answers to a collection of questions specially
   * marked by CiviForm admins.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result downloadDemographics(
      Http.Request request, Optional<String> fromDate, Optional<String> untilDate) {
    TimeFilter submitTimeFilter =
        TimeFilter.builder()
            .setFromTime(parseDateTimeFromQuery(dateConverter, fromDate, RelativeTimeOfDay.START))
            .setUntilTime(parseDateTimeFromQuery(dateConverter, untilDate, RelativeTimeOfDay.END))
            .build();
    String filename = String.format("demographics-%s.csv", nowProvider.get());
    String csv = exporterService.getDemographicsCsv(submitTimeFilter);
    return ok(csv)
        .as(Http.MimeTypes.BINARY)
        .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
  }

  /** Download a PDF file of the application to the program. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result download(Http.Request request, long programId, long applicationId)
      throws ProgramNotFoundException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    try {
      checkProgramAdminAuthorization(request, program.adminName()).join();
    } catch (CompletionException | MissingOptionalException e) {
      return unauthorized();
    }
    Optional<ApplicationModel> applicationMaybe =
        programAdminApplicationService.getApplication(applicationId, program);
    if (applicationMaybe.isEmpty()) {
      return badRequest(String.format("Application %d does not exist.", applicationId));
    }
    ApplicationModel application = applicationMaybe.get();
    PdfExporter.InMemoryPdf pdf =
        pdfExporterService.generateApplicationPdf(application, /* isAdmin= */ true);
    return ok(pdf.getByteArray())
        .as("application/pdf")
        .withHeader(
            "Content-Disposition", String.format("attachment; filename=\"%s\"", pdf.getFileName()));
  }

  /** Return a HTML page displaying the summary of the specified application. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result show(Http.Request request, long programId, long applicationId)
      throws ProgramNotFoundException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    String programName = program.adminName();

    try {
      checkProgramAdminAuthorization(request, programName).join();
    } catch (CompletionException | MissingOptionalException e) {
      return unauthorized();
    }

    Optional<ApplicationModel> applicationMaybe =
        programAdminApplicationService.getApplication(applicationId, program);
    if (applicationMaybe.isEmpty()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }
    ApplicationModel application = applicationMaybe.get();

    Messages messages = messagesApi.preferred(request);
    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)",
            ApplicantUtils.getApplicantName(
                application.getApplicantData().getApplicantDisplayName(), messages),
            application.id);

    ReadOnlyApplicantProgramService roApplicantService =
        applicantService
            .getReadOnlyApplicantProgramService(application)
            .toCompletableFuture()
            .join();
    ImmutableList<Block> blocks = roApplicantService.getAllActiveBlocks();
    ImmutableList<AnswerData> answers = roApplicantService.getSummaryDataOnlyActive();
    Optional<String> noteMaybe = programAdminApplicationService.getNote(application);

    return ok(
        applicationView.render(
            programId,
            programName,
            application,
            applicantNameWithApplicationId,
            blocks,
            answers,
            statusService.lookupActiveStatusDefinitions(programName),
            noteMaybe,
            program.hasEligibilityEnabled(),
            request));
  }

  /**
   * Updates the status for the associated application and redirects to the summary page for the
   * application.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result updateStatus(Http.Request request, long programId, long applicationId)
      throws ProgramNotFoundException,
          StatusEmailNotFoundException,
          StatusNotFoundException,
          AccountHasNoEmailException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    String programName = program.adminName();

    try {
      checkProgramAdminAuthorization(request, programName).join();
    } catch (CompletionException | MissingOptionalException e) {
      return unauthorized();
    }

    Optional<ApplicationModel> applicationMaybe =
        programAdminApplicationService.getApplication(applicationId, program);
    if (applicationMaybe.isEmpty()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }
    ApplicationModel application = applicationMaybe.get();

    Map<String, String> formData = formFactory.form().bindFromRequest(request).rawData();
    Optional<String> maybeCurrentStatus = Optional.ofNullable(formData.get(CURRENT_STATUS));
    Optional<String> maybeNewStatus = Optional.ofNullable(formData.get(NEW_STATUS));
    Optional<String> maybeSendEmail = Optional.ofNullable(formData.get(SEND_EMAIL));
    Optional<String> maybeRedirectUri = Optional.ofNullable(formData.get(REDIRECT_URI_KEY));
    if (maybeCurrentStatus.isEmpty()) {
      return badRequest(String.format("The %s field is not present", CURRENT_STATUS));
    }
    if (maybeNewStatus.isEmpty()) {
      return badRequest(String.format("The %s field is not present", NEW_STATUS));
    }
    if (maybeSendEmail.isEmpty()) {
      return badRequest(String.format("The %s field is not present", SEND_EMAIL));
    }
    if (maybeRedirectUri.isEmpty()) {
      return badRequest(String.format("The %s field is not present", REDIRECT_URI_KEY));
    }
    // Verify the UI is changing from the actual current status to detect an out of date UI.
    if (application.getLatestStatus().isPresent()) {
      if (!application.getLatestStatus().get().equals(maybeCurrentStatus.get())) {
        // Only allow relative URLs to ensure that we redirect to the same domain.
        String redirectUrl = UrlUtils.checkIsRelativeUrl(maybeRedirectUri.orElse(""));
        return redirect(redirectUrl)
            .flashing(
                "error",
                "The application state has changed since the page was loaded. Please reload and"
                    + " try again.");
      }
    } else if (!maybeCurrentStatus.get().isBlank()) {
      return badRequest(
          String.format("The %s field should be empty as there is no status set", CURRENT_STATUS));
    }
    // Save the new data.
    String newStatus = maybeNewStatus.get();
    final boolean sendEmail;
    if (maybeSendEmail.get().isBlank()) {
      sendEmail = false;
    } else if (maybeSendEmail.get().equals("on")) {
      sendEmail = true;
    } else {
      return badRequest(String.format("%s value is invalid: %s", SEND_EMAIL, maybeSendEmail.get()));
    }

    programAdminApplicationService.setStatus(
        application,
        ApplicationEventDetails.StatusEvent.builder()
            .setStatusText(newStatus)
            .setEmailSent(sendEmail)
            .build(),
        profileUtils.currentUserProfile(request).getAccount().join());
    // Only allow relative URLs to ensure that we redirect to the same domain.
    String redirectUrl = UrlUtils.checkIsRelativeUrl(maybeRedirectUri.orElse(""));
    return redirect(redirectUrl).flashing(FlashKey.SUCCESS, "Application status updated");
  }

  /**
   * Edits the note for the associated application and redirects to the summary page for the
   * application.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result updateNote(Http.Request request, long programId, long applicationId)
      throws ProgramNotFoundException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    String programName = program.adminName();

    try {
      checkProgramAdminAuthorization(request, programName).join();
    } catch (CompletionException | MissingOptionalException e) {
      return unauthorized();
    }

    Optional<ApplicationModel> applicationMaybe =
        programAdminApplicationService.getApplication(applicationId, program);
    if (applicationMaybe.isEmpty()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }
    ApplicationModel application = applicationMaybe.get();

    Map<String, String> formData = formFactory.form().bindFromRequest(request).rawData();
    Optional<String> maybeNote = Optional.ofNullable(formData.get(NOTE));
    Optional<String> maybeRedirectUri = Optional.ofNullable(formData.get(REDIRECT_URI_KEY));
    if (maybeNote.isEmpty()) {
      return badRequest("A note is not present.");
    }
    String note = maybeNote.get();
    if (maybeRedirectUri.isEmpty()) {
      return badRequest("A redirect URI is not present");
    }

    programAdminApplicationService.setNote(
        application,
        ApplicationEventDetails.NoteEvent.create(note),
        profileUtils.currentUserProfile(request).getAccount().join());

    // Only allow relative URLs to ensure that we redirect to the same domain.
    String redirectUrl = UrlUtils.checkIsRelativeUrl(maybeRedirectUri.orElse(""));
    return redirect(redirectUrl).flashing(FlashKey.SUCCESS, "Application note updated");
  }

  /** Return a paginated HTML page displaying (part of) all applications to the program. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index(
      Http.Request request,
      long programId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<String> fromDate,
      Optional<String> untilDate,
      Optional<String> applicationStatus,
      Optional<String> selectedApplicationUri,
      Optional<Boolean> showDownloadModal,
      Optional<String> message)
      throws ProgramNotFoundException {
    if (page.isEmpty()) {
      return redirect(
          routes.AdminApplicationController.index(
              programId,
              search,
              Optional.of(1),
              fromDate,
              untilDate,
              applicationStatus,
              selectedApplicationUri,
              showDownloadModal,
              message));
    }

    SubmittedApplicationFilter filters =
        SubmittedApplicationFilter.builder()
            .setSearchNameFragment(search)
            .setSubmitTimeFilter(
                TimeFilter.builder()
                    .setFromTime(
                        parseDateTimeFromQuery(dateConverter, fromDate, RelativeTimeOfDay.START))
                    .setUntilTime(
                        parseDateTimeFromQuery(dateConverter, untilDate, RelativeTimeOfDay.END))
                    .build())
            .setApplicationStatus(applicationStatus)
            .build();

    final ProgramDefinition program;
    try {
      program = programService.getFullProgramDefinition(programId);
      checkProgramAdminAuthorization(request, program.adminName()).join();
    } catch (CompletionException | MissingOptionalException e) {
      return unauthorized();
    }

    StatusDefinitions activeStatusDefinitions =
        statusService.lookupActiveStatusDefinitions(program.adminName());

    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    if (settingsManifest.getBulkStatusUpdateEnabled(request)) {
      var paginationSpec =
          new PageNumberPaginationSpec(
              PAGE_SIZE_BULK_STATUS,
              page.orElse(1),
              PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME);
      PaginationResult<ApplicationModel> applications =
          programService.getSubmittedProgramApplicationsAllVersions(
              programId, paginationSpec, filters);
      return ok(
          tableView.render(
              request,
              profile,
              program,
              activeStatusDefinitions,
              getAllApplicationStatusesForProgram(program.id()),
              paginationSpec,
              applications,
              RenderFilterParams.builder()
                  .setSearch(search)
                  .setFromDate(fromDate)
                  .setUntilDate(untilDate)
                  .setSelectedApplicationStatus(applicationStatus)
                  .build(),
              showDownloadModal,
              message));
    }
    var paginationSpec =
        new PageNumberPaginationSpec(
            PAGE_SIZE, page.orElse(1), PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME);
    PaginationResult<ApplicationModel> applications =
        programService.getSubmittedProgramApplicationsAllVersions(
            programId, paginationSpec, filters);
    return ok(
        applicationListView.render(
            request,
            profile,
            program,
            activeStatusDefinitions.getDefaultStatus(),
            getAllApplicationStatusesForProgram(program.id()),
            paginationSpec,
            applications,
            RenderFilterParams.builder()
                .setSearch(search)
                .setFromDate(fromDate)
                .setUntilDate(untilDate)
                .setSelectedApplicationStatus(applicationStatus)
                .build(),
            selectedApplicationUri,
            showDownloadModal));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result updateStatuses(Http.Request request, long programId)
      throws ProgramNotFoundException, StatusNotFoundException, StatusEmailNotFoundException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    String programName = program.adminName();
    try {
      checkProgramAdminAuthorization(request, programName).join();
    } catch (CompletionException | NoSuchElementException e) {
      return unauthorized();
    }
    Form<BulkStatusUpdateForm> form =
        formFactory.form(BulkStatusUpdateForm.class).bindFromRequest(request);
    var ids = form.get().getApplicationsIds();

    var applicationlist =
        programAdminApplicationService.getApplications(
            ids.stream().map(e -> Long.parseLong(e)).collect(ImmutableList.toImmutableList()),
            program);
    boolean sendEmail = form.get().isMaybeSendEmail();
    programAdminApplicationService.setStatus(
        applicationlist,
        ApplicationEventDetails.StatusEvent.builder()
            .setStatusText(form.get().getStatusText())
            .setEmailSent(sendEmail)
            .build(),
        profileUtils.currentUserProfile(request).getAccount().join());

    if (sendEmail) {
      return redirect(
          routes.AdminApplicationController.index(
                  programId,
                  /* search= */ Optional.empty(),
                  /* page= */ Optional.empty(),
                  /* fromDate= */ Optional.empty(),
                  /* untilDate= */ Optional.empty(),
                  /* applicationStatus= */ Optional.empty(),
                  Optional.empty(),
                  /* showDownloadModal= */ Optional.empty(),
                  /* message= */ Optional.of(
                      "Status updates sent to applicants with contact information on file"))
              .url());
    }
    return redirect(
        routes.AdminApplicationController.index(
                programId,
                /* search= */ Optional.empty(),
                /* page= */ Optional.empty(),
                /* fromDate= */ Optional.empty(),
                /* untilDate= */ Optional.empty(),
                /* applicationStatus= */ Optional.empty(),
                Optional.empty(),
                /* showDownloadModal= */ Optional.empty(),
                /* message= */ Optional.of("Status update success"))
            .url());
  }

  private ImmutableList<String> getAllApplicationStatusesForProgram(long programId)
      throws ProgramNotFoundException {
    return statusService.getAllPossibleStatusDefinitions(programId).stream()
        .map(stdef -> stdef.getStatuses())
        .flatMap(ImmutableList::stream)
        .map(StatusDefinitions.Status::statusText)
        .distinct()
        .sorted()
        .collect(ImmutableList.toImmutableList());
  }
}
