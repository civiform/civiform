package views.admin.questions;

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
    List<String> possibleKeys)
    implements BaseViewModel {

  /** Creates an empty MapQuestionSettingsPartialViewModel with default placeholders. */
  public static MapQuestionSettingsPartialViewModel withEmptyDefaults(List<String> possibleKeys) {
    return new MapQuestionSettingsPartialViewModel(
        OptionalInt.empty(),
        MapQuestionForm.Setting.emptySetting(),
        MapQuestionForm.Setting.emptySetting(),
        MapQuestionForm.Setting.emptySetting(),
        MapQuestionForm.Setting.emptyFilters(),
        possibleKeys);
  }
}
