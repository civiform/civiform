package views.dev;

import javax.inject.Inject;
import play.i18n.Messages;
import views.shared.LayoutDeps;
import views.shared.UnauthenticatedLayoutBaseView;

public final class DevToolsPageView extends UnauthenticatedLayoutBaseView<DevToolsPageViewModel> {
  @Inject
  public DevToolsPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
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
