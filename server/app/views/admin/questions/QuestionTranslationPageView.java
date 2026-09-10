package views.admin.questions;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the question translations page, rendering QuestionTranslationPage.html. */
public final class QuestionTranslationPageView
    extends LegacyTailwindLayoutBaseView<QuestionTranslationPageViewModel> {

  @Inject
  public QuestionTranslationPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(QuestionTranslationPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.QUESTIONS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/QuestionTranslationPage.html";
  }
}
