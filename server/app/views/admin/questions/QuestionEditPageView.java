package views.admin.questions;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the edit question page, rendering QuestionEditPage.html. */
public final class QuestionEditPageView
    extends LegacyTailwindLayoutBaseView<QuestionEditPageViewModel> {

  @Inject
  public QuestionEditPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(QuestionEditPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.QUESTIONS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/QuestionEditPage.html";
  }

  @Override
  @SuppressWarnings("deprecation")
  protected boolean isWidescreen() {
    return true;
  }
}
