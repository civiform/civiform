package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.CiviFormController;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.ErrorAnd;
import services.migration.ProgramMigrationService;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import views.admin.migration.AdminExportView;
import views.admin.migration.AdminProgramExportForm;

/**
 * A controller for the export part of program migration (allowing admins to easily migrate programs
 * between different environments). This controller is responsible for exporting a program into a
 * JSON format. The JSON can then be imported to a different environment to re-create the program
 * there. {@link AdminImportController} is responsible for reading the program JSON and turning it
 * into a program definition with blocks and questions.
 *
 * <p>Typically, admins will export from one environment (e.g. staging) and import to another
 * environment (e.g. production).
 */
public class AdminExportController extends CiviFormController {
  private final AdminExportView adminExportView;
  private final FormFactory formFactory;
  private final ProgramMigrationService programMigrationService;
  private final ProgramService programService;
  private final QuestionService questionService;

  @Inject
  public AdminExportController(
      AdminExportView adminExportView,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ProgramMigrationService programMigrationService,
      ProgramService programService,
      QuestionService questionService,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminExportView = checkNotNull(adminExportView);
    this.formFactory = checkNotNull(formFactory);
    this.programMigrationService = checkNotNull(programMigrationService);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, Long programId) {
    ProgramDefinition program;
    try {
      program = programService.getFullProgramDefinition(programId);
    } catch (ProgramNotFoundException e) {
      return badRequest(String.format("Program with ID %s could not be found", programId));
    }

    ImmutableList<QuestionDefinition> questionsUsedByProgram =
        program.getQuestionIdsInProgram().stream()
            .map(
                questionId -> {
                  try {
                    return questionService
                        .getReadOnlyQuestionServiceSync()
                        .getQuestionDefinition(questionId);
                  } catch (QuestionNotFoundException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(ImmutableList.toImmutableList());

    ErrorAnd<String, String> serializeResult =
        programMigrationService.serialize(
            programMigrationService.prepForExport(program), questionsUsedByProgram);

    if (serializeResult.isError()) {
      return badRequest(serializeResult.getErrors().stream().findFirst().orElseThrow());
    }

    return ok(
        adminExportView.render(request, program, serializeResult.getResult(), program.adminName()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result downloadJson(Http.Request request, String adminName) {

    Form<AdminProgramExportForm> form =
        formFactory
            .form(AdminProgramExportForm.class)
            .bindFromRequest(request, AdminProgramExportForm.FIELD_NAMES.toArray(new String[0]));

    String json = form.get().getProgramJson();
    String filename = adminName + "-exported.json";

    return ok(json)
        .as(Http.MimeTypes.JSON)
        .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
  }
}
