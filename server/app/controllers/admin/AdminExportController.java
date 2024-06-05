package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.util.Locale;
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
import services.settings.SettingsManifest;
import views.admin.migration.AdminExportView;
import views.admin.migration.AdminExportViewPartial;
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
  private final AdminExportViewPartial adminExportViewPartial;
  private final FormFactory formFactory;
  private final ProgramMigrationService programMigrationService;
  private final ProgramService programService;
  private final QuestionService questionService;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminExportController(
      AdminExportView adminExportView,
      AdminExportViewPartial adminExportViewPartial,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ProgramMigrationService programMigrationService,
      ProgramService programService,
      QuestionService questionService,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminExportView = checkNotNull(adminExportView);
    this.adminExportViewPartial = checkNotNull(adminExportViewPartial);
    this.formFactory = checkNotNull(formFactory);
    this.programMigrationService = checkNotNull(programMigrationService);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }

    // Show the most recent version of programs (eg. draft version if there is one) sorted
    // alphabetically by display name
    ImmutableList<ProgramDefinition> sortedPrograms =
        programService.getActiveAndDraftPrograms().getMostRecentProgramDefinitions().stream()
            .sorted(
                (p1, p2) ->
                    p1.localizedName()
                        .getOrDefault(Locale.getDefault())
                        .compareTo(p2.localizedName().getOrDefault(Locale.getDefault())))
            .collect(ImmutableList.toImmutableList());

    return ok(adminExportView.render(request, sortedPrograms));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxExportProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }

    Form<AdminProgramExportForm> form =
        formFactory
            .form(AdminProgramExportForm.class)
            .bindFromRequest(request, AdminProgramExportForm.FIELD_NAMES.toArray(new String[0]));

    Long programId = form.get().getProgramId();

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
        programMigrationService.serialize(program, questionsUsedByProgram);

    if (serializeResult.isError()) {
      return badRequest(serializeResult.getErrors().stream().findFirst().orElseThrow());
    }

    return ok(
        adminExportViewPartial
            .renderJSONPreview(request, serializeResult.getResult(), program.adminName())
            .render());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result downloadJSON(Http.Request request, String adminName) {

    Form<AdminProgramExportForm> form =
        formFactory
            .form(AdminProgramExportForm.class)
            .bindFromRequest(request, AdminProgramExportForm.FIELD_NAMES.toArray(new String[0]));

    String json = form.get().getProgramJSON();
    String filename = adminName + "-exported.json";

    return ok(json)
        .as(Http.MimeTypes.JSON)
        .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
  }
}
