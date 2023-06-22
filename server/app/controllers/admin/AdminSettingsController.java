package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Result;
import repository.VersionRepository;
import views.admin.settings.AdminSettingsIndexView;

/** Provides access to application settings to the CiviForm Admin role. */
public class AdminSettingsController extends CiviFormController {

  private final AdminSettingsIndexView indexView;

  @Inject
  public AdminSettingsController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      AdminSettingsIndexView indexView) {
    super(profileUtils, versionRepository);
    this.indexView = checkNotNull(indexView);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index() {
    return ok(indexView.render());
  }
}
