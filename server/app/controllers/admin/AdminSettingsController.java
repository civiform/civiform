package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import controllers.CiviFormController;
import controllers.FlashKey;
import java.util.Map;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.settings.SettingsService;
import views.admin.settings.AdminSettingsIndexView;

/** Provides access to application settings to the CiviForm Admin role. */
public class AdminSettingsController extends CiviFormController {

  private final AdminSettingsIndexView indexView;
  private final FormFactory formFactory;
  private final SettingsService settingsService;

  @Inject
  public AdminSettingsController(
      AdminSettingsIndexView indexView,
      FormFactory formFactory,
      SettingsService settingsService,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.indexView = checkNotNull(indexView);
    this.formFactory = checkNotNull(formFactory);
    this.settingsService = checkNotNull(settingsService);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    return ok(indexView.render(request));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Http.Request request) {
    ImmutableMap<String, String> settingUpdates =
        formFactory.form().bindFromRequest(request).rawData().entrySet().stream()
            .filter(entry -> !entry.getKey().equals("csrfToken"))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    SettingsService.SettingsGroupUpdateResult result =
        settingsService.updateSettings(settingUpdates, profileUtils.currentUserProfile(request));

    if (result.hasErrors()) {
      return ok(indexView.render(request, result.errorMessages()));
    }

    Result destination = redirect(routes.AdminSettingsController.index());
    return result.updated()
        ? destination.flashing(FlashKey.SUCCESS, "Settings updated")
        : destination.flashing(FlashKey.WARNING, "No changes to save");
  }
}
