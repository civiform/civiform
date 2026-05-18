package views.admin.programs;

import com.google.inject.Inject;
import controllers.FlashKey;
import modules.ThymeleafModule;
import play.i18n.Messages;
import play.mvc.Http;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf admin page for program image; used when file upload improvements are on. Client
 * behavior: {@code server/app/assets/javascripts/admin_program_image.ts}.
 */
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
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    super.customizeContext(request, context);
    context.setVariable("flashSuccess", request.flash().get(FlashKey.SUCCESS));
    context.setVariable("flashError", request.flash().get(FlashKey.ERROR));
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
