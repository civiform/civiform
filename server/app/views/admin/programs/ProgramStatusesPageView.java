package views.admin.programs;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the admin "Manage application statuses" page, rendering
 * ProgramStatusesPage.html. A 1:1 port of {@link ProgramStatusesView}.
 *
 * <p>Uses the transitional {@link LegacyTailwindLayoutBaseView} and is slated for replacement by an
 * {@code AdminLayoutBaseView} page in the admin redesign.
 */
public final class ProgramStatusesPageView
    extends LegacyTailwindLayoutBaseView<ProgramStatusesPageViewModel> {

  @Inject
  public ProgramStatusesPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ProgramStatusesPageViewModel model, Messages messages) {
    return "Manage program statuses";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ProgramStatusesPage.html";
  }
}
