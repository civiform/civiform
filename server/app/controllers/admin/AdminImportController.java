package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.CiviFormController;
import models.ProgramModel;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.ErrorAnd;
import services.migration.ProgramMigrationService;
import services.settings.SettingsManifest;
import views.admin.migration.AdminImportView;
import views.admin.migration.AdminImportViewPartial;
import views.admin.migration.AdminProgramImportForm;

/**
 * A controller for the import part of program migration (allowing admins to easily migrate programs
 * between different environments). This controller is responsible for reading a JSON file that
 * represents a program and turning it into a full-fledged {@link
 * services.program.ProgramDefinition}, including all blocks and questions. {@link
 * AdminExportController} is responsible for exporting an existing program into the JSON format that
 * will be read by this controller.
 *
 * <p>Typically, admins will export from one environment (e.g. staging) and import to another
 * environment (e.g. production).
 */
public class AdminImportController extends CiviFormController {

  private final AdminImportView adminImportView;
  private final AdminImportViewPartial adminImportViewPartial;
  private final FormFactory formFactory;
  private final ProgramMigrationService programMigrationService;
  private final SettingsManifest settingsManifest;
  private final ProgramRepository programRepository;

  @Inject
  public AdminImportController(
      AdminImportView adminImportView,
      AdminImportViewPartial adminImportViewPartial,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ProgramMigrationService programMigrationService,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository,
      ProgramRepository programRepository) {
    super(profileUtils, versionRepository);
    this.adminImportView = checkNotNull(adminImportView);
    this.adminImportViewPartial = checkNotNull(adminImportViewPartial);
    this.formFactory = checkNotNull(formFactory);
    this.programMigrationService = checkNotNull(programMigrationService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programRepository = checkNotNull(programRepository);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }
    return ok(adminImportView.render(request));
  }

  /** HTMX Partial that parses and renders the program data included in the request. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxImportProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }

    Form<AdminProgramImportForm> form =
        formFactory
            .form(AdminProgramImportForm.class)
            .bindFromRequest(request, AdminProgramImportForm.FIELD_NAMES.toArray(new String[0]));
    String jsonString = form.get().getProgramJson();
    if (jsonString == null) {
      // If they didn't upload anything, just re-render the main import page.
      return redirect(routes.AdminImportController.index().url());
    }

    // TODO(#7087) remove this when we add the ability to parse questions into QuestionDefinitions
    jsonString = trimQuestionsOffJson(jsonString);

    ErrorAnd<ProgramMigrationWrapper, String> deserializeResult =
        programMigrationService.deserialize(jsonString);

    if (deserializeResult.isError()) {
      return ok(
          adminImportViewPartial
              .renderError(deserializeResult.getErrors().stream().findFirst().orElseThrow())
              .render());
    }

    ProgramMigrationWrapper programMigrationWrapper = deserializeResult.getResult();
    if (programMigrationWrapper.getProgram() == null) {
      return ok(
          adminImportViewPartial
              .renderError("JSON did not have a top-level \"program\" field")
              .render());
    }
    return ok(
        adminImportViewPartial
            .renderProgramData(request, programMigrationWrapper, jsonString)
            .render());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result saveProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }

    Form<AdminProgramImportForm> form =
        formFactory
            .form(AdminProgramImportForm.class)
            .bindFromRequest(request, AdminProgramImportForm.FIELD_NAMES.toArray(new String[0]));

    String jsonString = form.get().getProgramJson();

    ErrorAnd<ProgramMigrationWrapper, String> deserializeResult =
        programMigrationService.deserialize(jsonString);

    if (deserializeResult.isError()) {
      return ok(
          adminImportViewPartial
              .renderError(deserializeResult.getErrors().stream().findFirst().orElseThrow())
              .render());
    }
    ProgramMigrationWrapper programMigrationWrapper = deserializeResult.getResult();
    ProgramModel programModel =
        new ProgramModel(
            programMigrationWrapper.getProgram(), versionRepository.getDraftVersionOrCreate());
    programRepository.insertProgramSync(programModel);

    return ok(adminImportView.render(request));
  }

  // TODO(#7087) remove this when we add the ability to parse questions into QuestionDefinitions
  private String trimQuestionsOffJson(String jsonString) {
    int indexOfQuestions = jsonString.indexOf("\"questions\"");
    // questions are not included in the json if the array of questions is empty, so we need to
    // check that this field exists before building a substring off of it
    if (indexOfQuestions != -1) {
      StringBuilder jsonStringBuilder =
          new StringBuilder(jsonString.substring(0, indexOfQuestions));
      jsonString =
          jsonStringBuilder.deleteCharAt(jsonStringBuilder.lastIndexOf(",")).append("}").toString();
    }
    return jsonString;
  }
}
