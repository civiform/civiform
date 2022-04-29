package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApplicationRepository;
import services.PaginationResult;
import services.PaginationSpec;
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
import views.admin.programs.ProgramApplicationListView;
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
  private final Clock clock;
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
      Clock clock) {
    this.programService = checkNotNull(programService);
    this.applicantService = checkNotNull(applicantService);
    this.applicationListView = checkNotNull(applicationListView);
    this.profileUtils = checkNotNull(profileUtils);
    this.applicationView = checkNotNull(applicationView);
    this.applicationRepository = checkNotNull(applicationRepository);
    this.clock = checkNotNull(clock);
    this.exporterService = checkNotNull(exporterService);
    this.jsonExporter = checkNotNull(jsonExporter);
    this.pdfExporter = checkNotNull(pdfExporter);
  }

  /** Download a JSON file containing all applications to all versions of the specified program. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadAllJson(Http.Request request, long programId) {
    final ProgramDefinition program;

    try {
      program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (CompletionException e) {
      return unauthorized();
    }

    String filename = String.format("%s-%s.json", program.adminName(), clock.instant().toString());
    String json = jsonExporter.export(program);

    return ok(json)
        .as(Http.MimeTypes.JSON)
        .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
  }

  /** Download a CSV file containing all applications to all versions of the specified program. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadAll(Http.Request request, long programId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
      String filename = String.format("%s-%s.csv", program.adminName(), clock.instant().toString());
      String csv = exporterService.getProgramAllVersionsCsv(programId);
      return ok(csv)
          .as(Http.MimeTypes.BINARY)
          .withHeader(
              "Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (CompletionException e) {
      return unauthorized();
    }
  }

  /**
   * Download a CSV file containing all applications to the specified program version. This was the
   * original behavior for the program admin CSV download but is currently unused as of 10/13/2021.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadSingleVersion(Http.Request request, long programId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
      String filename = String.format("%s-%s.csv", program.adminName(), clock.instant().toString());
      String csv = exporterService.getProgramCsv(programId);
      return ok(csv)
          .as(Http.MimeTypes.BINARY)
          .withHeader(
              "Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
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
  public Result downloadDemographics() {
    String filename = String.format("demographics-%s.csv", clock.instant().toString());
    String csv = exporterService.getDemographicsCsv();
    return ok(csv)
        .as(Http.MimeTypes.BINARY)
        .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
  }

  /** Download a PDF file of the application to the program. This feature is not implemented yet. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result download(Http.Request request, long programId, long applicationId) {
    ProgramDefinition program;
    try {
      program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (CompletionException e) {
      return unauthorized();
    }

    Optional<Application> applicationMaybe =
            this.applicationRepository.getApplication(applicationId).toCompletableFuture().join();

    if (!applicationMaybe.isPresent()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }

    Application application = applicationMaybe.get();
    String applicantNameWithApplicationId =
            String.format("%s (%d)", application.getApplicantData().getApplicantName(), application.id);

    ReadOnlyApplicantProgramService roApplicantService =
            applicantService
                    .getReadOnlyApplicantProgramService(application)
                    .toCompletableFuture()
                    .join();
    ImmutableList<AnswerData> answers = roApplicantService.getSummaryData();

    String filename = String.format("%s-%s.pdf", applicantNameWithApplicationId, clock.instant().toString());
    byte[] pdf = null;
    try{
      pdf = pdfExporter.export(answers,applicantNameWithApplicationId,program.adminName());
    }
    catch(Exception e)
    {
      return  notFound(e.toString());
    }
    return ok(pdf)
            .as("application/pdf")
            .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
  }



  /** Return a HTML page displaying the summary of the specified application. */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result show(Http.Request request, long programId, long applicationId) {
    String programName;

    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      programName = program.adminName();
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (CompletionException e) {
      return unauthorized();
    }

    Optional<Application> applicationMaybe =
        this.applicationRepository.getApplication(applicationId).toCompletableFuture().join();

    if (!applicationMaybe.isPresent()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }

    Application application = applicationMaybe.get();
    String applicantNameWithApplicationId =
        String.format("%s (%d)", application.getApplicantData().getApplicantName(), application.id);

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
      Http.Request request, long programId, Optional<String> search, Optional<Integer> page) {
    if (page.isEmpty()) {
      return redirect(routes.AdminApplicationController.index(programId, search, Optional.of(1)));
    }

    final ProgramDefinition program;
    try {
      program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (CompletionException e) {
      return unauthorized();
    }

    try {
      PaginationResult<Application> applications =
          programService.getSubmittedProgramApplicationsAllVersions(
              programId, new PaginationSpec(PAGE_SIZE, page.orElse(1)), search);

      return ok(applicationListView.render(request, program, applications, search));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
  }
}
