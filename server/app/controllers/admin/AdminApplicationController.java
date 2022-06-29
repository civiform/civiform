package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import annotations.BindingAnnotations.Now;
import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import com.itextpdf.text.DocumentException;
import controllers.CiviFormController;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApplicationRepository;
import repository.TimeFilter;
import services.DateConverter;
import services.IdentifierBasedPaginationSpec;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.export.ExporterService;
import services.export.JsonExporter;
import services.export.PdfExporter;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.ApplicantUtils;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramApplicationListView.RenderFilterParams;
import views.admin.programs.ProgramApplicationView;

/** Controller for admins viewing applications to programs. */
public class AdminApplicationController extends CiviFormController {

  private final ProgramService programService;
  private final ApplicantService applicantService;
  private final ApplicationRepository applicationRepository;
  private final ProgramApplicationListView applicationListView;
  private final ProgramApplicationView applicationView;
  private final ExporterService exporterService;
  private final JsonExporter jsonExporter;
  private final PdfExporter pdfExporter;
  private final ProfileUtils profileUtils;
  private final Provider<LocalDateTime> nowProvider;
  private final MessagesApi messagesApi;
  private final DateConverter dateConverter;
  private static final int PAGE_SIZE = 10;

  @Inject
  public AdminApplicationController(
      ProgramService programService,
      ApplicantService applicantService,
      ExporterService exporterService,
      JsonExporter jsonExporter,
      PdfExporter pdfExporter,
      ProgramApplicationListView applicationListView,
      ProgramApplicationView applicationView,
      ApplicationRepository applicationRepository,
      ProfileUtils profileUtils,
      MessagesApi messagesApi,
      DateConverter dateConverter,
      @Now Provider<LocalDateTime> nowProvider) {
    this.programService = checkNotNull(programService);
    this.applicantService = checkNotNull(applicantService);
    this.applicationListView = checkNotNull(applicationListView);
    this.profileUtils = checkNotNull(profileUtils);
    this.applicationView = checkNotNull(applicationView);
    this.applicationRepository = checkNotNull(applicationRepository);
    this.nowProvider = checkNotNull(nowProvider);
    this.exporterService = checkNotNull(exporterService);
    this.jsonExporter = checkNotNull(jsonExporter);
    this.pdfExporter = checkNotNull(pdfExporter);
    this.messagesApi = checkNotNull(messagesApi);
    this.dateConverter = checkNotNull(dateConverter);
  }

