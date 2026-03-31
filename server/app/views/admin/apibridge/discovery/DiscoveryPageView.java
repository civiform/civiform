package views.admin.apibridge.discovery;

import com.google.inject.Inject;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;
import views.shared.LayoutDeps;

/** View setup for rendering the DiscoveryPage.html */
public class DiscoveryPageView extends TransitionalLayoutBaseView<DiscoveryPageViewModel> {

  @Inject
  public DiscoveryPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(DiscoveryPageViewModel model) {
    return "API Bridge Discovery";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_BRIDGE_DISCOVERY;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/discovery/DiscoveryPage.html";
  }
}
