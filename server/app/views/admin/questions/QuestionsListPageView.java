package views.admin.questions;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the questions list page, rendering QuestionsListPage.html. */
public final class QuestionsListPageView
    extends LegacyTailwindLayoutBaseView<QuestionsListPageViewModel> {

  @Inject
  public QuestionsListPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(QuestionsListPageViewModel model, Messages messages) {
    return "All questions";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.QUESTIONS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/QuestionsListPage.html";
  }
}
