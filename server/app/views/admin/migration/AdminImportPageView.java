package views.admin.migration;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the program import page, rendering AdminImportPage.html. */
public final class AdminImportPageView
    extends LegacyTailwindLayoutBaseView<AdminImportPageViewModel> {

  @Inject
  public AdminImportPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(AdminImportPageViewModel model, Messages messages) {
    return "Import an existing program";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/migration/AdminImportPage.html";
  }
}
