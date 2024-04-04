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
import services.program.ProgramService;
import services.settings.SettingsManifest;
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
  private final ProgramService programService;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminExportController(
      AdminExportView adminExportView,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ProgramService programService,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminExportView = checkNotNull(adminExportView);
    this.formFactory = checkNotNull(formFactory);
    this.programService = checkNotNull(programService);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }
    return ok(
        adminExportView.render(
            request,
            // TODO(#7087): Should we allow admins to export only active programs, only
            // draft programs, or both?
            programService.getActiveAndDraftPrograms().getActivePrograms()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result exportProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }
    Form<AdminProgramExportForm> form =
        formFactory
            .form(AdminProgramExportForm.class)
            .bindFromRequest(request, AdminProgramExportForm.FIELD_NAMES.toArray(new String[0]));
    // TODO(#7087): Show an error if no program was selected.
    // TODO(#7087): Return JSON representing the exported program.
    return notFound(
        String.format(
            "Received ID: %s. Program export is not yet implemented", form.get().getProgramId()));
  }
}
