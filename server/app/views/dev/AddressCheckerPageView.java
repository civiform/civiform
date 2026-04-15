package views.dev;

import javax.inject.Inject;
import modules.ThymeleafModule;
import play.i18n.Messages;
import play.mvc.Http;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.admin.shared.AdminCommonHeader;
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
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    // Avoid requiring a user profile for dev tools page.
    context.setVariable(
        "adminCommonHeader",
        AdminCommonHeader.builder()
            .activeNavPage(AdminLayout.NavPage.NULL_PAGE)
            .isOnlyProgramAdmin(false)
            .isApiBridgeEnabled(settingsManifest.getApiBridgeEnabled(request))
            .build());
  }
}
