package views.admin.questions;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the new/edit question page, rendering QuestionFormPage.html. */
public final class QuestionFormPageView
    extends LegacyTailwindLayoutBaseView<QuestionFormPageViewModel> {

  @Inject
  public QuestionFormPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
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
