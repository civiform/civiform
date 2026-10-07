package views.admin.programs;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the program admin's program list page, rendering ProgramAdminIndexPage.html.
 *
 * <p>Uses the transitional {@link LegacyTailwindLayoutBaseView} and is slated for replacement by an
 * {@code AdminLayoutBaseView} page in the admin redesign.
 */
public final class ProgramAdminIndexPageView
    extends LegacyTailwindLayoutBaseView<ProgramAdminIndexPageViewModel> {

  @Inject
  public ProgramAdminIndexPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ProgramAdminIndexPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ProgramAdminIndexPage.html";
  }
}
