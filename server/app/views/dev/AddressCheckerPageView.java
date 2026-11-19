package views.dev;

import javax.inject.Inject;
import play.i18n.Messages;
import views.shared.LayoutDeps;
import views.shared.UnauthenticatedLayoutBaseView;

public final class AddressCheckerPageView
    extends UnauthenticatedLayoutBaseView<AddressCheckerPageViewModel> {
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
}
