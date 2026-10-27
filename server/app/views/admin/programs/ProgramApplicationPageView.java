package views.admin.programs;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for a program admin viewing a single submitted application, rendering
 * ProgramApplicationPage.html.
 *
 * <p>1:1 port of {@link ProgramApplicationView}; slated for replacement by an {@link
 * views.admin.AdminLayoutBaseView} page in the admin redesign.
 */
public final class ProgramApplicationPageView
    extends LegacyTailwindLayoutBaseView<ProgramApplicationPageViewModel> {

  @Inject
  public ProgramApplicationPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ProgramApplicationPageViewModel model, Messages messages) {
    return model.getProgramName() + " - " + model.getApplicantNameWithApplicationId();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ProgramApplicationPage.html";
  }
}
