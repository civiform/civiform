package views.dev;

import javax.inject.Inject;
import modules.ThymeleafModule;
import play.i18n.Messages;
import play.mvc.Http;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.admin.shared.AdminCommonHeader;
import views.shared.LayoutDeps;

public final class DevToolsPageView extends AdminLayoutBaseView<DevToolsPageViewModel> {
  @Inject
  public DevToolsPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
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

  @Override
  protected String pageTitle(DevToolsPageViewModel model, Messages messages) {
    return "Dev Tools";
  }

  @Override
  protected String pageTemplate() {
    return "dev/DevToolsPage.html";
  }
}
