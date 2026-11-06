package views.admin.programs;

import com.google.inject.Inject;
import modules.ThymeleafModule;
import play.i18n.Messages;
import play.mvc.Http;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;
import views.components.Icons;
import views.shared.LayoutDeps;

/** Thymeleaf admin page for program image; used when file upload improvements are on. */
public final class ProgramImagePageView
    extends TransitionalLayoutBaseView<ProgramImagePageViewModel> {

  @Inject
  public ProgramImagePageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    super.customizeContext(request, context);
    // The program card preview reuses applicant fragments that expect these icons, which are
    // otherwise only set by ApplicantBaseView. External program cards need both.
    context.setVariable("launchIcon", Icons.LAUNCH);
    context.setVariable("closeIcon", Icons.CLOSE);
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
