package views.dev;

import auth.CiviFormProfile;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import views.admin.AdminLayoutBaseView;
import views.shared.LayoutDeps;

public final class AddressCheckerPageView extends AdminLayoutBaseView<AddressCheckerPageViewModel> {
  @Inject
  public AddressCheckerPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(AddressCheckerPageViewModel model, Messages messages) {
    return "Address Checker";
  }

  @Override
  protected String pageTemplate() {
    return "dev/AddressCheckerPage.html";
  }

  @Override
  protected Optional<CiviFormProfile> getProfile(Http.Request request) {
    // Avoid requiring a user profile for dev tools page.
    return profileUtils.optionalCurrentUserProfile(request);
  }
}
