package views.admin.programs;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf admin page for program image; used when file upload improvements are on. */
public final class ProgramImagePageView
    extends TransitionalLayoutBaseView<ProgramImagePageViewModel> {

  @Inject
  public ProgramImagePageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTitle(ProgramImagePageViewModel model, Messages messages) {
    return "Image upload";
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ProgramImageFragment";
  }
}
