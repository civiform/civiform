package views.admin.questions;

import javax.inject.Inject;
import views.admin.BaseView;
import views.shared.BaseViewDeps;

public final class MapQuestionSettingsPartialView
    extends BaseView<MapQuestionSettingsPartialViewModel> {
  @Inject
  public MapQuestionSettingsPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/MapQuestionSettingsPartial.html";
  }
}
