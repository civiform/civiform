package views.admin.programs;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the new/edit program details page, rendering ProgramFormPage.html. */
public final class ProgramFormPageView
    extends LegacyTailwindLayoutBaseView<ProgramFormPageViewModel> {

  @Inject
  public ProgramFormPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ProgramFormPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ProgramFormPage.html";
  }
}
