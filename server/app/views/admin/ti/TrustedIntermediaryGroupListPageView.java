package views.admin.ti;

import javax.inject.Inject;
import play.i18n.Messages;
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
  protected String pageTitle(TrustedIntermediaryGroupListPageViewModel model, Messages messages) {
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
