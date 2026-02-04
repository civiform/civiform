package views.admin.questions;

import controllers.admin.routes;
import java.util.List;
import lombok.Builder;
import views.admin.BaseViewModel;

@Builder
public record MapQuestionSettingsFiltersPartialViewModel(
    List<String> possibleKeys, long currentIndex) implements BaseViewModel {

  public String hxDeleteMapQuestionFilterEndpoint() {
    return routes.AdminQuestionController.deleteMapQuestionFilter().url();
  }
}
