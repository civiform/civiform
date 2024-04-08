package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.inject.Inject;
import controllers.CiviFormController;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
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
  private final ObjectMapper objectMapper;

  private final SettingsManifest settingsManifest;

  @Inject
  public AdminImportController(
      AdminImportView adminImportView,
      AdminImportViewPartial adminImportViewPartial,
      FormFactory formFactory,
      ObjectMapper objectMapper,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminImportView = checkNotNull(adminImportView);
    this.adminImportViewPartial = checkNotNull(adminImportViewPartial);
    this.formFactory = checkNotNull(formFactory);
    // These extra modules let ObjectMapper serialize Guava types like ImmutableList.
    this.objectMapper =
        checkNotNull(objectMapper)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());
    this.settingsManifest = checkNotNull(settingsManifest);
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

    ProgramMigration programMigration;
    try {
      programMigration = objectMapper.readValue(jsonString, ProgramMigration.class);
    } catch (RuntimeException | JsonProcessingException e) {
      return ok(
          adminImportViewPartial
              .renderError("JSON is incorrectly formatted: " + e.getMessage())
              .render());
    }

    if (programMigration.getProgram() == null) {
      return ok(
          adminImportViewPartial
              .renderError("JSON file did not have a top-level \"program\" field")
              .render());
    }
    return ok(adminImportViewPartial.renderProgramData(programMigration).render());
  }
}
