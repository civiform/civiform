package views.admin.apibridge.discovery;

import com.google.inject.Inject;
import views.BaseView;
import views.shared.BaseViewDeps;

/** View setup for rendering the DiscoveryDetailsPartial.html */
public class DiscoveryDetailsPartialView extends BaseView<DiscoveryDetailsPartialViewModel> {

  @Inject
  public DiscoveryDetailsPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/discovery/DiscoveryDetailsPartial";
  }
}
