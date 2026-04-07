package views.admin.ti;

import javax.inject.Inject;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.shared.LayoutDeps;

public final class TrustedIntermediaryGroupListPageView
    extends AdminLayoutBaseView<TrustedIntermediaryGroupListPageViewModel> {
  @Inject
  public TrustedIntermediaryGroupListPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(TrustedIntermediaryGroupListPageViewModel model) {
    return "Manage Trusted Intermediaries";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.INTERMEDIARIES;
  }

  @Override
  protected String pageTemplate() {
    return "admin/ti/TrustedIntermediaryGroupListPage.html";
  }
}
