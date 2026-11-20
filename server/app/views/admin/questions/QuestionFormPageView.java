package views.admin.questions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import modules.ThymeleafModule;
import play.i18n.Messages;
import play.mvc.Http;
import services.cloud.PublicStorageClient;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the new/edit question page, rendering QuestionFormPage.html. */
public final class QuestionFormPageView
    extends LegacyTailwindLayoutBaseView<QuestionFormPageViewModel> {

  private final PublicStorageClient publicStorageClient;

  @Inject
  public QuestionFormPageView(LayoutDeps layoutDeps, PublicStorageClient publicStorageClient) {
    super(layoutDeps);
    this.publicStorageClient = checkNotNull(publicStorageClient);
  }

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    super.customizeContext(request, context);
    context.setVariable("questionImageMaxFileSizeMb", publicStorageClient.getFileLimitMb());
  }

  @Override
  protected String pageTitle(QuestionFormPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.QUESTIONS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/QuestionFormPage.html";
  }

  @Override
  @SuppressWarnings("deprecation")
  protected boolean isWidescreen() {
    return true;
  }
}
