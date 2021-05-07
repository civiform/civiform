package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.time.Clock;
import java.util.Optional;
import javax.inject.Inject;
import models.Application;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApplicationRepository;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.Block;
import services.applicant.ReadOnlyApplicantProgramService;
import services.export.ExporterService;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramApplicationView;

/** Controller for admins viewing responses to programs. */
public class AdminApplicationController extends CiviFormController {

  private final ProgramService programService;
  private final ApplicantService applicantService;
  private final ApplicationRepository applicationRepository;
  private final ProgramApplicationListView applicationListView;
  private final ProgramApplicationView applicationView;
  private final ExporterService exporterService;
  private final ProfileUtils profileUtils;
  private final Clock clock;

  @Inject
  public AdminApplicationController(
      ProgramService programService,
      ApplicantService applicantService,
      ExporterService exporterService,
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
    this.clock = clock;
    this.exporterService = checkNotNull(exporterService);
  }

  @Secure(authorizers = Authorizers.Labels.PROGRAM_ADMIN)
  public Result downloadAll(Http.Request request, long programId) {
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
    }
  }

  @Secure(authorizers = Authorizers.Labels.PROGRAM_ADMIN)
  public Result download(Http.Request request, long programId, long applicationId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
      throw new UnsupportedOperationException("Not yet implemented.");
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.PROGRAM_ADMIN)
  public Result show(Http.Request request, long programId, long applicationId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
    Optional<Application> applicationMaybe =
        this.applicationRepository.getApplication(applicationId).toCompletableFuture().join();
    if (!applicationMaybe.isPresent()) {
      return notFound(String.format("Application %d does not exist.", applicationId));
    }
    Application application = applicationMaybe.get();

    ReadOnlyApplicantProgramService roApplicantService =
        applicantService
            .getReadOnlyApplicantProgramService(application)
            .toCompletableFuture()
            .join();
    ImmutableList<Block> blocks = roApplicantService.getAllBlocks();
    ImmutableList<AnswerData> answers = roApplicantService.getSummaryData();
    return ok(
        applicationView.render(
            programId,
            applicationId,
            application.getApplicantData().getApplicantName(),
            blocks,
            answers));
  }

  @Secure(authorizers = Authorizers.Labels.PROGRAM_ADMIN)
  public Result index(Http.Request request, long programId) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
    try {
      ImmutableList<Application> applications = programService.getProgramApplications(programId);
      return ok(applicationListView.render(programId, applications));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
  }
}
