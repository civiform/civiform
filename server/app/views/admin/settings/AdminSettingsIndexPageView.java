package views.admin.settings;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the admin server settings page, rendering AdminSettingsIndexPage.html. */
public final class AdminSettingsIndexPageView
    extends LegacyTailwindLayoutBaseView<AdminSettingsIndexPageViewModel> {

  @Inject
  public AdminSettingsIndexPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(AdminSettingsIndexPageViewModel model, Messages messages) {
    return "Settings";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.SETTINGS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/settings/AdminSettingsIndexPage.html";
  }

  /** The legacy page rendered full width (AdminStyles.MAIN_FULL). */
  @Override
  @SuppressWarnings("deprecation")
  protected boolean isWidescreen() {
    return true;
  }
}
