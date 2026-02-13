package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import controllers.CiviFormController;
import controllers.FlashKey;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import mapping.admin.settings.AdminSettingsPageMapper;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.settings.SettingsManifest;
import services.settings.SettingsService;
import services.settings.SettingsService.SettingsGroupUpdateResult.UpdateError;
import views.CiviFormMarkdown;
import views.admin.settings.AdminSettingsIndexView;
import views.admin.settings.AdminSettingsPageView;
import views.admin.settings.AdminSettingsPageViewModel;

/** Provides access to application settings to the CiviForm Admin role. */
public class AdminSettingsController extends CiviFormController {

  private final AdminSettingsIndexView indexView;
  private final AdminSettingsPageView adminSettingsPageView;
  private final FormFactory formFactory;
  private final SettingsService settingsService;
  private final SettingsManifest settingsManifest;
  private final CiviFormMarkdown civiFormMarkdown;

  @Inject
  public AdminSettingsController(
      AdminSettingsIndexView indexView,
      AdminSettingsPageView adminSettingsPageView,
      FormFactory formFactory,
      SettingsService settingsService,
      SettingsManifest settingsManifest,
      CiviFormMarkdown civiFormMarkdown,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.indexView = checkNotNull(indexView);
    this.adminSettingsPageView = checkNotNull(adminSettingsPageView);
    this.formFactory = checkNotNull(formFactory);
    this.settingsService = checkNotNull(settingsService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.civiFormMarkdown = checkNotNull(civiFormMarkdown);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      AdminSettingsPageViewModel model =
          buildViewModel(request, /* errorMessages= */ Optional.empty());
      return ok(adminSettingsPageView.render(request, model)).as(Http.MimeTypes.HTML);
    }
    return ok(indexView.render(request));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Http.Request request) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    ImmutableMap<String, String> settingUpdates =
        formFactory.form().bindFromRequest(request).rawData().entrySet().stream()
            .filter(entry -> !entry.getKey().equals("csrfToken"))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    SettingsService.SettingsGroupUpdateResult result =
        settingsService.updateSettings(settingUpdates, profile);

    if (result.hasErrors()) {
      if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
        AdminSettingsPageViewModel model = buildViewModel(request, result.errorMessages());
        return ok(adminSettingsPageView.render(request, model)).as(Http.MimeTypes.HTML);
      }
      return ok(indexView.render(request, result.errorMessages()));
    }

    Result destination = redirect(routes.AdminSettingsController.index());
    return result.updated()
        ? destination.flashing(FlashKey.SUCCESS, "Settings updated")
        : destination.flashing(FlashKey.WARNING, "No changes to save");
  }

  private AdminSettingsPageViewModel buildViewModel(
      Http.Request request, Optional<ImmutableMap<String, UpdateError>> errorMessages) {
    AdminSettingsPageMapper mapper = new AdminSettingsPageMapper();
    return mapper.map(request, settingsManifest, civiFormMarkdown, errorMessages);
  }
}
