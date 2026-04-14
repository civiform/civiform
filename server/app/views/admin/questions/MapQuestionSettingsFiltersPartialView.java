package views.admin.questions;

import javax.inject.Inject;
import views.BaseView;
import views.shared.BaseViewDeps;

public final class MapQuestionSettingsFiltersPartialView
    extends BaseView<MapQuestionSettingsFiltersPartialViewModel> {
  @Inject
  public MapQuestionSettingsFiltersPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/MapQuestionSettingsFiltersPartial.html";
  }
}
