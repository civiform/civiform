package views.admin.programs;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the program translations page, rendering ProgramTranslationPage.html.
 *
 * <p>A 1:1 port of {@link ProgramTranslationView}; slated for replacement by an {@code
 * AdminLayoutBaseView} page during the admin redesign.
 */
public final class ProgramTranslationPageView
    extends LegacyTailwindLayoutBaseView<ProgramTranslationPageViewModel> {

  @Inject
  public ProgramTranslationPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ProgramTranslationPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ProgramTranslationPage.html";
  }
}
