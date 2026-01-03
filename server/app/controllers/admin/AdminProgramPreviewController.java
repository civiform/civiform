package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import controllers.applicant.ProgramSlugHandler;
import java.util.concurrent.CompletionStage;
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
import services.settings.SettingsManifest;

/** Controller for admins previewing a program as an applicant. */
public final class AdminProgramPreviewController extends CiviFormController {
  private final PdfExporterService pdfExporterService;
  private final ProgramService programService;
  private final QuestionService questionService;
  private final ProgramSlugHandler programSlugHandler;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminProgramPreviewController(
      PdfExporterService pdfExporterService,
      ProfileUtils profileUtils,
      ProgramService programService,
      QuestionService questionService,
      VersionRepository versionRepository,
      ProgramSlugHandler programSlugHandler,
      SettingsManifest settingsManifest) {
    super(profileUtils, versionRepository);
    this.pdfExporterService = checkNotNull(pdfExporterService);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.programSlugHandler = checkNotNull(programSlugHandler);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /**
   * Retrieves the admin's user profile and redirects to the application review page where the admin
   * can preview the program.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> preview(Request request, String programSlug) {
    return programSlugHandler.showProgramPreview(this, request, programSlug);
  }

  /**
   * Creates and downloads a PDF for the given program. The PDF will contain all the blocks and the
   * questions in each block.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> pdfPreview(Request request, long programId)
      throws ProgramNotFoundException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            roQuestionService -> {
              PdfExporter.InMemoryPdf pdf =
                  pdfExporterService.generateProgramPreviewPdf(
                      program,
                      roQuestionService.getAllQuestions(),
                      settingsManifest.getExpandedFormLogicEnabled());
              return ok(pdf.getByteArray())
                  .as("application/pdf")
                  .withHeader(
                      "Content-Disposition",
                      String.format("attachment; filename=\"%s\"", pdf.getFileName()));
            });
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
