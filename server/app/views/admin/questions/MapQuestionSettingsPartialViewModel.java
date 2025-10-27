package views.admin.questions;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import forms.MapQuestionForm;
import java.util.List;
import java.util.OptionalInt;
import lombok.Builder;
import views.admin.BaseViewModel;

@Builder
public record MapQuestionSettingsPartialViewModel(
    OptionalInt maxLocationSelections,
    MapQuestionForm.Setting locationName,
    MapQuestionForm.Setting locationAddress,
    MapQuestionForm.Setting locationDetailsUrl,
    List<MapQuestionForm.Setting> filters,
    MapQuestionForm.Setting locationTag,
    ImmutableList<String> possibleKeys)
    implements BaseViewModel {

  /** Creates an empty MapQuestionSettingsPartialViewModel with default placeholders. */
  public static MapQuestionSettingsPartialViewModel withEmptyDefaults(
      ImmutableList<String> possibleKeys) {
    return new MapQuestionSettingsPartialViewModel(
        OptionalInt.empty(),
        MapQuestionForm.Setting.emptySetting(),
        MapQuestionForm.Setting.emptySetting(),
        MapQuestionForm.Setting.emptySetting(),
        MapQuestionForm.Setting.emptyFilters(),
        MapQuestionForm.Setting.emptySetting(),
        possibleKeys);
  }

  public String hxAddMapQuestionFilterEndpoint() {
    return routes.AdminQuestionController.addMapQuestionFilter().url();
  }

  public String hxDeleteMapQuestionFilterEndpoint() {
    return routes.AdminQuestionController.deleteMapQuestionFilter().url();
  }
}
