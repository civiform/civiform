package views.admin.migration;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the program export page, rendering AdminExportPage.html. */
public final class AdminExportPageView
    extends LegacyTailwindLayoutBaseView<AdminExportPageViewModel> {

  @Inject
  public AdminExportPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(AdminExportPageViewModel model, Messages messages) {
    return "Export a program";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/migration/AdminExportPage.html";
  }

  @Override
  @SuppressWarnings("deprecation")
  protected boolean isWidescreen() {
    return true;
  }
}
