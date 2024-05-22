package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
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
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminExportController(
      AdminExportView adminExportView,
      AdminExportViewPartial adminExportViewPartial,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ProgramMigrationService programMigrationService,
      ProgramService programService,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminExportView = checkNotNull(adminExportView);
    this.adminExportViewPartial = checkNotNull(adminExportViewPartial);
    this.formFactory = checkNotNull(formFactory);
    this.programMigrationService = checkNotNull(programMigrationService);
    this.programService = checkNotNull(programService);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }

    // Show the most recent version of the program (eg. draft version if there is one)
    return ok(
        adminExportView.render(
            request, programService.getActiveAndDraftPrograms().getMostRecentProgramDefinitions()));
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
    if (programId == null) {
      // If they didn't select anything, just re-render the main export page.
      return redirect(routes.AdminExportController.index().url());
    }

    ProgramDefinition program;
    try {
      program = programService.getFullProgramDefinition(programId);
    } catch (ProgramNotFoundException e) {
      return badRequest(String.format("Program with ID %s could not be found", programId));
    }

    ErrorAnd<String, String> serializeResult = programMigrationService.serialize(program);
    if (serializeResult.isError()) {
      return badRequest(serializeResult.getErrors().stream().findFirst().orElseThrow());
    }

    return ok(
        adminExportViewPartial
            .renderProgramData(request, serializeResult.getResult(), program.adminName())
            .render());
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
