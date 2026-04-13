package views.admin.questions;

import javax.inject.Inject;
import views.BaseView;
import views.shared.BaseViewDeps;

public final class MapQuestionSettingsFiltersListPartialView
    extends BaseView<MapQuestionSettingsFiltersListPartialViewModel> {
  @Inject
  public MapQuestionSettingsFiltersListPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/MapQuestionSettingsFiltersListPartial.html";
  }
}