  /** Download a JSON file containing all applications to all versions of the specified program. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadAllJson(
      Http.Request request,
      long programId,
      Optional<String> search,
      Optional<String> fromDate,
      Optional<String> untilDate,
      Optional<String> ignoreFilters)
      throws ProgramNotFoundException {
    final ProgramDefinition program;

    try {
      program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (CompletionException e) {
      return unauthorized();
    }

    boolean shouldApplyFilters = ignoreFilters.orElse("").isEmpty();
    TimeFilter submitTimeFilter =
        shouldApplyFilters
            ? TimeFilter.builder()
                .setFromTime(parseDateFromQuery(dateConverter, fromDate))
                .setUntilTime(parseDateFromQuery(dateConverter, untilDate))
                .build()
            : TimeFilter.EMPTY;
    Optional<String> searchFragment = shouldApplyFilters ? search : Optional.empty();

    String filename = String.format("%s-%s.json", program.adminName(), nowProvider.get());
    String json =
        jsonExporter
            .export(
                program,
                IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG,
                searchFragment,
                submitTimeFilter)
            .getLeft();

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
      Optional<String> ignoreFilters)
      throws ProgramNotFoundException {
    boolean shouldApplyFilters = ignoreFilters.orElse("").isEmpty();
    try {
      TimeFilter submitTimeFilter =
          shouldApplyFilters
              ? TimeFilter.builder()
                  .setFromTime(parseDateFromQuery(dateConverter, fromDate))
                  .setUntilTime(parseDateFromQuery(dateConverter, untilDate))
                  .build()
              : TimeFilter.EMPTY;
      Optional<String> searchFragment = shouldApplyFilters ? search : Optional.empty();
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
      String filename = String.format("%s-%s.csv", program.adminName(), nowProvider.get());
      String csv =
          exporterService.getProgramAllVersionsCsv(programId, searchFragment, submitTimeFilter);
      return ok(csv)
          .as(Http.MimeTypes.BINARY)
          .withHeader(
              "Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
    } catch (CompletionException e) {
      return unauthorized();
    }
  }

  /**
   * Download a CSV file containing all applications to the specified program version. This was the
   * original behavior for the program admin CSV download but is currently unused as of 10/13/2021.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadSingleVersion(Http.Request request, long programId)
      throws ProgramNotFoundException {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
      String filename = String.format("%s-%s.csv", program.adminName(), nowProvider.get());
      String csv = exporterService.getProgramCsv(programId);
      return ok(csv)
          .as(Http.MimeTypes.BINARY)
          .withHeader(
              "Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
    } catch (CompletionException e) {
      return unauthorized();
    }
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
            .setFromTime(parseDateFromQuery(dateConverter, fromDate))
            .setUntilTime(parseDateFromQuery(dateConverter, untilDate))
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
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (CompletionException e) {
      return unauthorized();
    }

    Optional<Application> applicationMaybe =
        this.applicationRepository.getApplication(applicationId).toCompletableFuture().join();

    if (!applicationMaybe.isPresent()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }
    Application application = applicationMaybe.get();
    PdfExporter.InMemoryPdf pdf;
    try {
      pdf = pdfExporter.export(application);
    } catch (DocumentException | IOException e) {
      throw new RuntimeException(e);
    }
    return ok(pdf.getByteArray())
        .as("application/pdf")
        .withHeader(
            "Content-Disposition", String.format("attachment; filename=\"%s\"", pdf.getFileName()));
  }

  /** Return a HTML page displaying the summary of the specified application. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result show(Http.Request request, long programId, long applicationId)
      throws ProgramNotFoundException {
    String programName;

    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      programName = program.adminName();
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (CompletionException e) {
      return unauthorized();
    }

    Optional<Application> applicationMaybe =
        this.applicationRepository.getApplication(applicationId).toCompletableFuture().join();

    if (!applicationMaybe.isPresent()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }

    Application application = applicationMaybe.get();
    Messages messages = messagesApi.preferred(request);
    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)",
            ApplicantUtils.getApplicantName(
                application.getApplicantData().getApplicantName(), messages),
            application.id);

    ReadOnlyApplicantProgramService roApplicantService =
        applicantService
            .getReadOnlyApplicantProgramService(application)
            .toCompletableFuture()
            .join();
    ImmutableList<Block> blocks = roApplicantService.getAllActiveBlocks();
    ImmutableList<AnswerData> answers = roApplicantService.getSummaryData();

    return ok(
        applicationView.render(
            programId,
            programName,
            applicationId,
            applicantNameWithApplicationId,
            blocks,
            answers));
  }

  /** Return a paginated HTML page displaying (part of) all applications to the program. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index(
      Http.Request request,
      long programId,
      Optional<String> search,
      Optional<Integer> page,
      Optional<String> fromDate,
      Optional<String> untilDate)
      throws ProgramNotFoundException {
    if (page.isEmpty()) {
      return redirect(
          routes.AdminApplicationController.index(
              programId, search, Optional.of(1), fromDate, untilDate));
    }

    TimeFilter submitTimeFilter =
        TimeFilter.builder()
            .setFromTime(parseDateFromQuery(dateConverter, fromDate))
            .setUntilTime(parseDateFromQuery(dateConverter, untilDate))
            .build();

    final ProgramDefinition program;
    try {
      program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (CompletionException e) {
      return unauthorized();
    }

    var paginationSpec = new PageNumberBasedPaginationSpec(PAGE_SIZE, page.orElse(1));
    PaginationResult<Application> applications =
        programService.getSubmittedProgramApplicationsAllVersions(
            programId, F.Either.Right(paginationSpec), search, submitTimeFilter);

    return ok(
        applicationListView.render(
            request,
            program,
            paginationSpec,
            applications,
            RenderFilterParams.builder()
                .setSearch(search)
                .setFromDate(fromDate)
                .setUntilDate(untilDate)
                .build()));
  }
}
