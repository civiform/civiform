package controllers.admin;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.CiviFormController;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import views.admin.importexport.AdminImportExportView;

/** TODO */
public final class AdminImportExportController extends CiviFormController {
  private final AdminImportExportView adminImportExportView;

  @Inject
  public AdminImportExportController(
      AdminImportExportView adminImportExportView,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminImportExportView = adminImportExportView;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    return ok(adminImportExportView.render(request));
  }
}
