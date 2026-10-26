package views.admin.programs;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the manage program admins page, rendering ManageProgramAdminsPage.html.
 *
 * <p>This is a 1:1 port of {@link ManageProgramAdminsView} on the transitional legacy Tailwind
 * layout; it is slated for replacement by an {@link views.admin.AdminLayoutBaseView} page in the
 * admin redesign.
 */
public final class ManageProgramAdminsPageView
    extends LegacyTailwindLayoutBaseView<ManageProgramAdminsPageViewModel> {

  // Keep in sync with the heading in ManageProgramAdminsPage.html.
  private static final String PAGE_TITLE_PREFIX = "Manage admins for program: ";

  @Inject
  public ManageProgramAdminsPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ManageProgramAdminsPageViewModel model, Messages messages) {
    return PAGE_TITLE_PREFIX + model.getProgramAdminName();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ManageProgramAdminsPage.html";
  }
}
