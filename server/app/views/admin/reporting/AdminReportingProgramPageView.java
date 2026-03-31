package views.admin.reporting;

import javax.inject.Inject;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.shared.LayoutDeps;

public final class AdminReportingProgramPageView
    extends AdminLayoutBaseView<AdminReportingProgramPageViewModel> {
  @Inject
  public AdminReportingProgramPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(AdminReportingProgramPageViewModel model) {
    return "Reporting";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.REPORTING;
  }

  @Override
  protected String pageTemplate() {
    return "admin/reporting/AdminReportingProgramPage.html";
  }
}
