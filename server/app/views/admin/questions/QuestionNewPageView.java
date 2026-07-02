package views.admin.questions;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import play.i18n.Messages;
import views.LayoutType;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.shared.LayoutDeps;
import views.shared.ScriptElementSettings;

/** Thymeleaf view for the new question creation page. */
public final class QuestionNewPageView extends AdminLayoutBaseView<QuestionNewPageViewModel> {

  @Inject
  public QuestionNewPageView(LayoutDeps adminLayoutDeps) {
    super(adminLayoutDeps);
  }

  @Override
  protected String pageTitle(QuestionNewPageViewModel model, Messages messages) {
    return "New " + model.getQuestionTypeLabel() + " question";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.QUESTIONS;
  }

  @Override
  protected LayoutType layoutType() {
    return LayoutType.CONTENT_WITH_RIGHT_SIDEBAR;
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/QuestionNewPage.html";
  }

  @Override
  protected ImmutableList<ScriptElementSettings> getPageBodyScripts() {
    return ImmutableList.of(
        ScriptElementSettings.builder()
            .src(bundledAssetsFinder.getPageBundle("admin/question_edit_page"))
            .type("module")
            .build());
  }
}
