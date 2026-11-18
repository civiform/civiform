package views.dev;

import auth.CiviFormProfile;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import views.admin.AdminLayoutBaseView;
import views.shared.LayoutDeps;

public final class DevToolsPageView extends AdminLayoutBaseView<DevToolsPageViewModel> {
  @Inject
  public DevToolsPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected Optional<CiviFormProfile> getProfile(Http.Request request) {
    // Avoid requiring a user profile for dev tools page.
    return profileUtils.optionalCurrentUserProfile(request);
  }

  @Override
  protected String pageTitle(DevToolsPageViewModel model, Messages messages) {
    return "Dev Tools";
  }

  @Override
  protected String pageTemplate() {
    return "dev/DevToolsPage.html";
  }
}
