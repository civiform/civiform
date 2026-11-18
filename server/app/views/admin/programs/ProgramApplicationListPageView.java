package views.admin.programs;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the admin's list of applications submitted to a program, rendering
 * ProgramApplicationListPage.html.
 *
 * <p>Uses the transitional {@link LegacyTailwindLayoutBaseView} and is slated for replacement by an
 * {@code AdminLayoutBaseView} page in the admin redesign.
 */
public final class ProgramApplicationListPageView
    extends LegacyTailwindLayoutBaseView<ProgramApplicationListPageViewModel> {

  @Inject
  public ProgramApplicationListPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ProgramApplicationListPageViewModel model, Messages messages) {
    return model.getProgramName() + " - Applications";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ProgramApplicationListPage.html";
  }
}
