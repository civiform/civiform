package views.admin.questions;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the new question page, rendering QuestionNewPage.html. */
public final class QuestionNewPageView
    extends LegacyTailwindLayoutBaseView<QuestionNewPageViewModel> {

  @Inject
  public QuestionNewPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(QuestionNewPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.QUESTIONS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/QuestionNewPage.html";
  }

  @Override
  @SuppressWarnings("deprecation")
  protected boolean isWidescreen() {
    return true;
  }
}
