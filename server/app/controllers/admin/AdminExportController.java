package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.util.concurrent.CompletionStage;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ProgramService;
import views.admin.migration.AdminExportView;
import views.admin.migration.AdminProgramExportForm;

/**
 * A controller that allows admins to export programs into a JSON format. This JSON format will be
 * consumed by {@link AdminImportController} to re-create the programs. Typically, admins will
 * export from one environment (e.g. staging) and import to another environment (e.g. production).
 */
public class AdminExportController extends CiviFormController {
  private final AdminExportView adminExportView;
  private final FormFactory formFactory;
  private final ProgramService programService;

  @Inject
  public AdminExportController(
      AdminExportView adminExportView,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ProgramService programService,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminExportView = checkNotNull(adminExportView);
    this.formFactory = checkNotNull(formFactory);
    this.programService = checkNotNull(programService);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> index(Http.Request request) {
    return supplyAsync(
        () ->
            ok(
                adminExportView.render(
                    request,
                    // TODO(#7087): Should we allow admins to export only active programs, only
                    // draft programs, or both?
                    programService.getActiveAndDraftPrograms().getActivePrograms())));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result exportProgram(Http.Request request) {
    Form<AdminProgramExportForm> form =
        formFactory
            .form(AdminProgramExportForm.class)
            .bindFromRequest(request, AdminProgramExportForm.FIELD_NAMES.toArray(new String[0]));
    // TODO(#7087): Return JSON representing the exported program.
    System.out.println("selected program: " + form.get().getProgramId());
    return badRequest();
  }
}
