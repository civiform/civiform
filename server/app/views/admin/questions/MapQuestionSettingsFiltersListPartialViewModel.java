package views.admin.questions;

import controllers.admin.routes;
import forms.MapQuestionForm;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import views.admin.BaseViewModel;

@Getter
@AllArgsConstructor
public final class MapQuestionSettingsFiltersListPartialViewModel implements BaseViewModel {
  private List<String> possibleKeys;
  private List<MapQuestionForm.Setting> filters;

  public String hxDeleteMapQuestionFilterEndpoint() {
    return routes.AdminQuestionController.deleteMapQuestionFilter().url();
  }
}
