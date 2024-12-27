package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.rawHtml;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import controllers.CiviFormController;
import controllers.FlashKey;
import java.util.Map;
import javax.inject.Inject;

import j2html.tags.specialized.DivTag;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.settings.SettingsService;
import views.admin.settings.AdminSettingsIndex2View;
import views.admin.settings.AdminSettingsIndexView;

/** Provides access to application settings to the CiviForm Admin role. */
public class AdminSettingsController extends CiviFormController {

  private final AdminSettingsIndexView indexView;
  private final FormFactory formFactory;
  private final SettingsService settingsService;
  private final AdminSettingsIndex2View adminSettingsIndex2View;

  @Inject
  public AdminSettingsController(
      AdminSettingsIndexView indexView,
      FormFactory formFactory,
      SettingsService settingsService,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      AdminSettingsIndex2View adminSettingsIndex2View) {
    super(profileUtils, versionRepository);
    this.indexView = checkNotNull(indexView);
    this.formFactory = checkNotNull(formFactory);
    this.settingsService = checkNotNull(settingsService);
    this.adminSettingsIndex2View = checkNotNull(adminSettingsIndex2View);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    return ok(indexView.render(request));
  }

  // TODO Gwen
  //@Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index2(Http.Request request) {
    // Things I could do:

    // // Render my view (full page or partial page) with thymeleaf to string and return to the client
    // return ok(adminSettingsIndex2View.render(request)).as(Http.MimeTypes.HTML);

    // // Or add the thymeleaf html string to a j2html div and return the div to the client
    // String adminSettingsIndex2ViewHtml = adminSettingsIndex2View.render(request);
    // DivTag divResult = div().with(rawHtml(adminSettingsIndex2ViewHtml));
    // return ok(divResult.render());

    // // Pass the j2html into the view and have it render the raw string
    // no example yet

    return ok(adminSettingsIndex2View.render(request)).as(Http.MimeTypes.HTML);
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
      return ok(indexView.render(request, result.errorMessages()));
    }

    Result destination = redirect(routes.AdminSettingsController.index());
    return result.updated()
        ? destination.flashing(FlashKey.SUCCESS, "Settings updated")
        : destination.flashing(FlashKey.WARNING, "No changes to save");
  }
}
