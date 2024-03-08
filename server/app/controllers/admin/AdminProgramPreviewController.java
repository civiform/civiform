package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.CiviFormController;
import controllers.applicant.ApplicantRoutes;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Call;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.applications.PdfExporterService;
import services.export.PdfExporter;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;

/** Controller for admins previewing a program as an applicant. */
public final class AdminProgramPreviewController extends CiviFormController {
  private final ApplicantRoutes applicantRoutes;
  private final PdfExporterService pdfExporterService;
  private final ProgramService programService;
  private final QuestionService questionService;

  @Inject
  public AdminProgramPreviewController(
      PdfExporterService pdfExporterService,
      ProfileUtils profileUtils,
      ProgramService programService,
      QuestionService questionService,
      VersionRepository versionRepository,
      ApplicantRoutes applicantRoutes) {
    super(profileUtils, versionRepository);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.pdfExporterService = checkNotNull(pdfExporterService);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
  }

  /**
   * Retrieves the admin's user profile and redirects to the application review page where the admin
   * can preview the program.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result preview(Request request, long programId) {
    CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);

    try {
      return redirect(applicantRoutes.review(profile, profile.getApplicant().get().id, programId));
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result pdfPreview(Request request, long programId) throws ProgramNotFoundException {
    // Copied from AdminApplicationController
    ProgramDefinition program = programService.getFullProgramDefinition(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();
    PdfExporter.InMemoryPdf pdf =
        pdfExporterService.generateProgramPreview(program, roQuestionService.getAllQuestions());
    return ok(pdf.getByteArray())
        .as("application/pdf")
        .withHeader(
            "Content-Disposition", String.format("attachment; filename=\"%s\"", pdf.getFileName()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> back(Request request, long programId) {
    return versionRepository
        .isDraftProgramAsync(programId)
        .thenApplyAsync(
            (isDraftProgram) -> {
              Call reviewPage =
                  controllers.admin.routes.AdminProgramBlocksController.readOnlyIndex(programId);
              if (isDraftProgram) {
                reviewPage = controllers.admin.routes.AdminProgramBlocksController.index(programId);
              }
              return redirect(reviewPage);
            });
  }
}
