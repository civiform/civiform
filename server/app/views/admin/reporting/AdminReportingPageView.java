package views.admin.reporting;

import javax.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;
import views.shared.LayoutDeps;

public final class AdminReportingPageView
    extends TransitionalLayoutBaseView<AdminReportingPageViewModel> {
  @Inject
  public AdminReportingPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(AdminReportingPageViewModel model, Messages messages) {
    return "Reporting";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.REPORTING;
  }

  @Override
  protected String pageTemplate() {
    return "admin/reporting/AdminReportingPage.html";
  }
}
